package channal.bfs.tag.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TagJpaRepository extends JpaRepository<TagEntity, UUID> {
    Optional<TagEntity> findByName(String name);
    List<TagEntity> findByIdIn(List<UUID> ids);
}

