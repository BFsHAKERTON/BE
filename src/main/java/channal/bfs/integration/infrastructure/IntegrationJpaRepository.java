package channal.bfs.integration.infrastructure;

import channal.bfs.common.domain.IntegrationType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface IntegrationJpaRepository extends JpaRepository<IntegrationEntity, UUID> {
    Optional<IntegrationEntity> findByUserIdAndType(UUID userId, IntegrationType type);
}

