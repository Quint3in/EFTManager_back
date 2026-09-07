package cat.itacademy.s05.t02.eftmanager.task;

import java.util.List;

public record TaskOtherRequirement(String id, String type, List<String> traderIds) {}