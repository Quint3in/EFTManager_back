package cat.itacademy.s05.t02.eftmanager.favorite;

import cat.itacademy.s05.t02.eftmanager.common.GameMode;
import cat.itacademy.s05.t02.eftmanager.user.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "favorite_items", uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "item_id", "mode"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class FavoriteItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "item_id", nullable = false, length = 50)
    private String itemId;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private GameMode mode;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}