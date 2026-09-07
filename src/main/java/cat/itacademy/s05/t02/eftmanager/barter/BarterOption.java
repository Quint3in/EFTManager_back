package cat.itacademy.s05.t02.eftmanager.barter;

import java.util.List;

public record BarterOption(
        String traderId,
        int minTraderLevel,
        List<BarterRequiredItem> requiredItems,
        int offeredCount,
        Integer buyLimit,
        String taskUnlockId
) {}