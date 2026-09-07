package cat.itacademy.s05.t02.eftmanager.user;

import java.time.LocalDateTime;

public record UserProfileResponse(
        Long id,
        String username,
        String email,
        String role,
        LocalDateTime createdAt
) {}