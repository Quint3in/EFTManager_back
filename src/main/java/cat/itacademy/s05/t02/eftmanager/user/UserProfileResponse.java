package cat.itacademy.s05.t02.eftmanager.user;

public record UserProfileResponse(
        Long id,
        String username,
        String email,
        String role
) {}