package cat.itacademy.s05.t02.eftmanager.task;

import java.util.List;

public record TaskRequirementInfo(
        String taskId, String taskName, List<String> status, Boolean satisfied
) {}