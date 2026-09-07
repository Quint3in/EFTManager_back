package cat.itacademy.s05.t02.eftmanager.task;

import java.util.List;

public record TaskSummaryResponse(
        String id, String name, String traderId, String mapId, int minPlayerLevel,
        String taskImageLink, int experience, boolean kappaRequired, boolean lightkeeperRequired,
        boolean completed, List<TaskUnmetRequirement> unmetRequirements
) {}