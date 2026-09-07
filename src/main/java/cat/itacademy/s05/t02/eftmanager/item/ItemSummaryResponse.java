package cat.itacademy.s05.t02.eftmanager.item;

import java.util.List;

public record ItemSummaryResponse(
        String id,
        String name,
        String normalizedName,
        String iconLink,
        int avg24hPrice,
        int lastLowPrice,
        int changeLast48h,
        double changeLast48hPercent,
        int basePrice,
        boolean canSellOnFlea,
        int minLevelForFlea,
        List<String> categoryIds
) {}