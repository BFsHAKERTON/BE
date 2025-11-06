package channal.bfs.auth.application;

import channal.bfs.auth.domain.UserRepository;
import org.springframework.stereotype.Service;

@Service
public class AuthQueryService {
    private final UserRepository userRepository;

    public AuthQueryService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }
}


