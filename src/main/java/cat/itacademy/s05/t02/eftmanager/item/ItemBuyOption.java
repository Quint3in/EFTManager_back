package cat.itacademy.s05.t02.eftmanager.item;

public record ItemBuyOption(
        String traderId,
        int price,
        String currency,
        String currencyItemId,
        int priceRUB,
        int minTraderLevel,
        Integer buyLimit,
        int restockAmount,
        String taskUnlockId
) {}