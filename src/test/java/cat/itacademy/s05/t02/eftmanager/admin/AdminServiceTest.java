package cat.itacademy.s05.t02.eftmanager.admin;

import cat.itacademy.s05.t02.eftmanager.user.User;
import cat.itacademy.s05.t02.eftmanager.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminServiceTest {

    @Mock private UserRepository userRepository;
    private AdminService adminService;

    @BeforeEach
    void setUp() {
        adminService = new AdminService(userRepository);
    }

    @Test
    void listUsers_mapsAllUsersToResponse() {
        User user = User.builder().id(1L).username("joel").email("joel@test.com").role(User.Role.USER).build();
        when(userRepository.findAll()).thenReturn(List.of(user));

        List<AdminUserResponse> result = adminService.listUsers();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).username()).isEqualTo("joel");
        assertThat(result.get(0).role()).isEqualTo("USER");
    }

    @Test
    void deleteUser_targetingSelf_throwsAdminSelfActionException() {
        assertThatThrownBy(() -> adminService.deleteUser(1L, 1L))
                .isInstanceOf(AdminSelfActionException.class);

        verify(userRepository, never()).deleteById(any());
    }

    @Test
    void deleteUser_targetingUnknownUser_throwsUsernameNotFoundException() {
        when(userRepository.existsById(2L)).thenReturn(false);

        assertThatThrownBy(() -> adminService.deleteUser(2L, 1L))
                .isInstanceOf(UsernameNotFoundException.class);
    }

    @Test
    void deleteUser_validTarget_deletesSuccessfully() {
        when(userRepository.existsById(2L)).thenReturn(true);

        adminService.deleteUser(2L, 1L);

        verify(userRepository).deleteById(2L);
    }

    @Test
    void updateRole_targetingSelf_throwsAdminSelfActionException() {
        assertThatThrownBy(() -> adminService.updateRole(1L, "ADMIN", 1L))
                .isInstanceOf(AdminSelfActionException.class);

        verify(userRepository, never()).save(any());
    }

    @Test
    void updateRole_withInvalidRole_throwsIllegalArgumentException() {
        User target = User.builder().id(2L).role(User.Role.USER).build();
        when(userRepository.findById(2L)).thenReturn(Optional.of(target));

        assertThatThrownBy(() -> adminService.updateRole(2L, "SUPERADMIN", 1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("SUPERADMIN");
    }

    @Test
    void updateRole_withValidRole_updatesAndSaves() {
        User target = User.builder().id(2L).role(User.Role.USER).build();
        when(userRepository.findById(2L)).thenReturn(Optional.of(target));

        adminService.updateRole(2L, "ADMIN", 1L);

        assertThat(target.getRole()).isEqualTo(User.Role.ADMIN);
        verify(userRepository).save(target);
    }
}