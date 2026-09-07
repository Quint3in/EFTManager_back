package cat.itacademy.s05.t02.eftmanager.task;

import java.util.List;

public record TaskObjectiveResponse(
        String id, String description, String type, boolean optional,
        Integer count, Boolean foundInRaid, Integer minDurability, Integer maxDurability,
        Integer dogTagLevel, List<String> mapIds, List<String> itemIds
) {}