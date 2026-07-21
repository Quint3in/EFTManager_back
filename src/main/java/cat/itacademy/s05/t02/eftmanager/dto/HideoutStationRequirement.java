package cat.itacademy.s05.t02.eftmanager.dto;

public record HideoutStationRequirement(
        String stationId,
        String stationName,
        String imageLink,
        int level
) {}