package cat.itacademy.s05.t02.eftmanager.dto;

public record HideoutItemRequirement(
        String itemId,
        int count,
        boolean foundInRaid
) {}