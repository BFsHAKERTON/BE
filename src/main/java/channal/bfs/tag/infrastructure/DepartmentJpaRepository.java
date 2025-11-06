package channal.bfs.tag.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DepartmentJpaRepository extends JpaRepository<DepartmentEntity, UUID> {
    Optional<DepartmentEntity> findByName(String name);
    List<DepartmentEntity> findByIdIn(List<UUID> ids);
}

