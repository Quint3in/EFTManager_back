package cat.itacademy.s05.t02.eftmanager.admin;

import java.time.LocalDateTime;

public record AdminUserResponse(
        Long id,
        String username,
        String email,
        String role,
        LocalDateTime createdAt
) {}