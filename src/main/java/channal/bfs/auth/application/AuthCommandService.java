package channal.bfs.auth.application;

import channal.bfs.auth.infrastructure.AppUserEntity;
import channal.bfs.auth.infrastructure.JpaUserRepository;
import channal.bfs.auth.infrastructure.JwtTokenService;
import channal.bfs.auth.infrastructure.KakaoOAuthClient;
import channal.bfs.common.exception.AppException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
public class AuthCommandService {

    private final JpaUserRepository userRepository;
    private final JwtTokenService jwtTokenService;
    private final KakaoOAuthClient kakaoOAuthClient;

    public AuthCommandService(
            JpaUserRepository userRepository,
            JwtTokenService jwtTokenService,
            KakaoOAuthClient kakaoOAuthClient) {
        this.userRepository = userRepository;
        this.jwtTokenService = jwtTokenService;
        this.kakaoOAuthClient = kakaoOAuthClient;
    }

    public TokenPair handleKakaoCallback(String code) {
        if (code == null || code.isBlank()) {
            throw new AppException("INVALID_KAKAO_CODE", "유효하지 않은 카카오 인증 코드입니다.");
        }

        try {
            // 1. 카카오 액세스 토큰 획득
            KakaoOAuthClient.KakaoTokenResponse tokenResponse = kakaoOAuthClient.getAccessToken(code);
            if (tokenResponse == null || tokenResponse.getAccessToken() == null) {
                throw new AppException("INVALID_KAKAO_CODE", "카카오 토큰 획득에 실패했습니다.");
            }

            // 2. 카카오 사용자 정보 조회
            KakaoOAuthClient.KakaoUserInfo userInfo = kakaoOAuthClient.getUserInfo(tokenResponse.getAccessToken());
            if (userInfo == null || userInfo.getId() == null) {
                throw new AppException("INVALID_KAKAO_CODE", "카카오 사용자 정보 조회에 실패했습니다.");
            }

            // 3. DB에서 사용자 조회 또는 생성
            String kakaoId = userInfo.getId().toString();
            Optional<AppUserEntity> existingUser = userRepository.findByKakaoId(kakaoId);

            AppUserEntity user;
            if (existingUser.isPresent()) {
                user = existingUser.get();
            } else {
                // 신규 사용자 생성
                user = new AppUserEntity();
                user.setKakaoId(kakaoId);
                
                if (userInfo.getKakaoAccount() != null) {
                    if (userInfo.getKakaoAccount().getEmail() != null) {
                        user.setEmail(userInfo.getKakaoAccount().getEmail());
                    }
                    if (userInfo.getKakaoAccount().getProfile() != null &&
                            userInfo.getKakaoAccount().getProfile().getNickname() != null) {
                        user.setName(userInfo.getKakaoAccount().getProfile().getNickname());
                    } else {
                        user.setName("카카오 사용자");
                    }
                } else {
                    user.setName("카카오 사용자");
                }
                
                user = userRepository.save(user);
            }

            // 4. JWT 토큰 발급
            String accessToken = jwtTokenService.generateAccessToken(user.getId());
            String refreshToken = jwtTokenService.generateRefreshToken(user.getId());

            return new TokenPair(accessToken, refreshToken);

        } catch (AppException e) {
            throw e;
        } catch (Exception e) {
            throw new AppException("KAKAO_LOGIN_FAILED", "카카오 로그인 처리 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    public record TokenPair(String accessToken, String refreshToken) {}
}

