package cat.itacademy.s05.t02.eftmanager.auth;

import cat.itacademy.s05.t02.eftmanager.security.JwtService;
import cat.itacademy.s05.t02.eftmanager.user.User;
import cat.itacademy.s05.t02.eftmanager.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtService jwtService;
    @Mock private AuthenticationManager authenticationManager;
    @Mock private UserDetailsService userDetailsService;

    private AuthService authService;
    private RegisterRequest registerRequest;

    @BeforeEach
    void setUp() {
        authService = new AuthService(userRepository, passwordEncoder, jwtService, authenticationManager, userDetailsService);
        registerRequest = new RegisterRequest("nuevoUsuario", "nuevo@test.com", "password123");
    }

    @Test
    void register_withNewUsername_savesUserAndReturnsToken() {
        when(userRepository.existsByUsername("nuevoUsuario")).thenReturn(false);
        when(userRepository.existsByEmail("nuevo@test.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("hashedPassword");

        // Simula lo que hace @PrePersist en un guardado real (asignar el rol por defecto),
        // ya que ese callback de JPA no se dispara con un repositorio simulado.
        doAnswer(invocation -> {
            User savedUser = invocation.getArgument(0);
            if (savedUser.getRole() == null) {
                savedUser.setRole(User.Role.USER);
            }
            return savedUser;
        }).when(userRepository).save(any(User.class));

        UserDetails mockUserDetails = mock(UserDetails.class);
        when(userDetailsService.loadUserByUsername("nuevoUsuario")).thenReturn(mockUserDetails);
        when(jwtService.generateToken(mockUserDetails)).thenReturn("fake-jwt-token");

        AuthResponse response = authService.register(registerRequest);

        assertThat(response.token()).isEqualTo("fake-jwt-token");
        assertThat(response.username()).isEqualTo("nuevoUsuario");
        assertThat(response.role()).isEqualTo("USER");
        verify(userRepository).save(any(User.class));
    }

    @Test
    void register_withExistingUsername_throwsUserAlreadyExistsException() {
        when(userRepository.existsByUsername("nuevoUsuario")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(registerRequest))
                .isInstanceOf(UserAlreadyExistsException.class)
                .hasMessageContaining("username");

        verify(userRepository, never()).save(any());
    }

    @Test
    void register_withExistingEmail_throwsUserAlreadyExistsException() {
        when(userRepository.existsByUsername("nuevoUsuario")).thenReturn(false);
        when(userRepository.existsByEmail("nuevo@test.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(registerRequest))
                .isInstanceOf(UserAlreadyExistsException.class)
                .hasMessageContaining("email");

        verify(userRepository, never()).save(any());
    }

    @Test
    void login_withValidCredentials_returnsToken() {
        LoginRequest loginRequest = new LoginRequest("existente", "password123");
        User existingUser = User.builder()
                .id(1L)
                .username("existente")
                .email("existente@test.com")
                .password("hashedPassword")
                .role(User.Role.USER)
                .build();

        UserDetails mockUserDetails = mock(UserDetails.class);
        when(userDetailsService.loadUserByUsername("existente")).thenReturn(mockUserDetails);
        when(jwtService.generateToken(mockUserDetails)).thenReturn("fake-jwt-token");
        when(userRepository.findByUsername("existente")).thenReturn(Optional.of(existingUser));

        AuthResponse response = authService.login(loginRequest);

        assertThat(response.token()).isEqualTo("fake-jwt-token");
        assertThat(response.username()).isEqualTo("existente");
        verify(authenticationManager).authenticate(any());
    }

    @Test
    void login_withInvalidCredentials_throwsBadCredentialsException() {
        LoginRequest loginRequest = new LoginRequest("existente", "wrongPassword");
        when(authenticationManager.authenticate(any()))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        assertThatThrownBy(() -> authService.login(loginRequest))
                .isInstanceOf(BadCredentialsException.class);

        verify(jwtService, never()).generateToken(any());
    }
}