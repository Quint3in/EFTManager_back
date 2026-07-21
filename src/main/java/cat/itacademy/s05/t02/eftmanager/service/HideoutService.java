package cat.itacademy.s05.t02.eftmanager.service;

import cat.itacademy.s05.t02.eftmanager.dto.*;
import cat.itacademy.s05.t02.eftmanager.entity.HideoutProgress;
import cat.itacademy.s05.t02.eftmanager.exception.ExternalApiException;
import cat.itacademy.s05.t02.eftmanager.exception.InvalidHideoutLevelException;
import cat.itacademy.s05.t02.eftmanager.exception.StationNotFoundException;
import cat.itacademy.s05.t02.eftmanager.repository.HideoutProgressRepository;
import cat.itacademy.s05.t02.eftmanager.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import tools.jackson.databind.JsonNode;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class HideoutService {

    private static final String STASH_NORMALIZED_NAME = "stash";
    private static final int STASH_MIN_LEVEL = 1;

    private final RestClient restClient;
    private final HideoutProgressRepository hideoutProgressRepository;
    private final UserRepository userRepository;
    private final String language;

    public HideoutService(RestClient.Builder restClientBuilder,
                          @Value("${tarkov.api.base-url}") String tarkovApiBaseUrl,
                          @Value("${tarkov.api.language}") String language,
                          HideoutProgressRepository hideoutProgressRepository,
                          UserRepository userRepository) {
        this.restClient = restClientBuilder.baseUrl(tarkovApiBaseUrl).build();
        this.language = language;
        this.hideoutProgressRepository = hideoutProgressRepository;
        this.userRepository = userRepository;
    }

    @Cacheable(value = "hideoutData", key = "#mode")
    public JsonNode getHideout(GameMode mode) {
        try {
            return restClient.get()
                    .uri("/{externalMode}/hideout", mode.getExternalPath())
                    .retrieve()
                    .body(JsonNode.class);
        } catch (RestClientException ex) {
            throw new ExternalApiException("No se pudo obtener los datos del hideout desde Tarkov.dev", ex);
        }
    }

    @Cacheable(value = "hideoutLocale", key = "#mode")
    public JsonNode getHideoutLocale(GameMode mode) {
        try {
            return restClient.get()
                    .uri("/{externalMode}/hideout_{lang}", mode.getExternalPath(), language)
                    .retrieve()
                    .body(JsonNode.class);
        } catch (RestClientException ex) {
            throw new ExternalApiException("No se pudo obtener las traducciones del hideout desde Tarkov.dev", ex);
        }
    }

    @CacheEvict(value = {"hideoutData", "hideoutLocale"}, allEntries = true)
    @Scheduled(fixedRate = 24, timeUnit = TimeUnit.HOURS)
    public void evictHideoutCache() {
    }

    public List<HideoutStationResponse> getHideoutWithProgress(GameMode mode, Long userId) {
        JsonNode catalog = getHideout(mode);
        JsonNode locale = getHideoutLocale(mode).path("data");
        JsonNode stationsNode = catalog.path("data");

        Map<String, HideoutProgress> progressByStation = hideoutProgressRepository
                .findByUserIdAndMode(userId, mode).stream()
                .collect(Collectors.toMap(HideoutProgress::getStationId, p -> p));

        List<HideoutStationResponse> result = new ArrayList<>();

        for (Map.Entry<String, JsonNode> entry : stationsNode.properties()) {
            String stationId = entry.getKey();
            JsonNode station = entry.getValue();

            int minLevel = getMinLevel(station);
            int currentLevel = progressByStation.containsKey(stationId)
                    ? Math.max(progressByStation.get(stationId).getLevel(), minLevel)
                    : minLevel;

            result.add(buildStationResponse(stationId, station, stationsNode, locale, currentLevel));
        }

        return result;
    }

    @Transactional
    public HideoutStationResponse updateProgress(Long userId, GameMode mode, String stationId, int requestedLevel) {
        JsonNode catalog = getHideout(mode);
        JsonNode locale = getHideoutLocale(mode).path("data");
        JsonNode stationsNode = catalog.path("data");
        JsonNode station = stationsNode.path(stationId);

        if (station.isMissingNode()) {
            throw new StationNotFoundException("Estación no encontrada: " + stationId);
        }

        int minLevel = getMinLevel(station);
        int maxLevel = computeMaxLevel(station);

        if (requestedLevel < minLevel) {
            throw new InvalidHideoutLevelException(
                    "Nivel inválido para '" + stationId + "'. El nivel mínimo permitido es " + minLevel);
        }
        if (requestedLevel > maxLevel) {
            throw new InvalidHideoutLevelException(
                    "Nivel inválido para '" + stationId + "'. Debe estar entre " + minLevel + " y " + maxLevel);
        }

        HideoutProgress progress = hideoutProgressRepository
                .findByUserIdAndStationIdAndMode(userId, stationId, mode)
                .orElseGet(() -> HideoutProgress.builder()
                        .user(userRepository.getReferenceById(userId))
                        .stationId(stationId)
                        .mode(mode)
                        .build());

        progress.setLevel(requestedLevel);
        hideoutProgressRepository.save(progress);

        return buildStationResponse(stationId, station, stationsNode, locale, requestedLevel);
    }

    private int getMinLevel(JsonNode station) {
        String normalizedName = station.path("normalizedName").asString("");
        return STASH_NORMALIZED_NAME.equals(normalizedName) ? STASH_MIN_LEVEL : 0;
    }

    private HideoutStationResponse buildStationResponse(String stationId, JsonNode station, JsonNode allStations,
                                                        JsonNode locale, int currentLevel) {
        int maxLevel = computeMaxLevel(station);
        int minLevel = getMinLevel(station);
        List<HideoutLevelRequirement> remaining = computeRemainingRequirements(station, currentLevel, maxLevel, allStations, locale);

        String nameKey = station.path("name").asString("");
        String normalizedName = station.path("normalizedName").asString("");
        String resolvedName = locale.path(nameKey).asString(normalizedName);

        return new HideoutStationResponse(
                stationId,
                resolvedName,
                normalizedName,
                station.path("imageLink").asString(null),
                currentLevel,
                minLevel,
                maxLevel,
                remaining
        );
    }

    private int computeMaxLevel(JsonNode station) {
        int max = 0;
        for (JsonNode levelNode : station.path("levels")) {
            int level = levelNode.path("level").asInt(0);
            if (level > max) max = level;
        }
        return max;
    }

    private List<HideoutLevelRequirement> computeRemainingRequirements(JsonNode station, int currentLevel, int maxLevel,
                                                                       JsonNode allStations, JsonNode locale) {
        if (currentLevel >= maxLevel) {
            return List.of();
        }

        List<HideoutLevelRequirement> remaining = new ArrayList<>();

        for (JsonNode levelNode : station.path("levels")) {
            int level = levelNode.path("level").asInt(0);
            if (level <= currentLevel) continue;

            int constructionTime = levelNode.path("constructionTime").asInt(0);

            List<HideoutItemRequirement> items = new ArrayList<>();
            for (JsonNode itemReq : levelNode.path("itemRequirements")) {
                items.add(new HideoutItemRequirement(
                        itemReq.path("item").asString(""),
                        itemReq.path("count").asInt(0),
                        itemReq.path("attributes").path("foundInRaid").asBoolean(false)
                ));
            }

            List<HideoutTraderRequirement> traderReqs = new ArrayList<>();
            for (JsonNode t : levelNode.path("traderRequirements")) {
                String traderId = t.path("trader").asString("");
                int reqLevel = t.path("value").asInt(0);
                traderReqs.add(new HideoutTraderRequirement(traderId, reqLevel));
            }

            List<HideoutStationRequirement> stationReqs = new ArrayList<>();
            for (JsonNode s : levelNode.path("stationLevelRequirements")) {
                String otherStationId = s.path("station").asString("");
                int reqLevel = s.path("level").asInt(0);
                JsonNode otherStation = allStations.path(otherStationId);
                String otherName = resolveStationName(otherStationId, allStations, locale);
                String otherImage = otherStation.path("imageLink").asString(null);
                stationReqs.add(new HideoutStationRequirement(otherStationId, otherName, otherImage, reqLevel));
            }

            List<HideoutSkillRequirement> skillReqs = new ArrayList<>();
            for (JsonNode sk : levelNode.path("skillRequirements")) {
                String skill = sk.path("skill").asString("");
                int reqLevel = sk.path("level").asInt(0);
                skillReqs.add(new HideoutSkillRequirement(formatSkillName(skill), reqLevel));
            }

            remaining.add(new HideoutLevelRequirement(level, constructionTime, items, traderReqs, stationReqs, skillReqs));
        }

        remaining.sort(Comparator.comparingInt(HideoutLevelRequirement::level));
        return remaining;
    }

    private String resolveStationName(String stationId, JsonNode allStations, JsonNode locale) {
        JsonNode other = allStations.path(stationId);
        if (other.isMissingNode()) return stationId;
        String nameKey = other.path("name").asString("");
        String normalizedName = other.path("normalizedName").asString(stationId);
        return locale.path(nameKey).asString(normalizedName);
    }

    private String formatSkillName(String skill) {
        if (skill == null || skill.isBlank()) return skill;
        return skill.replaceAll("(?<=[a-z])(?=[A-Z])", " ");
    }
}