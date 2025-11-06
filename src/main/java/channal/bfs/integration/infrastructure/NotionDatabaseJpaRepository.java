package channal.bfs.integration.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface NotionDatabaseJpaRepository extends JpaRepository<NotionDatabaseEntity, UUID> {
    List<NotionDatabaseEntity> findByUserId(UUID userId);
    Optional<NotionDatabaseEntity> findByNotionDatabaseId(String notionDatabaseId);
}

