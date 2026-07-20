package cat.itacademy.s05.t02.eftmanager.dto;

import jakarta.validation.constraints.Min;

public record UpdateHideoutProgressRequest(
        @Min(value = 0, message = "El nivel no puede ser negativo")
        int level
) {}