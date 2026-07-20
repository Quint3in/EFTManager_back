package cat.itacademy.s05.t02.eftmanager.entity;

import cat.itacademy.s05.t02.eftmanager.service.GameMode;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "hideout_progress",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "station_id", "mode"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HideoutProgress {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "station_id", nullable = false, length = 50)
    private String stationId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private GameMode mode;

    @Column(nullable = false)
    private int level;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    @PreUpdate
    protected void onSave() {
        this.updatedAt = LocalDateTime.now();
    }
}