package cat.itacademy.s05.t02.eftmanager.hideout;

public record HideoutStationRequirement(
        String stationId,
        String stationName,
        String imageLink,
        int level
) {}