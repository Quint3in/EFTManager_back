package cat.itacademy.s05.t02.eftmanager.favorite;

import cat.itacademy.s05.t02.eftmanager.common.GameMode;
import cat.itacademy.s05.t02.eftmanager.user.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class FavoriteService {

    private final FavoriteItemRepository favoriteItemRepository;
    private final UserRepository userRepository;

    public FavoriteService(FavoriteItemRepository favoriteItemRepository, UserRepository userRepository) {
        this.favoriteItemRepository = favoriteItemRepository;
        this.userRepository = userRepository;
    }

    public List<String> getFavoriteItemIds(Long userId, GameMode mode) {
        return favoriteItemRepository.findByUserIdAndMode(userId, mode).stream()
                .map(FavoriteItem::getItemId)
                .toList();
    }

    @Transactional
    public void addFavorite(Long userId, GameMode mode, String itemId) {
        if (favoriteItemRepository.existsByUserIdAndItemIdAndMode(userId, itemId, mode)) return;
        FavoriteItem favorite = FavoriteItem.builder()
                .user(userRepository.getReferenceById(userId))
                .itemId(itemId)
                .mode(mode)
                .build();
        favoriteItemRepository.save(favorite);
    }

    @Transactional
    public void removeFavorite(Long userId, GameMode mode, String itemId) {
        favoriteItemRepository.deleteByUserIdAndItemIdAndMode(userId, itemId, mode);
    }
}