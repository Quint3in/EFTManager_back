package cat.itacademy.s05.t02.eftmanager.dto;

import java.util.List;

public record HideoutLevelRequirement(
        int level,
        int constructionTimeSeconds,
        List<HideoutItemRequirement> itemRequirements,
        List<HideoutTraderRequirement> traderRequirements,
        List<HideoutStationRequirement> stationRequirements,
        List<HideoutSkillRequirement> skillRequirements
) {}