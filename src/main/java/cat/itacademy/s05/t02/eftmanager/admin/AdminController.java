package cat.itacademy.s05.t02.eftmanager.admin;

import cat.itacademy.s05.t02.eftmanager.common.CurrentUserResolver;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;
    private final CurrentUserResolver currentUserResolver;

    @GetMapping
    public List<AdminUserResponse> listUsers() {
        return adminService.listUsers();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id, @AuthenticationPrincipal UserDetails userDetails) {
        Long currentUserId = currentUserResolver.resolveUserId(userDetails);
        adminService.deleteUser(id, currentUserId);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/role")
    public ResponseEntity<Void> updateRole(@PathVariable Long id,
                                           @Valid @RequestBody UpdateRoleRequest request,
                                           @AuthenticationPrincipal UserDetails userDetails) {
        Long currentUserId = currentUserResolver.resolveUserId(userDetails);
        adminService.updateRole(id, request.role(), currentUserId);
        return ResponseEntity.noContent().build();
    }
}