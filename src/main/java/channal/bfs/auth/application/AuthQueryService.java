package channal.bfs.auth.application;

import channal.bfs.auth.domain.UserRepository;
import channal.bfs.auth.infrastructure.AppUserEntity;
import channal.bfs.common.exception.AppException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class AuthQueryService {
    private final UserRepository userRepository;

    public AuthQueryService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public AppUserEntity getCurrentUser(UUID userId) {
        Optional<AppUserEntity> user = userRepository.findById(userId);
        if (user.isEmpty()) {
            throw new AppException("USER_NOT_FOUND", "사용자를 찾을 수 없습니다.");
        }
        return user.get();
    }
}


