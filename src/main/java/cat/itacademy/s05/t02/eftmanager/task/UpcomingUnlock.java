package cat.itacademy.s05.t02.eftmanager.task;

public record UpcomingUnlock(String taskId, String taskName, int delaySecondsMin, int delaySecondsMax) {}