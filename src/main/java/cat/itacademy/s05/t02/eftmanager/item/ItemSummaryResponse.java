package cat.itacademy.s05.t02.eftmanager.item;

public record ItemSummaryResponse(
        String id,
        String name,
        String normalizedName,
        String iconLink,
        int avg24hPrice,
        int lastLowPrice,
        int changeLast48h,
        double changeLast48hPercent,
        int basePrice
) {}