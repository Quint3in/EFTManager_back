package cat.itacademy.s05.t02.eftmanager.dto;

import java.util.List;

public record HideoutStationResponse(
        String id,
        String name,
        String normalizedName,
        String imageLink,
        int currentLevel,
        int minLevel,
        int maxLevel,
        List<HideoutLevelRequirement> remainingRequirements
) {}