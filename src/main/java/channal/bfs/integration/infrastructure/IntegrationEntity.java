package channal.bfs.integration.infrastructure;

import channal.bfs.auth.infrastructure.AppUserEntity;
import channal.bfs.common.domain.IntegrationType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "integration", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"user_id", "type"})
})
@Getter
@Setter
@NoArgsConstructor
public class IntegrationEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUserEntity user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private IntegrationType type;

    // 노션용
    @Column(name = "notion_access_token", columnDefinition = "TEXT")
    private String notionAccessToken;

    @Column(name = "notion_workspace_name")
    private String notionWorkspaceName;

    @Column(name = "notion_workspace_url")
    private String notionWorkspaceUrl;

    // 채널톡용
    @Column(name = "channel_talk_api_key", columnDefinition = "TEXT")
    private String channelTalkApiKey;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        OffsetDateTime now = OffsetDateTime.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = OffsetDateTime.now();
    }
}

