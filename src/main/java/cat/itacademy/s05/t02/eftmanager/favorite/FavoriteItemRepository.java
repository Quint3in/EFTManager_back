package cat.itacademy.s05.t02.eftmanager.favorite;

import cat.itacademy.s05.t02.eftmanager.common.GameMode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FavoriteItemRepository extends JpaRepository<FavoriteItem, Long> {
    List<FavoriteItem> findByUserIdAndMode(Long userId, GameMode mode);
    boolean existsByUserIdAndItemIdAndMode(Long userId, String itemId, GameMode mode);
    void deleteByUserIdAndItemIdAndMode(Long userId, String itemId, GameMode mode);
}