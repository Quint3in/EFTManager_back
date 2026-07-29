package cat.itacademy.s05.t02.eftmanager.auth;

public record AuthResponse(
        String token,
        String username,
        String email,
        String role
) {}