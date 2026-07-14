package cat.itacademy.s05.t02.eftmanager.service;

import cat.itacademy.s05.t02.eftmanager.dto.AuthResponse;
import cat.itacademy.s05.t02.eftmanager.dto.LoginRequest;
import cat.itacademy.s05.t02.eftmanager.dto.RegisterRequest;
import cat.itacademy.s05.t02.eftmanager.entity.User;
import cat.itacademy.s05.t02.eftmanager.exception.UserAlreadyExistsException;
import cat.itacademy.s05.t02.eftmanager.repository.UserRepository;
import cat.itacademy.s05.t02.eftmanager.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final UserDetailsService userDetailsService;

    public AuthResponse register(RegisterRequest request) {

        if (userRepository.existsByUsername(request.username())) {
            throw new UserAlreadyExistsException("El username ya está en uso");
        }
        if (userRepository.existsByEmail(request.email())) {
            throw new UserAlreadyExistsException("El email ya está en uso");
        }

        User newUser = User.builder()
                .username(request.username())
                .email(request.email())
                .password(passwordEncoder.encode(request.password()))
                .build();

        userRepository.save(newUser);

        UserDetails userDetails = userDetailsService.loadUserByUsername(newUser.getUsername());
        String token = jwtService.generateToken(userDetails);

        return new AuthResponse(token, newUser.getUsername(), newUser.getEmail(), newUser.getRole().name());
    }

    public AuthResponse login(LoginRequest request) {

        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.username(), request.password())
        );

        UserDetails userDetails = userDetailsService.loadUserByUsername(request.username());
        String token = jwtService.generateToken(userDetails);

        User user = userRepository.findByUsername(request.username())
                .orElseThrow();
        return new AuthResponse(token, user.getUsername(),user.getEmail(), user.getRole().name());
    }
}