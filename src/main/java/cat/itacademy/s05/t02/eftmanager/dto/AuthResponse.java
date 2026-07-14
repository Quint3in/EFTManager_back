package cat.itacademy.s05.t02.eftmanager.dto;

public record AuthResponse(
        String token,
        String username,
        String email,
        String role
) {}