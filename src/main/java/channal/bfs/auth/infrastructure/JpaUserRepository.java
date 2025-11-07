package channal.bfs.auth.infrastructure;

import channal.bfs.auth.domain.UserRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public class JpaUserRepository implements UserRepository {
    
    private final AppUserJpaRepository jpaRepository;
    
    public JpaUserRepository(AppUserJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }
    
    @Override
    public Optional<AppUserEntity> findById(UUID userId) {
        return jpaRepository.findById(userId);
    }
    
    public Optional<AppUserEntity> findByEmail(String email) {
        return jpaRepository.findByEmail(email);
    }
    
    public Optional<AppUserEntity> findByKakaoId(String kakaoId) {
        return jpaRepository.findByKakaoId(kakaoId);
    }
    
    public AppUserEntity save(AppUserEntity user) {
        return jpaRepository.save(user);
    }
}


