package channal.bfs.integration.notion.domain;

import channal.bfs.auth.infrastructure.AppUserEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "notion_tokens")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotionToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", referencedColumnName = "id", nullable = false, unique = true)
    private AppUserEntity user;

    @Column(nullable = false, length = 500)
    private String accessToken;

    @Column(length = 100)
    private String workspaceId;

    @Column(length = 200)
    private String workspaceName;

    @Column(length = 100)
    private String botId;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public void updateToken(String accessToken, String workspaceId, String workspaceName, String botId) {
        this.accessToken = accessToken;
        this.workspaceId = workspaceId;
        this.workspaceName = workspaceName;
        this.botId = botId;
    }
}
