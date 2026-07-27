package cat.itacademy.s05.t02.eftmanager.item;

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