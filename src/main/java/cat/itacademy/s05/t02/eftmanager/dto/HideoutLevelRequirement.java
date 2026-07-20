package cat.itacademy.s05.t02.eftmanager.dto;

import java.util.List;

public record HideoutLevelRequirement(
        int level,
        List<HideoutItemRequirement> itemRequirements
) {}