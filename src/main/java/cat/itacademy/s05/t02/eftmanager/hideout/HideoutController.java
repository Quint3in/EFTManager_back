package cat.itacademy.s05.t02.eftmanager.hideout;

import cat.itacademy.s05.t02.eftmanager.common.CurrentUserResolver;
import cat.itacademy.s05.t02.eftmanager.common.GameMode;
import cat.itacademy.s05.t02.eftmanager.common.TarkovMetadataService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;

import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/hideout")
@RequiredArgsConstructor
public class HideoutController {

    private final HideoutService hideoutService;
    private final CurrentUserResolver currentUserResolver;
    private final TarkovMetadataService tarkovMetadataService;

    @GetMapping("/{mode}")
    public List<HideoutStationResponse> getHideout(@PathVariable String mode,
                                                   @RequestParam(required = false) String lang,
                                                   @AuthenticationPrincipal UserDetails userDetails) {
        GameMode gameMode = GameMode.valueOf(mode.toUpperCase());
        Long userId = currentUserResolver.resolveUserId(userDetails);
        return hideoutService.getHideoutWithProgress(gameMode, userId, tarkovMetadataService.resolveLanguage(lang));
    }

    @PutMapping("/{mode}/{stationId}")
    public HideoutStationResponse updateProgress(@PathVariable String mode,
                                                 @PathVariable String stationId,
                                                 @RequestParam(required = false) String lang,
                                                 @Valid @RequestBody UpdateHideoutProgressRequest request,
                                                 @AuthenticationPrincipal UserDetails userDetails) {
        GameMode gameMode = GameMode.valueOf(mode.toUpperCase());
        Long userId = currentUserResolver.resolveUserId(userDetails);
        return hideoutService.updateProgress(userId, gameMode, stationId, request.level(), tarkovMetadataService.resolveLanguage(lang));
    }
}