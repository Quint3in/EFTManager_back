package cat.itacademy.s05.t02.eftmanager.task;

import cat.itacademy.s05.t02.eftmanager.common.GameMode;
import cat.itacademy.s05.t02.eftmanager.user.User;
import cat.itacademy.s05.t02.eftmanager.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TaskServiceTest {

    @Mock private TaskProgressRepository taskProgressRepository;
    @Mock private UserRepository userRepository;

    private TaskService taskService;
    private final JsonMapper jsonMapper = JsonMapper.builder().build();

    private static final String TASKS_CATALOG = """
        {
          "data": {
            "tasks": {
              "task-1": {
                "name": "task1_key",
                "normalizedName": "task-one",
                "trader": "trader-a",
                "map": "map-1",
                "minPlayerLevel": 10,
                "experience": 500,
                "kappaRequired": false,
                "lightkeeperRequired": false,
                "wikiLink": "https://wiki.example/task-one",
                "restartable": false,
                "factionName": "Any",
                "availableDelaySecondsMin": 3600,
                "availableDelaySecondsMax": 7200,
                "objectives": [
                  {
                    "id": "obj-1",
                    "description": "obj1_desc_key",
                    "type": "giveItem",
                    "optional": false,
                    "count": 3,
                    "foundInRaid": true,
                    "items": ["item-x"]
                  }
                ],
                "finishRewards": {
                  "experience": 500,
                  "items": [ { "item": "item-y", "count": 2 } ],
                  "traderStanding": [ { "trader": "trader-a", "standing": 0.02 } ],
                  "achievement": ["ach-1"]
                },
                "startRewards": { "items": [] },
                "failureOutcome": { "items": [] },
                "taskRequirements": [
                  { "task": "task-3", "status": ["complete"] }
                ],
                "traderRequirements": [
                  { "trader": "trader-a", "value": 2 }
                ],
                "neededKeys": [
                  { "map": "map-1", "keys": ["key-item-1"] }
                ],
                "otherRequirements": []
              },
              "task-2": {
                "name": "task2_key", "normalizedName": "task-two", "trader": "trader-b",
                "kappaRequired": true, "lightkeeperRequired": false,
                "minPlayerLevel": 5, "experience": 200
              },
              "task-3": {
                "name": "task3_key", "normalizedName": "task-three", "trader": "trader-a",
                "kappaRequired": false, "lightkeeperRequired": true,
                "minPlayerLevel": 1, "experience": 100
              }
            }
          }
        }
        """;

    private static final String LOCALE = """
        { "data": { "task1_key": "Tarea Uno", "task2_key": "Tarea Dos", "task3_key": "Tarea Tres", "obj1_desc_key": "Entrega objetos médicos" } }
        """;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = mock(RestClient.Builder.class, RETURNS_DEEP_STUBS);
        JsonNode catalogNode = jsonMapper.readTree(TASKS_CATALOG);
        JsonNode localeNode = jsonMapper.readTree(LOCALE);

        lenient().when(builder.baseUrl(anyString()).build()
                        .get().uri(anyString(), any(Object[].class))
                        .retrieve().body(JsonNode.class))
                .thenReturn(catalogNode, localeNode);

        taskService = new TaskService(builder, "https://json.tarkov.dev", taskProgressRepository, userRepository, null);
        ReflectionTestUtils.setField(taskService, "self", taskService);
    }

    private TaskProgress completed(String taskId, Long userId) {
        return TaskProgress.builder()
                .user(User.builder().id(userId).build())
                .taskId(taskId)
                .mode(GameMode.PVP)
                .completed(true)
                .build();
    }

    // ---- compareProgress ----

    @Test
    void compareProgress_withUnknownUsername_throwsException() {
        when(userRepository.findByUsername("fantasma")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> taskService.compareProgress(GameMode.PVP, 1L, "fantasma", "es"))
                .isInstanceOf(UserNotFoundForComparisonException.class);
    }

    @Test
    void compareProgress_computesCorrectTotalsAndDiffs() {
        User other = User.builder().id(2L).username("amigo").build();
        when(userRepository.findByUsername("amigo")).thenReturn(Optional.of(other));
        when(taskProgressRepository.findByUserIdAndMode(1L, GameMode.PVP)).thenReturn(List.of(completed("task-1", 1L)));
        when(taskProgressRepository.findByUserIdAndMode(2L, GameMode.PVP)).thenReturn(List.of(completed("task-2", 2L)));

        TaskComparisonResponse result = taskService.compareProgress(GameMode.PVP, 1L, "amigo", "es");

        assertThat(result.totalTasks()).isEqualTo(3);
        assertThat(result.onlySelfCompleted()).extracting("taskId").containsExactly("task-1");
        assertThat(result.onlyOtherCompleted()).extracting("taskId").containsExactly("task-2");
    }

    // ---- getTasks ----

    @Test
    void getTasks_withNoFilters_returnsAllTasksWithCompletionStatus() {
        when(taskProgressRepository.findByUserIdAndMode(1L, GameMode.PVP)).thenReturn(List.of(completed("task-2", 1L)));

        List<TaskSummaryResponse> tasks = taskService.getTasks(GameMode.PVP, 1L, null, null, null, "all", false, false, null, "es");

        assertThat(tasks).hasSize(3);
        assertThat(tasks).filteredOn(t -> t.id().equals("task-2")).extracting(TaskSummaryResponse::completed).containsExactly(true);
    }

    @Test
    void getTasks_filteredByKappaOnly_returnsOnlyKappaTasks() {
        when(taskProgressRepository.findByUserIdAndMode(1L, GameMode.PVP)).thenReturn(List.of());

        List<TaskSummaryResponse> tasks = taskService.getTasks(GameMode.PVP, 1L, null, null, null, "all", true, false, null, "es");

        assertThat(tasks).extracting(TaskSummaryResponse::id).containsExactly("task-2");
    }

    @Test
    void getTasks_filteredByLightkeeperOnly_returnsOnlyLightkeeperTasks() {
        when(taskProgressRepository.findByUserIdAndMode(1L, GameMode.PVP)).thenReturn(List.of());

        List<TaskSummaryResponse> tasks = taskService.getTasks(GameMode.PVP, 1L, null, null, null, "all", false, true, null, "es");

        assertThat(tasks).extracting(TaskSummaryResponse::id).containsExactly("task-3");
    }

    @Test
    void getTasks_sortedByExperienceDescending_returnsCorrectOrder() {
        when(taskProgressRepository.findByUserIdAndMode(1L, GameMode.PVP)).thenReturn(List.of());

        List<TaskSummaryResponse> tasks = taskService.getTasks(GameMode.PVP, 1L, null, null, null, "all", false, false, "exp_desc", "es");

        assertThat(tasks).extracting(TaskSummaryResponse::id).containsExactly("task-1", "task-2", "task-3");
    }

    @Test
    void getTasks_withUnmetRequirement_flagsItCorrectly() {
        when(taskProgressRepository.findByUserIdAndMode(1L, GameMode.PVP)).thenReturn(List.of());

        List<TaskSummaryResponse> tasks = taskService.getTasks(GameMode.PVP, 1L, null, null, null, "all", false, false, null, "es");

        TaskSummaryResponse task1 = tasks.stream().filter(t -> t.id().equals("task-1")).findFirst().orElseThrow();
        assertThat(task1.unmetRequirements()).extracting("taskId").containsExactly("task-3");
    }

    // ---- getTaskDetail ----

    @Test
    void getTaskDetail_returnsFullyPopulatedResponse() {
        when(taskProgressRepository.findByUserIdAndMode(1L, GameMode.PVP)).thenReturn(List.of());

        TaskResponse detail = taskService.getTaskDetail(GameMode.PVP, "task-1", 1L, "es");

        assertThat(detail.name()).isEqualTo("Tarea Uno");
        assertThat(detail.objectives()).hasSize(1);
        assertThat(detail.objectives().get(0).description()).isEqualTo("Entrega objetos médicos");
        assertThat(detail.rewardItems()).extracting("itemId").containsExactly("item-y");
        assertThat(detail.traderStandingRewards()).extracting("traderId").containsExactly("trader-a");
        assertThat(detail.achievementRewardIds()).containsExactly("ach-1");
        assertThat(detail.traderRequirements()).extracting("traderId").containsExactly("trader-a");
        assertThat(detail.neededKeys()).extracting("mapId").containsExactly("map-1");
        assertThat(detail.taskRequirements()).extracting("taskId").containsExactly("task-3");
    }

    @Test
    void getTaskDetail_withUnknownTaskId_throwsTaskNotFoundException() {
        assertThatThrownBy(() -> taskService.getTaskDetail(GameMode.PVP, "no-existe", 1L, "es"))
                .isInstanceOf(TaskNotFoundException.class);
    }

    // ---- setCompleted ----

    @Test
    void setCompleted_whenUnlocksAnotherTaskWithDelay_returnsUpcomingUnlock() {
        when(taskProgressRepository.findByUserIdAndTaskIdAndMode(1L, "task-3", GameMode.PVP)).thenReturn(Optional.empty());
        when(userRepository.getReferenceById(1L)).thenReturn(null);
        when(taskProgressRepository.save(any(TaskProgress.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(taskProgressRepository.findByUserIdAndMode(1L, GameMode.PVP)).thenReturn(List.of(completed("task-3", 1L)));

        List<UpcomingUnlock> unlocks = taskService.setCompleted(1L, GameMode.PVP, "task-3", true, "es");

        assertThat(unlocks).extracting(UpcomingUnlock::taskId).containsExactly("task-1");
        assertThat(unlocks.get(0).delaySecondsMin()).isEqualTo(3600);
    }

    @Test
    void setCompleted_markingIncomplete_returnsEmptyUnlockList() {
        TaskProgress existing = completed("task-3", 1L);
        when(taskProgressRepository.findByUserIdAndTaskIdAndMode(1L, "task-3", GameMode.PVP)).thenReturn(Optional.of(existing));
        when(taskProgressRepository.save(any(TaskProgress.class))).thenAnswer(invocation -> invocation.getArgument(0));

        List<UpcomingUnlock> unlocks = taskService.setCompleted(1L, GameMode.PVP, "task-3", false, "es");

        assertThat(unlocks).isEmpty();
    }

    // ---- getProgressSummary ----

    @Test
    void getProgressSummary_countsTasksAndCompletionsPerMode() {
        RestClient.Builder builder = mock(RestClient.Builder.class, RETURNS_DEEP_STUBS);
        JsonNode catalogNode = jsonMapper.readTree(TASKS_CATALOG);

        lenient().when(builder.baseUrl(anyString()).build()
                        .get().uri(anyString(), any(Object[].class))
                        .retrieve().body(JsonNode.class))
                .thenReturn(catalogNode);

        TaskService localService = new TaskService(builder, "https://json.tarkov.dev", taskProgressRepository, userRepository, null);
        ReflectionTestUtils.setField(localService, "self", localService);

        when(taskProgressRepository.findByUserIdAndMode(1L, GameMode.PVP)).thenReturn(List.of(completed("task-1", 1L)));
        when(taskProgressRepository.findByUserIdAndMode(1L, GameMode.PVE)).thenReturn(List.of());
        when(taskProgressRepository.findByUserIdAndMode(1L, GameMode.SEASON)).thenReturn(List.of());

        List<TaskModeSummary> summaries = localService.getProgressSummary(1L);

        TaskModeSummary pvp = summaries.stream().filter(s -> s.mode().equals("pvp")).findFirst().orElseThrow();
        assertThat(pvp.totalTasks()).isEqualTo(3);
        assertThat(pvp.completedTasks()).isEqualTo(1);
    }
}