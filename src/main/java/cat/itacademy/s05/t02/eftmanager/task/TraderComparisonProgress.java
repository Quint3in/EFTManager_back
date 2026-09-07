package cat.itacademy.s05.t02.eftmanager.task;

public record TraderComparisonProgress(String traderId, int selfCompleted, int otherCompleted, int total) {}