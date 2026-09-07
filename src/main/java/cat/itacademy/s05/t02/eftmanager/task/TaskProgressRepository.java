package cat.itacademy.s05.t02.eftmanager.task;

import cat.itacademy.s05.t02.eftmanager.common.GameMode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TaskProgressRepository extends JpaRepository<TaskProgress, Long> {
    List<TaskProgress> findByUserIdAndMode(Long userId, GameMode mode);
    Optional<TaskProgress> findByUserIdAndTaskIdAndMode(Long userId, String taskId, GameMode mode);
}