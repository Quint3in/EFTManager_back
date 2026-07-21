package cat.itacademy.s05.t02.eftmanager.dto;

import java.time.Instant;

public record TraderResponse(
        String id,
        String name,
        String description,
        String normalizedName,
        String currency,
        Instant resetTime,
        String imageLink
) {}