package cat.itacademy.s05.t02.eftmanager.dashboard;

import cat.itacademy.s05.t02.eftmanager.item.ItemSummaryResponse;

import java.util.List;

public record ModeTopFavorites(String mode, List<ItemSummaryResponse> items) {}