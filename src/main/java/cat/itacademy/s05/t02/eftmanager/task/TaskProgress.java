package cat.itacademy.s05.t02.eftmanager.task;

import cat.itacademy.s05.t02.eftmanager.common.GameMode;
import cat.itacademy.s05.t02.eftmanager.user.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "task_progress", uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "task_id", "mode"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class TaskProgress {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "task_id", nullable = false, length = 50)
    private String taskId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private GameMode mode;

    @Column(nullable = false)
    private boolean completed;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;
}