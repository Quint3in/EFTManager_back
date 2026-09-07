package cat.itacademy.s05.t02.eftmanager.task;

import cat.itacademy.s05.t02.eftmanager.common.GameMode;
import cat.itacademy.s05.t02.eftmanager.common.exception.ExternalApiException;
import cat.itacademy.s05.t02.eftmanager.user.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class TaskService {

    private final RestClient restClient;
    private final TaskProgressRepository taskProgressRepository;
    private final UserRepository userRepository;
    private final TaskService self;

    public TaskService(RestClient.Builder restClientBuilder,
                       @Value("${tarkov.api.base-url}") String tarkovApiBaseUrl,
                       TaskProgressRepository taskProgressRepository,
                       UserRepository userRepository,
                       @Lazy TaskService self) {
        this.restClient = restClientBuilder.baseUrl(tarkovApiBaseUrl).build();
        this.taskProgressRepository = taskProgressRepository;
        this.userRepository = userRepository;
        this.self = self;
    }

    @Cacheable(value = "tasksData", key = "#mode")
    public JsonNode getTasksCatalog(GameMode mode) {
        try {
            return restClient.get()
                    .uri("/{externalMode}/tasks", mode.getExternalPath())
                    .retrieve()
                    .body(JsonNode.class);
        } catch (RestClientException ex) {
            throw new ExternalApiException("No se pudo obtener el catálogo de misiones desde Tarkov.dev", ex);
        }
    }

    @Cacheable(value = "tasksLocale", key = "#mode + '-' + #lang")
    public JsonNode getTasksLocale(GameMode mode, String lang) {
        try {
            return restClient.get()
                    .uri("/{externalMode}/tasks_{lang}", mode.getExternalPath(), lang)
                    .retrieve()
                    .body(JsonNode.class);
        } catch (RestClientException ex) {
            throw new ExternalApiException("No se pudo obtener las traducciones de misiones", ex);
        }
    }

    @CacheEvict(value = {"tasksData", "tasksLocale"}, allEntries = true)
    @Scheduled(fixedRate = 24, timeUnit = TimeUnit.HOURS)
    public void evictTasksCache() {
    }

    public List<TaskSummaryResponse> getTasks(GameMode mode, Long userId, String query, String traderId,
                                              String mapId, String status, boolean kappaOnly, boolean lightkeeperOnly,
                                              String sort, String lang) {
        JsonNode catalog = self.getTasksCatalog(mode);
        JsonNode locale = self.getTasksLocale(mode, lang).path("data");
        JsonNode tasksNode = catalog.path("data").path("tasks");

        Set<String> completedIds = taskProgressRepository.findByUserIdAndMode(userId, mode).stream()
                .filter(TaskProgress::isCompleted)
                .map(TaskProgress::getTaskId)
                .collect(Collectors.toSet());

        String normalizedQuery = query == null ? "" : query.toLowerCase();

        List<TaskSummaryResponse> result = new ArrayList<>();

        for (Map.Entry<String, JsonNode> entry : tasksNode.properties()) {
            String taskId = entry.getKey();
            JsonNode task = entry.getValue();

            String nameKey = task.path("name").asString(taskId);
            String resolvedName = locale.path(nameKey).asString(nameKey);
            String taskTraderId = task.path("trader").asString("");

            JsonNode mapNode = task.path("map");
            String taskMapId = (mapNode.isMissingNode() || mapNode.isNull()) ? null : mapNode.asString(null);

            boolean isCompleted = completedIds.contains(taskId);
            boolean isKappaRequired = task.path("kappaRequired").asBoolean(false);
            boolean isLightkeeperRequired = task.path("lightkeeperRequired").asBoolean(false);

            boolean matchesQuery = normalizedQuery.isBlank() || resolvedName.toLowerCase().contains(normalizedQuery);
            boolean matchesTrader = traderId == null || traderId.isBlank() || traderId.equals(taskTraderId);
            boolean matchesMap = mapId == null || mapId.isBlank() || mapId.equals(taskMapId);
            boolean matchesStatus = status == null || status.isBlank() || "all".equals(status)
                    || ("completed".equals(status) && isCompleted)
                    || ("incomplete".equals(status) && !isCompleted);
            boolean matchesKappa = !kappaOnly || isKappaRequired;
            boolean matchesLightkeeper = !lightkeeperOnly || isLightkeeperRequired;

            List<TaskUnmetRequirement> unmetRequirements = new ArrayList<>();
            for (JsonNode req : task.path("taskRequirements")) {
                List<String> statuses = new ArrayList<>();
                for (JsonNode s : req.path("status")) statuses.add(s.asString());
                if (!statuses.contains("complete")) continue;

                String reqTaskId = req.path("task").asString("");
                if (!completedIds.contains(reqTaskId)) {
                    JsonNode otherTask = tasksNode.path(reqTaskId);
                    String reqNameKey = otherTask.path("name").asString(reqTaskId);
                    String reqName = locale.path(reqNameKey).asString(reqTaskId);
                    unmetRequirements.add(new TaskUnmetRequirement(reqTaskId, reqName));
                }
            }

            if (matchesQuery && matchesTrader && matchesMap && matchesStatus && matchesKappa && matchesLightkeeper) {
                result.add(new TaskSummaryResponse(
                        taskId, resolvedName, taskTraderId, taskMapId,
                        task.path("minPlayerLevel").asInt(0), task.path("taskImageLink").asString(null),
                        task.path("experience").asInt(0), isKappaRequired, isLightkeeperRequired,
                        isCompleted, unmetRequirements
                ));
            }
        }

        applySort(result, sort);
        return result;
    }

    private void applySort(List<TaskSummaryResponse> tasks, String sort) {
        if (sort == null) return;

        Comparator<TaskSummaryResponse> comparator = switch (sort) {
            case "name_asc" -> Comparator.comparing(TaskSummaryResponse::name, String.CASE_INSENSITIVE_ORDER);
            case "name_desc" -> Comparator.comparing(TaskSummaryResponse::name, String.CASE_INSENSITIVE_ORDER).reversed();
            case "level_asc" -> Comparator.comparingInt(TaskSummaryResponse::minPlayerLevel);
            case "level_desc" -> Comparator.comparingInt(TaskSummaryResponse::minPlayerLevel).reversed();
            case "exp_asc" -> Comparator.comparingInt(TaskSummaryResponse::experience);
            case "exp_desc" -> Comparator.comparingInt(TaskSummaryResponse::experience).reversed();
            default -> null;
        };

        if (comparator != null) tasks.sort(comparator);
    }

    public TaskResponse getTaskDetail(GameMode mode, String taskId, Long userId, String lang) {
        JsonNode catalog = self.getTasksCatalog(mode);
        JsonNode locale = self.getTasksLocale(mode, lang).path("data");
        JsonNode tasksNode = catalog.path("data").path("tasks");
        JsonNode task = tasksNode.path(taskId);

        if (task.isMissingNode()) {
            throw new TaskNotFoundException("Misión no encontrada: " + taskId);
        }

        String nameKey = task.path("name").asString(taskId);
        String resolvedName = locale.path(nameKey).asString(nameKey);
        String normalizedName = task.path("normalizedName").asString("");

        List<TaskObjectiveResponse> objectives = new ArrayList<>();
        for (JsonNode obj : task.path("objectives")) {
            String descKey = obj.path("description").asString("");
            String resolvedDesc = locale.path(descKey).asString(descKey);

            Integer count = nullableInt(obj.path("count"));
            Boolean foundInRaid = nullableBool(obj.path("foundInRaid"));
            Integer minDurability = nullableInt(obj.path("minDurability"));
            Integer maxDurability = nullableInt(obj.path("maxDurability"));
            Integer dogTagLevel = nullableInt(obj.path("dogTagLevel"));

            List<String> mapIds = new ArrayList<>();
            for (JsonNode m : obj.path("maps")) mapIds.add(m.asString());

            List<String> itemIds = new ArrayList<>();
            for (JsonNode itemId : obj.path("items")) {
                itemIds.add(itemId.asString());
            }
            JsonNode questItemNode = obj.path("questItem");
            if (!questItemNode.isMissingNode() && !questItemNode.isNull()) {
                itemIds.add(questItemNode.asString());
            }

            objectives.add(new TaskObjectiveResponse(
                    obj.path("id").asString(""), resolvedDesc, obj.path("type").asString(""),
                    obj.path("optional").asBoolean(false),
                    count, foundInRaid, minDurability, maxDurability, dogTagLevel, mapIds, itemIds
            ));
        }

        List<TaskRewardItem> rewardItems = extractRewardItems(task.path("finishRewards"));
        List<TaskRewardItem> startRewardItems = extractRewardItems(task.path("startRewards"));
        List<TaskRewardItem> failureRewardItems = extractRewardItems(task.path("failureOutcome"));

        List<TaskRewardStanding> traderStandingRewards = new ArrayList<>();
        for (JsonNode standing : task.path("finishRewards").path("traderStanding")) {
            traderStandingRewards.add(new TaskRewardStanding(
                    standing.path("trader").asString(""), standing.path("standing").asDouble(0)));
        }

        List<String> achievementRewardIds = new ArrayList<>();
        for (JsonNode a : task.path("finishRewards").path("achievement")) {
            achievementRewardIds.add(a.asString());
        }

        Set<String> completedTaskIds = taskProgressRepository.findByUserIdAndMode(userId, mode).stream()
                .filter(TaskProgress::isCompleted)
                .map(TaskProgress::getTaskId)
                .collect(Collectors.toSet());

        List<TaskRequirementInfo> taskRequirements = new ArrayList<>();
        for (JsonNode req : task.path("taskRequirements")) {
            String reqTaskId = req.path("task").asString("");
            JsonNode otherTask = tasksNode.path(reqTaskId);
            String reqNameKey = otherTask.path("name").asString(reqTaskId);
            String reqName = locale.path(reqNameKey).asString(reqTaskId);

            List<String> statuses = new ArrayList<>();
            for (JsonNode s : req.path("status")) statuses.add(s.asString());

            Boolean satisfied = statuses.contains("complete") ? completedTaskIds.contains(reqTaskId) : null;
            taskRequirements.add(new TaskRequirementInfo(reqTaskId, reqName, statuses, satisfied));
        }

        List<TaskTraderRequirement> traderRequirements = new ArrayList<>();
        for (JsonNode req : task.path("traderRequirements")) {
            traderRequirements.add(new TaskTraderRequirement(
                    req.path("trader").asString(""), req.path("value").asInt(0)));
        }

        List<TaskNeededKeys> neededKeys = new ArrayList<>();
        for (JsonNode nk : task.path("neededKeys")) {
            List<String> keyIds = new ArrayList<>();
            for (JsonNode k : nk.path("keys")) keyIds.add(k.asString());
            neededKeys.add(new TaskNeededKeys(nk.path("map").asString(""), keyIds));
        }

        List<TaskOtherRequirement> otherRequirements = new ArrayList<>();
        for (JsonNode req : task.path("otherRequirements")) {
            List<String> traderIds = new ArrayList<>();
            for (JsonNode t : req.path("traders")) traderIds.add(t.asString());
            otherRequirements.add(new TaskOtherRequirement(
                    req.path("id").asString(""), req.path("type").asString(""), traderIds));
        }

        boolean isCompleted = completedTaskIds.contains(taskId);

        JsonNode mapNode = task.path("map");
        String mapId = (mapNode.isMissingNode() || mapNode.isNull()) ? null : mapNode.asString(null);

        return new TaskResponse(
                taskId, resolvedName, normalizedName, task.path("trader").asString(""),
                task.path("minPlayerLevel").asInt(0), task.path("wikiLink").asString(null),
                task.path("taskImageLink").asString(null), task.path("kappaRequired").asBoolean(false),
                mapId, task.path("restartable").asBoolean(false), task.path("factionName").asString("Any"),
                task.path("availableDelaySecondsMin").asInt(0), task.path("availableDelaySecondsMax").asInt(0),
                task.path("lightkeeperRequired").asBoolean(false),
                objectives, rewardItems, traderStandingRewards, startRewardItems, failureRewardItems,
                achievementRewardIds, taskRequirements, traderRequirements, neededKeys,
                task.path("finishRewards").path("experience").asInt(task.path("experience").asInt(0)),
                isCompleted
        );
    }

    public List<QuestItemResponse> getQuestItems(GameMode mode, List<String> ids, String lang) {
        JsonNode catalog = self.getTasksCatalog(mode);
        JsonNode locale = self.getTasksLocale(mode, lang).path("data");
        JsonNode questItemsNode = catalog.path("data").path("questItems");

        List<QuestItemResponse> result = new ArrayList<>();
        for (String id : ids) {
            JsonNode item = questItemsNode.path(id);
            if (item.isMissingNode()) continue;

            String normalizedName = item.path("normalizedName").asString("");
            String nameKey = item.path("name").asString("");
            String resolvedName = locale.path(nameKey).asString(normalizedName);
            String shortNameKey = item.path("shortName").asString("");
            String resolvedShortName = locale.path(shortNameKey).asString(normalizedName);

            result.add(new QuestItemResponse(id, resolvedName, resolvedShortName, normalizedName,
                    item.path("iconLink").asString(null)));
        }
        return result;
    }

    public List<AchievementResponse> getAchievements(GameMode mode, List<String> ids, String lang) {
        JsonNode catalog = self.getTasksCatalog(mode);
        JsonNode locale = self.getTasksLocale(mode, lang).path("data");
        JsonNode achievementsNode = catalog.path("data").path("achievements");

        List<AchievementResponse> result = new ArrayList<>();
        for (String id : ids) {
            JsonNode ach = achievementsNode.path(id);
            if (ach.isMissingNode()) continue;

            String normalizedName = ach.path("normalizedName").asString("");
            String nameKey = ach.path("name").asString(id);
            String resolvedName = locale.path(nameKey).asString(normalizedName);

            result.add(new AchievementResponse(
                    id, resolvedName, normalizedName,
                    ach.path("normalizedRarity").asString(""),
                    ach.path("imageLink").asString(null)
            ));
        }
        return result;
    }

    public List<TaskModeSummary> getProgressSummary(Long userId) {
        List<TaskModeSummary> result = new ArrayList<>();

        for (GameMode mode : GameMode.values()) {
            JsonNode catalog = self.getTasksCatalog(mode);
            JsonNode tasksNode = catalog.path("data").path("tasks");

            int total = 0;
            for (JsonNode ignored : tasksNode) total++;

            long completed = taskProgressRepository.findByUserIdAndMode(userId, mode).stream()
                    .filter(TaskProgress::isCompleted)
                    .count();

            result.add(new TaskModeSummary(mode.name().toLowerCase(), total, (int) completed));
        }

        return result;
    }

    public TaskComparisonResponse compareProgress(GameMode mode, Long selfUserId, String otherUsername, String lang) {
        var otherUser = userRepository.findByUsername(otherUsername)
                .orElseThrow(() -> new UserNotFoundForComparisonException("Usuario no encontrado: " + otherUsername));
        Long otherUserId = otherUser.getId();

        JsonNode catalog = self.getTasksCatalog(mode);
        JsonNode locale = self.getTasksLocale(mode, lang).path("data");
        JsonNode tasksNode = catalog.path("data").path("tasks");

        Set<String> selfCompleted = taskProgressRepository.findByUserIdAndMode(selfUserId, mode).stream()
                .filter(TaskProgress::isCompleted).map(TaskProgress::getTaskId).collect(Collectors.toSet());
        Set<String> otherCompleted = taskProgressRepository.findByUserIdAndMode(otherUserId, mode).stream()
                .filter(TaskProgress::isCompleted).map(TaskProgress::getTaskId).collect(Collectors.toSet());

        Map<String, int[]> traderCounts = new LinkedHashMap<>(); // [selfCompleted, otherCompleted, total]
        List<TaskDiffItem> onlySelf = new ArrayList<>();
        List<TaskDiffItem> onlyOther = new ArrayList<>();
        int total = 0;

        for (Map.Entry<String, JsonNode> entry : tasksNode.properties()) {
            String taskId = entry.getKey();
            JsonNode task = entry.getValue();
            String traderId = task.path("trader").asString("");

            boolean iCompleted = selfCompleted.contains(taskId);
            boolean theyCompleted = otherCompleted.contains(taskId);

            total++;
            traderCounts.computeIfAbsent(traderId, k -> new int[3]);
            int[] counts = traderCounts.get(traderId);
            counts[2]++;
            if (iCompleted) counts[0]++;
            if (theyCompleted) counts[1]++;

            if (iCompleted && !theyCompleted) {
                String nameKey = task.path("name").asString(taskId);
                String name = locale.path(nameKey).asString(taskId);
                onlySelf.add(new TaskDiffItem(taskId, name, traderId));
            } else if (theyCompleted && !iCompleted) {
                String nameKey = task.path("name").asString(taskId);
                String name = locale.path(nameKey).asString(taskId);
                onlyOther.add(new TaskDiffItem(taskId, name, traderId));
            }
        }

        List<TraderComparisonProgress> byTrader = traderCounts.entrySet().stream()
                .map(e -> new TraderComparisonProgress(e.getKey(), e.getValue()[0], e.getValue()[1], e.getValue()[2]))
                .toList();

        return new TaskComparisonResponse(
                otherUsername, selfCompleted.size(), otherCompleted.size(), total,
                byTrader, onlySelf, onlyOther
        );
    }

    @Transactional
    public List<UpcomingUnlock> setCompleted(Long userId, GameMode mode, String taskId, boolean completed, String lang) {
        TaskProgress progress = taskProgressRepository.findByUserIdAndTaskIdAndMode(userId, taskId, mode)
                .orElseGet(() -> TaskProgress.builder()
                        .user(userRepository.getReferenceById(userId))
                        .taskId(taskId)
                        .mode(mode)
                        .build());

        progress.setCompleted(completed);
        progress.setCompletedAt(completed ? LocalDateTime.now() : null);
        taskProgressRepository.save(progress);

        if (!completed) {
            return List.of();
        }

        return findUpcomingUnlocks(mode, userId, taskId, lang);
    }

    private List<UpcomingUnlock> findUpcomingUnlocks(GameMode mode, Long userId, String justCompletedTaskId, String lang) {
        JsonNode catalog = self.getTasksCatalog(mode);
        JsonNode locale = self.getTasksLocale(mode, lang).path("data");
        JsonNode tasksNode = catalog.path("data").path("tasks");

        Set<String> completedIds = taskProgressRepository.findByUserIdAndMode(userId, mode).stream()
                .filter(TaskProgress::isCompleted)
                .map(TaskProgress::getTaskId)
                .collect(Collectors.toSet());

        List<UpcomingUnlock> result = new ArrayList<>();

        for (Map.Entry<String, JsonNode> entry : tasksNode.properties()) {
            String candidateId = entry.getKey();
            if (completedIds.contains(candidateId)) continue;

            JsonNode candidate = entry.getValue();
            boolean dependsOnJustCompleted = false;
            boolean allOtherRequirementsMet = true;

            for (JsonNode req : candidate.path("taskRequirements")) {
                List<String> statuses = new ArrayList<>();
                for (JsonNode s : req.path("status")) statuses.add(s.asString());
                if (!statuses.contains("complete")) continue;

                String reqTaskId = req.path("task").asString("");
                if (reqTaskId.equals(justCompletedTaskId)) {
                    dependsOnJustCompleted = true;
                } else if (!completedIds.contains(reqTaskId)) {
                    allOtherRequirementsMet = false;
                }
            }

            if (!dependsOnJustCompleted || !allOtherRequirementsMet) continue;

            int delayMin = candidate.path("availableDelaySecondsMin").asInt(0);
            int delayMax = candidate.path("availableDelaySecondsMax").asInt(0);
            if (delayMin <= 0 && delayMax <= 0) continue;

            String nameKey = candidate.path("name").asString(candidateId);
            String name = locale.path(nameKey).asString(candidateId);

            result.add(new UpcomingUnlock(candidateId, name, delayMin, delayMax));
        }

        return result;
    }

    private List<TaskRewardItem> extractRewardItems(JsonNode rewardsNode) {
        List<TaskRewardItem> items = new ArrayList<>();
        for (JsonNode item : rewardsNode.path("items")) {
            items.add(new TaskRewardItem(item.path("item").asString(""), item.path("count").asInt(0)));
        }
        return items;
    }

    private Integer nullableInt(JsonNode node) {
        return node.isMissingNode() || node.isNull() ? null : node.asInt();
    }

    private Boolean nullableBool(JsonNode node) {
        return node.isMissingNode() || node.isNull() ? null : node.asBoolean();
    }
}