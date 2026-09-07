package cat.itacademy.s05.t02.eftmanager.dashboard;

import cat.itacademy.s05.t02.eftmanager.common.GameMode;
import cat.itacademy.s05.t02.eftmanager.favorite.FavoriteService;
import cat.itacademy.s05.t02.eftmanager.hideout.HideoutService;
import cat.itacademy.s05.t02.eftmanager.item.ItemService;
import cat.itacademy.s05.t02.eftmanager.item.ItemSummaryResponse;
import cat.itacademy.s05.t02.eftmanager.task.TaskService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    @Mock private HideoutService hideoutService;
    @Mock private FavoriteService favoriteService;
    @Mock private ItemService itemService;
    @Mock private TaskService taskService;

    private DashboardService dashboardService;

    @BeforeEach
    void setUp() {
        dashboardService = new DashboardService(hideoutService, favoriteService, itemService, taskService);
    }

    private ItemSummaryResponse item(String id, int avgPrice, int basePrice) {
        return new ItemSummaryResponse(id, "Item " + id, id, null, avgPrice, 0, 0, 0, basePrice, true, 0, List.of());
    }

    @Test
    void getTopFavorites_ordersByEffectivePriceDescending_andLimitsToThree() {
        when(favoriteService.getFavoriteItemIds(1L, GameMode.PVP))
                .thenReturn(List.of("item-a", "item-b", "item-c", "item-d"));
        when(favoriteService.getFavoriteItemIds(1L, GameMode.PVE)).thenReturn(List.of());
        when(favoriteService.getFavoriteItemIds(1L, GameMode.SEASON)).thenReturn(List.of());

        when(itemService.getSummariesByIds(GameMode.PVP, List.of("item-a", "item-b", "item-c", "item-d"), "es"))
                .thenReturn(List.of(
                        item("item-a", 100, 10),
                        item("item-b", 500, 10),
                        item("item-c", 300, 10),
                        item("item-d", 50, 10)
                ));

        List<ModeTopFavorites> result = dashboardService.getTopFavorites(1L, "es");

        ModeTopFavorites pvp = result.stream().filter(r -> r.mode().equals("pvp")).findFirst().orElseThrow();
        assertThat(pvp.items()).hasSize(3);
        assertThat(pvp.items()).extracting("id").containsExactly("item-b", "item-c", "item-a");
    }

    @Test
    void getTopFavorites_usesBasePriceWhenNoMarketPrice() {
        when(favoriteService.getFavoriteItemIds(1L, GameMode.PVP)).thenReturn(List.of("item-a"));
        when(favoriteService.getFavoriteItemIds(1L, GameMode.PVE)).thenReturn(List.of());
        when(favoriteService.getFavoriteItemIds(1L, GameMode.SEASON)).thenReturn(List.of());

        when(itemService.getSummariesByIds(GameMode.PVP, List.of("item-a"), "es"))
                .thenReturn(List.of(item("item-a", 0, 999)));

        List<ModeTopFavorites> result = dashboardService.getTopFavorites(1L, "es");

        ModeTopFavorites pvp = result.stream().filter(r -> r.mode().equals("pvp")).findFirst().orElseThrow();
        assertThat(pvp.items().get(0).basePrice()).isEqualTo(999);
    }

    @Test
    void getTopFavorites_withNoFavorites_returnsEmptyListForThatMode() {
        when(favoriteService.getFavoriteItemIds(1L, GameMode.PVP)).thenReturn(List.of());
        when(favoriteService.getFavoriteItemIds(1L, GameMode.PVE)).thenReturn(List.of());
        when(favoriteService.getFavoriteItemIds(1L, GameMode.SEASON)).thenReturn(List.of());

        List<ModeTopFavorites> result = dashboardService.getTopFavorites(1L, "es");

        assertThat(result).allSatisfy(r -> assertThat(r.items()).isEmpty());
    }
}