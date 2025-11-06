package channal.bfs.auth.domain;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository {
    Optional<Object> findById(UUID userId);
}


