package cat.itacademy.s05.t02.eftmanager.task;

import java.util.List;

public record TaskResponse(
        String id, String name, String normalizedName, String traderId, int minPlayerLevel,
        String wikiLink, String taskImageLink, boolean kappaRequired,
        String mapId, boolean restartable, String factionName,
        int availableDelaySecondsMin, int availableDelaySecondsMax, boolean lightkeeperRequired,
        List<TaskObjectiveResponse> objectives,
        List<TaskRewardItem> rewardItems, List<TaskRewardStanding> traderStandingRewards,
        List<TaskRewardItem> startRewardItems, List<TaskRewardItem> failureRewardItems,
        List<String> achievementRewardIds,
        List<TaskRequirementInfo> taskRequirements, List<TaskTraderRequirement> traderRequirements,
        List<TaskNeededKeys> neededKeys,
        int experience, boolean completed
) {}