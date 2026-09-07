package cat.itacademy.s05.t02.eftmanager.task;

import java.util.List;

public record TaskComparisonResponse(
        String otherUsername, int selfCompleted, int otherCompleted, int totalTasks,
        List<TraderComparisonProgress> byTrader,
        List<TaskDiffItem> onlySelfCompleted,
        List<TaskDiffItem> onlyOtherCompleted
) {}