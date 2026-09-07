package cat.itacademy.s05.t02.eftmanager.dashboard;

import cat.itacademy.s05.t02.eftmanager.common.GameMode;
import cat.itacademy.s05.t02.eftmanager.favorite.FavoriteService;
import cat.itacademy.s05.t02.eftmanager.hideout.HideoutModeSummary;
import cat.itacademy.s05.t02.eftmanager.hideout.HideoutService;
import cat.itacademy.s05.t02.eftmanager.item.ItemService;
import cat.itacademy.s05.t02.eftmanager.item.ItemSummaryResponse;
import cat.itacademy.s05.t02.eftmanager.task.TaskModeSummary;
import cat.itacademy.s05.t02.eftmanager.task.TaskService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class DashboardService {

    private final HideoutService hideoutService;
    private final FavoriteService favoriteService;
    private final ItemService itemService;
    private final TaskService taskService;

    public DashboardService(HideoutService hideoutService, FavoriteService favoriteService, ItemService itemService, TaskService taskService) {
        this.hideoutService = hideoutService;
        this.favoriteService = favoriteService;
        this.itemService = itemService;
        this.taskService = taskService;
    }

    public List<HideoutModeSummary> getHideoutSummary(Long userId) {
        return hideoutService.getProgressSummary(userId);
    }

    public List<ModeTopFavorites> getTopFavorites(Long userId, String lang) {
        List<ModeTopFavorites> result = new ArrayList<>();

        for (GameMode mode : GameMode.values()) {
            List<String> favIds = favoriteService.getFavoriteItemIds(userId, mode);
            List<ItemSummaryResponse> topItems = List.of();

            if (!favIds.isEmpty()) {
                topItems = itemService.getSummariesByIds(mode, favIds, lang).stream()
                        .sorted(Comparator.comparingInt(DashboardService::effectiveValue).reversed())
                        .limit(3)
                        .toList();
            }

            result.add(new ModeTopFavorites(mode.name().toLowerCase(), topItems));
        }

        return result;
    }

    private static int effectiveValue(ItemSummaryResponse item) {
        return item.avg24hPrice() > 0 ? item.avg24hPrice() : item.basePrice();
    }

    public List<TaskModeSummary> getTasksSummary(Long userId) {
        return taskService.getProgressSummary(userId);
    }
}