package cat.itacademy.s05.t02.eftmanager.dto;

public record ItemResponse(
        String id,
        String name,
        String shortName,
        String normalizedName,
        double weight,
        int width,
        int height,
        String iconLink
) {}