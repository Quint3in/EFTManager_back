package cat.itacademy.s05.t02.eftmanager.item;

public record ItemSellOption(
        String traderId,
        int price,
        String currency,
        String currencyItemId,
        int priceRUB
) {}