package cat.itacademy.s05.t02.eftmanager.repository;

import cat.itacademy.s05.t02.eftmanager.entity.HideoutProgress;
import cat.itacademy.s05.t02.eftmanager.service.GameMode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface HideoutProgressRepository extends JpaRepository<HideoutProgress, Long> {

    List<HideoutProgress> findByUserIdAndMode(Long userId, GameMode mode);

    Optional<HideoutProgress> findByUserIdAndStationIdAndMode(
            Long userId, String stationId, GameMode mode);
}