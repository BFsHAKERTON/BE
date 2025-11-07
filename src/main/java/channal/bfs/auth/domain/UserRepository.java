package channal.bfs.auth.domain;

import channal.bfs.auth.infrastructure.AppUserEntity;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository {
    Optional<AppUserEntity> findById(UUID userId);
}


