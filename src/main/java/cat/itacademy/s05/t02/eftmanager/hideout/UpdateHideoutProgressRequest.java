package cat.itacademy.s05.t02.eftmanager.hideout;

import jakarta.validation.constraints.Min;

public record UpdateHideoutProgressRequest(
        @Min(value = 0, message = "El nivel no puede ser negativo")
        int level
) {}