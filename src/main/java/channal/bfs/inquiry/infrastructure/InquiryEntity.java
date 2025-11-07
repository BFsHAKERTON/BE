package channal.bfs.inquiry.infrastructure;

import channal.bfs.auth.infrastructure.AppUserEntity;
import channal.bfs.common.domain.InquiryStatus;
import channal.bfs.tag.infrastructure.DepartmentEntity;
import channal.bfs.tag.infrastructure.TagEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "inquiry")
@Getter
@Setter
@NoArgsConstructor
public class InquiryEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private InquiryStatus status = InquiryStatus.NEW;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requester_id")
    private AppUserEntity requester;

    @Column(columnDefinition = "TEXT")
    private String summary;

    @Column(name = "key_feedback", columnDefinition = "TEXT")
    private String keyFeedback;

    @Column(name = "channel_talk_url")
    private String channelTalkUrl;

    @Column(name = "customer_city")
    private String customerCity;

    @Column(name = "notion_page_url")
    private String notionPageUrl;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @ManyToMany
    @JoinTable(
        name = "inquiry_tags",
        joinColumns = @JoinColumn(name = "inquiry_id"),
        inverseJoinColumns = @JoinColumn(name = "tag_id")
    )
    private Set<TagEntity> tags = new HashSet<>();

    @ManyToMany
    @JoinTable(
        name = "inquiry_departments",
        joinColumns = @JoinColumn(name = "inquiry_id"),
        inverseJoinColumns = @JoinColumn(name = "department_id")
    )
    private Set<DepartmentEntity> departments = new HashSet<>();

    @ManyToMany
    @JoinTable(
        name = "inquiry_assignees",
        joinColumns = @JoinColumn(name = "inquiry_id"),
        inverseJoinColumns = @JoinColumn(name = "user_id")
    )
    private Set<AppUserEntity> assignees = new HashSet<>();

    @OneToMany(mappedBy = "inquiry", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<ChatMessageEntity> chatMessages = new HashSet<>();

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

