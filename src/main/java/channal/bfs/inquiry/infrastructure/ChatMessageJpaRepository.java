package channal.bfs.inquiry.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ChatMessageJpaRepository extends JpaRepository<ChatMessageEntity, UUID> {
    List<ChatMessageEntity> findByInquiryIdOrderByTimestampAsc(UUID inquiryId);
}

