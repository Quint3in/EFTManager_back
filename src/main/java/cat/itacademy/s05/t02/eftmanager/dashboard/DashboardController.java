package cat.itacademy.s05.t02.eftmanager.dashboard;

import cat.itacademy.s05.t02.eftmanager.common.CurrentUserResolver;
import cat.itacademy.s05.t02.eftmanager.common.TarkovMetadataService;
import cat.itacademy.s05.t02.eftmanager.hideout.HideoutModeSummary;
import cat.itacademy.s05.t02.eftmanager.task.TaskModeSummary;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;
    private final CurrentUserResolver currentUserResolver;
    private final TarkovMetadataService tarkovMetadataService;

    @GetMapping("/hideout-summary")
    public List<HideoutModeSummary> getHideoutSummary(@AuthenticationPrincipal UserDetails userDetails) {
        return dashboardService.getHideoutSummary(currentUserResolver.resolveUserId(userDetails));
    }

    @GetMapping("/top-favorites")
    public List<ModeTopFavorites> getTopFavorites(@RequestParam(required = false) String lang,
                                                  @AuthenticationPrincipal UserDetails userDetails) {
        return dashboardService.getTopFavorites(
                currentUserResolver.resolveUserId(userDetails), tarkovMetadataService.resolveLanguage(lang));
    }

    @GetMapping("/tasks-summary")
    public List<TaskModeSummary> getTasksSummary(@AuthenticationPrincipal UserDetails userDetails) {
        return dashboardService.getTasksSummary(currentUserResolver.resolveUserId(userDetails));
    }
}