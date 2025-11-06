package channal.bfs.auth.infrastructure;

import channal.bfs.auth.domain.UserRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public class JpaUserRepository implements UserRepository {
    @Override
    public Optional<Object> findById(UUID userId) {
        return Optional.empty();
    }
}


