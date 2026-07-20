package cat.itacademy.s05.t02.eftmanager.controller;

import cat.itacademy.s05.t02.eftmanager.dto.HideoutStationResponse;
import cat.itacademy.s05.t02.eftmanager.dto.UpdateHideoutProgressRequest;
import cat.itacademy.s05.t02.eftmanager.entity.User;
import cat.itacademy.s05.t02.eftmanager.repository.UserRepository;
import cat.itacademy.s05.t02.eftmanager.service.GameMode;
import cat.itacademy.s05.t02.eftmanager.service.HideoutService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/hideout")
@RequiredArgsConstructor
public class HideoutController {

    private final HideoutService hideoutService;
    private final UserRepository userRepository;

    @GetMapping("/{mode}")
    public List<HideoutStationResponse> getHideout(@PathVariable String mode,
                                                   @AuthenticationPrincipal UserDetails userDetails) {
        GameMode gameMode = GameMode.valueOf(mode.toUpperCase());
        Long userId = resolveUserId(userDetails);
        return hideoutService.getHideoutWithProgress(gameMode, userId);
    }

    @PutMapping("/{mode}/{stationId}")
    public HideoutStationResponse updateProgress(@PathVariable String mode,
                                                 @PathVariable String stationId,
                                                 @Valid @RequestBody UpdateHideoutProgressRequest request,
                                                 @AuthenticationPrincipal UserDetails userDetails) {
        GameMode gameMode = GameMode.valueOf(mode.toUpperCase());
        Long userId = resolveUserId(userDetails);
        return hideoutService.updateProgress(userId, gameMode, stationId, request.level());
    }

    private Long resolveUserId(UserDetails userDetails) {
        User user = userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado"));
        return user.getId();
    }
}