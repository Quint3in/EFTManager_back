package cat.itacademy.s05.t02.eftmanager.hideout;

public record HideoutItemRequirement(
        String itemId,
        int count,
        boolean foundInRaid
) {}