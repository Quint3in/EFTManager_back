package cat.itacademy.s05.t02.eftmanager.admin;

import jakarta.validation.constraints.NotBlank;

public record UpdateRoleRequest(@NotBlank String role) {}