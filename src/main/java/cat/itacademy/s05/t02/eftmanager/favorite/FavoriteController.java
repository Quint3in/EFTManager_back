package cat.itacademy.s05.t02.eftmanager.favorite;

import cat.itacademy.s05.t02.eftmanager.common.CurrentUserResolver;
import cat.itacademy.s05.t02.eftmanager.common.GameMode;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/favorites")
public class FavoriteController {

    private final FavoriteService favoriteService;
    private final CurrentUserResolver currentUserResolver;

    public FavoriteController(FavoriteService favoriteService, CurrentUserResolver currentUserResolver) {
        this.favoriteService = favoriteService;
        this.currentUserResolver = currentUserResolver;
    }

    @GetMapping
    public List<String> getFavorites(@RequestParam String mode, @AuthenticationPrincipal UserDetails userDetails) {
        GameMode gameMode = GameMode.valueOf(mode.toUpperCase());
        return favoriteService.getFavoriteItemIds(currentUserResolver.resolveUserId(userDetails), gameMode);
    }

    @PostMapping
    public ResponseEntity<Void> addFavorite(@RequestParam String mode, @AuthenticationPrincipal UserDetails userDetails,
                                            @Valid @RequestBody AddFavoriteRequest request) {
        GameMode gameMode = GameMode.valueOf(mode.toUpperCase());
        favoriteService.addFavorite(currentUserResolver.resolveUserId(userDetails), gameMode, request.itemId());
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @DeleteMapping("/{itemId}")
    public ResponseEntity<Void> removeFavorite(@RequestParam String mode, @PathVariable String itemId,
                                               @AuthenticationPrincipal UserDetails userDetails) {
        GameMode gameMode = GameMode.valueOf(mode.toUpperCase());
        favoriteService.removeFavorite(currentUserResolver.resolveUserId(userDetails), gameMode, itemId);
        return ResponseEntity.noContent().build();
    }
}