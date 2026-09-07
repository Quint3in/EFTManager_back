package cat.itacademy.s05.t02.eftmanager.task;

import cat.itacademy.s05.t02.eftmanager.common.CurrentUserResolver;
import cat.itacademy.s05.t02.eftmanager.common.GameMode;
import cat.itacademy.s05.t02.eftmanager.common.TarkovMetadataService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tasks")
@RequiredArgsConstructor
public class TaskController {

    private final TaskService taskService;
    private final CurrentUserResolver currentUserResolver;
    private final TarkovMetadataService tarkovMetadataService;

    @GetMapping("/{mode}")
    public List<TaskSummaryResponse> getTasks(@PathVariable String mode,
                                              @RequestParam(required = false) String query,
                                              @RequestParam(required = false) String traderId,
                                              @RequestParam(required = false) String mapId,
                                              @RequestParam(required = false) String status,
                                              @RequestParam(defaultValue = "false") boolean kappaOnly,
                                              @RequestParam(defaultValue = "false") boolean lightkeeperOnly,
                                              @RequestParam(required = false) String sort,
                                              @RequestParam(required = false) String lang,
                                              @AuthenticationPrincipal UserDetails userDetails) {
        GameMode gameMode = GameMode.valueOf(mode.toUpperCase());
        Long userId = currentUserResolver.resolveUserId(userDetails);
        return taskService.getTasks(gameMode, userId, query, traderId, mapId, status, kappaOnly, lightkeeperOnly, sort,
                tarkovMetadataService.resolveLanguage(lang));
    }

    @GetMapping("/{mode}/{taskId}")
    public TaskResponse getTaskDetail(@PathVariable String mode, @PathVariable String taskId,
                                      @RequestParam(required = false) String lang,
                                      @AuthenticationPrincipal UserDetails userDetails) {
        GameMode gameMode = GameMode.valueOf(mode.toUpperCase());
        Long userId = currentUserResolver.resolveUserId(userDetails);
        return taskService.getTaskDetail(gameMode, taskId, userId, tarkovMetadataService.resolveLanguage(lang));
    }

    @GetMapping("/{mode}/quest-items")
    public List<QuestItemResponse> getQuestItems(@PathVariable String mode, @RequestParam List<String> ids,
                                                 @RequestParam(required = false) String lang) {
        GameMode gameMode = GameMode.valueOf(mode.toUpperCase());
        return taskService.getQuestItems(gameMode, ids, tarkovMetadataService.resolveLanguage(lang));
    }

    @GetMapping("/{mode}/achievements")
    public List<AchievementResponse> getAchievements(@PathVariable String mode, @RequestParam List<String> ids,
                                                     @RequestParam(required = false) String lang) {
        GameMode gameMode = GameMode.valueOf(mode.toUpperCase());
        return taskService.getAchievements(gameMode, ids, tarkovMetadataService.resolveLanguage(lang));
    }

    @PutMapping("/{mode}/{taskId}/complete")
    public List<UpcomingUnlock> setCompleted(@PathVariable String mode, @PathVariable String taskId,
                                             @RequestParam(required = false) String lang,
                                             @RequestBody UpdateTaskProgressRequest request,
                                             @AuthenticationPrincipal UserDetails userDetails) {
        GameMode gameMode = GameMode.valueOf(mode.toUpperCase());
        Long userId = currentUserResolver.resolveUserId(userDetails);
        return taskService.setCompleted(userId, gameMode, taskId, request.completed(),
                tarkovMetadataService.resolveLanguage(lang));
    }

    @GetMapping("/{mode}/compare")
    public TaskComparisonResponse compare(@PathVariable String mode, @RequestParam String username,
                                          @RequestParam(required = false) String lang,
                                          @AuthenticationPrincipal UserDetails userDetails) {
        GameMode gameMode = GameMode.valueOf(mode.toUpperCase());
        Long userId = currentUserResolver.resolveUserId(userDetails);
        return taskService.compareProgress(gameMode, userId, username, tarkovMetadataService.resolveLanguage(lang));
    }
}