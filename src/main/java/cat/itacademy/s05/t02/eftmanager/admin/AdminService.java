package cat.itacademy.s05.t02.eftmanager.admin;

import cat.itacademy.s05.t02.eftmanager.user.User;
import cat.itacademy.s05.t02.eftmanager.user.UserRepository;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AdminService {

    private final UserRepository userRepository;

    public AdminService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public List<AdminUserResponse> listUsers() {
        return userRepository.findAll().stream()
                .map(u -> new AdminUserResponse(u.getId(), u.getUsername(), u.getEmail(), u.getRole().name(), u.getCreatedAt()))
                .toList();
    }

    @Transactional
    public void deleteUser(Long targetId, Long currentUserId) {
        if (targetId.equals(currentUserId)) {
            throw new AdminSelfActionException("No puedes eliminar tu propia cuenta");
        }
        if (!userRepository.existsById(targetId)) {
            throw new UsernameNotFoundException("Usuario no encontrado");
        }
        userRepository.deleteById(targetId);
    }

    @Transactional
    public void updateRole(Long targetId, String newRole, Long currentUserId) {
        if (targetId.equals(currentUserId)) {
            throw new AdminSelfActionException("No puedes cambiar tu propio rol");
        }

        User user = userRepository.findById(targetId)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado"));

        User.Role role;
        try {
            role = User.Role.valueOf(newRole.toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Rol inválido: " + newRole);
        }

        user.setRole(role);
        userRepository.save(user);
    }
}