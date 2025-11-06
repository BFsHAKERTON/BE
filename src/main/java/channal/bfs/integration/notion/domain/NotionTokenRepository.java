package channal.bfs.integration.notion.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface NotionTokenRepository extends JpaRepository<NotionToken, Long> {
    Optional<NotionToken> findByUserId(Long userId);
    boolean existsByUserId(Long userId);
}
