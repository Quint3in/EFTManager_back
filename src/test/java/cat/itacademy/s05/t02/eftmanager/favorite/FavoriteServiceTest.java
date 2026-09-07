package cat.itacademy.s05.t02.eftmanager.favorite;

import cat.itacademy.s05.t02.eftmanager.common.GameMode;
import cat.itacademy.s05.t02.eftmanager.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FavoriteServiceTest {

    @Mock private FavoriteItemRepository favoriteItemRepository;
    @Mock private UserRepository userRepository;

    private FavoriteService favoriteService;

    @BeforeEach
    void setUp() {
        favoriteService = new FavoriteService(favoriteItemRepository, userRepository);
    }

    @Test
    void addFavorite_whenNotAlreadyFavorite_savesIt() {
        when(favoriteItemRepository.existsByUserIdAndItemIdAndMode(1L, "item-a", GameMode.PVP))
                .thenReturn(false);
        when(userRepository.getReferenceById(1L)).thenReturn(null);

        favoriteService.addFavorite(1L, GameMode.PVP, "item-a");

        verify(favoriteItemRepository).save(any(FavoriteItem.class));
    }

    @Test
    void addFavorite_whenAlreadyFavorite_doesNotSaveAgain() {
        when(favoriteItemRepository.existsByUserIdAndItemIdAndMode(1L, "item-a", GameMode.PVP))
                .thenReturn(true);

        favoriteService.addFavorite(1L, GameMode.PVP, "item-a");

        verify(favoriteItemRepository, never()).save(any());
    }

    @Test
    void removeFavorite_delegatesToRepository() {
        favoriteService.removeFavorite(1L, GameMode.PVP, "item-a");

        verify(favoriteItemRepository).deleteByUserIdAndItemIdAndMode(1L, "item-a", GameMode.PVP);
    }

    @Test
    void getFavoriteItemIds_returnsOnlyItemIdsForGivenUserAndMode() {
        FavoriteItem fav = FavoriteItem.builder().itemId("item-a").mode(GameMode.PVP).build();
        when(favoriteItemRepository.findByUserIdAndMode(1L, GameMode.PVP)).thenReturn(List.of(fav));

        List<String> result = favoriteService.getFavoriteItemIds(1L, GameMode.PVP);

        assertThat(result).containsExactly("item-a");
    }
}