package cat.itacademy.s05.t02.eftmanager.price;

import java.time.Instant;

public record PricePointResponse(
        int price,
        int priceMin,
        Instant timestamp
) {}