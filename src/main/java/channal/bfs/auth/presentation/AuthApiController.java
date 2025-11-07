package channal.bfs.auth.presentation;

import channal.bfs.api.AuthApi;
import channal.bfs.auth.application.AuthCommandService;
import channal.bfs.auth.application.AuthQueryService;
import channal.bfs.auth.infrastructure.AppUserEntity;
import channal.bfs.auth.infrastructure.JwtTokenService;
import channal.bfs.auth.infrastructure.KakaoOAuthClient;
import channal.bfs.common.exception.AppException;
import channal.bfs.model.AuthTokens;
import channal.bfs.model.ErrorResponse;
import channal.bfs.model.User;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.net.URI;
import java.util.UUID;

@RestController
public class AuthApiController implements AuthApi {

    private final AuthCommandService authCommandService;
    private final AuthQueryService authQueryService;
    private final KakaoOAuthClient kakaoOAuthClient;
    private final JwtTokenService jwtTokenService;
    private final String frontendRedirectUri;

    public AuthApiController(
            AuthCommandService authCommandService,
            AuthQueryService authQueryService,
            KakaoOAuthClient kakaoOAuthClient,
            JwtTokenService jwtTokenService,
            @Value("${kakao.oauth.frontend-redirect-uri}") String frontendRedirectUri) {
        this.authCommandService = authCommandService;
        this.authQueryService = authQueryService;
        this.kakaoOAuthClient = kakaoOAuthClient;
        this.jwtTokenService = jwtTokenService;
        this.frontendRedirectUri = frontendRedirectUri;
    }

    // 인터페이스 구현 (YAML에 맞춰 redirectUri 파라미터 추가됨)
    @Override
    public ResponseEntity<Void> authKakaoGet(URI redirectUri) {
        // redirectUri를 state로 인코딩하여 카카오에 전달
        // 콜백에서 state를 읽어서 원래 URL로 리다이렉트
        String state = redirectUri != null ? java.net.URLEncoder.encode(redirectUri.toString(), java.nio.charset.StandardCharsets.UTF_8) : null;
        String authUrl = kakaoOAuthClient.getAuthorizationUrl(state);
        return ResponseEntity.status(HttpStatus.FOUND)
                .header(HttpHeaders.LOCATION, authUrl)
                .build();
    }

    // 인터페이스 구현 (YAML에 맞춰 HTML 반환 및 state 파라미터 추가됨)
    @Override
    public ResponseEntity<String> authKakaoCallbackGet(String code, String error, String state) {
        try {
            // state에서 원래 리다이렉트 URL 추출 (없으면 기본값 사용)
            String targetRedirectUri = frontendRedirectUri;
            if (state != null && !state.isBlank()) {
                try {
                    targetRedirectUri = java.net.URLDecoder.decode(state, java.nio.charset.StandardCharsets.UTF_8);
                } catch (Exception e) {
                    // 디코딩 실패 시 기본값 사용
                }
            }
            
            if (error != null) {
                // 에러 시 원래 URL로 리디렉션 (에러 정보 포함)
                String errorRedirectUrl = targetRedirectUri + (targetRedirectUri.contains("?") ? "&" : "?") + "error=" + error;
                String html = generateRedirectHtml(errorRedirectUrl, null, "카카오 인증이 취소되었습니다: " + error);
                return ResponseEntity.ok()
                        .contentType(MediaType.TEXT_HTML)
                        .body(html);
            }

            AuthCommandService.TokenPair tokenPair = authCommandService.handleKakaoCallback(code);
            
            // 성공 시 원래 URL로 리디렉션 (토큰을 HTML에 포함)
            String html = generateRedirectHtml(targetRedirectUri, tokenPair, null);
            
            return ResponseEntity.ok()
                    .contentType(MediaType.TEXT_HTML)
                    .body(html);
            
        } catch (AppException e) {
            // 에러 시 원래 URL로 리디렉션
            String targetRedirectUri = frontendRedirectUri;
            if (state != null && !state.isBlank()) {
                try {
                    targetRedirectUri = java.net.URLDecoder.decode(state, java.nio.charset.StandardCharsets.UTF_8);
                } catch (Exception ex) {
                    // 디코딩 실패 시 기본값 사용
                }
            }
            String errorRedirectUrl = targetRedirectUri + (targetRedirectUri.contains("?") ? "&" : "?") + "error=" + e.getErrorCode() + "&message=" + java.net.URLEncoder.encode(e.getMessage(), java.nio.charset.StandardCharsets.UTF_8);
            String html = generateRedirectHtml(errorRedirectUrl, null, e.getMessage());
            return ResponseEntity.ok()
                    .contentType(MediaType.TEXT_HTML)
                    .body(html);
        } catch (Exception e) {
            // 에러 시 원래 URL로 리디렉션
            String targetRedirectUri = frontendRedirectUri;
            if (state != null && !state.isBlank()) {
                try {
                    targetRedirectUri = java.net.URLDecoder.decode(state, java.nio.charset.StandardCharsets.UTF_8);
                } catch (Exception ex) {
                    // 디코딩 실패 시 기본값 사용
                }
            }
            String errorRedirectUrl = targetRedirectUri + (targetRedirectUri.contains("?") ? "&" : "?") + "error=INTERNAL_ERROR&message=" + java.net.URLEncoder.encode("서버 오류가 발생했습니다.", java.nio.charset.StandardCharsets.UTF_8);
            String html = generateRedirectHtml(errorRedirectUrl, null, "서버 오류가 발생했습니다.");
            return ResponseEntity.ok()
                    .contentType(MediaType.TEXT_HTML)
                    .body(html);
        }
    }

    private String generateRedirectHtml(String redirectUrl, AuthCommandService.TokenPair tokenPair, String errorMessage) {
        if (tokenPair != null) {
            // 성공: 토큰을 localStorage에 저장하고 리디렉션
            return String.format("""
                <!DOCTYPE html>
                <html>
                <head>
                    <title>로그인 처리 중...</title>
                </head>
                <body>
                    <script>
                        // 토큰을 localStorage에 저장
                        localStorage.setItem('accessToken', '%s');
                        localStorage.setItem('refreshToken', '%s');
                        
                        // 원래 페이지로 리디렉션
                        window.location.href = '%s';
                    </script>
                    <p>로그인 처리 중...</p>
                </body>
                </html>
                """, tokenPair.accessToken(), tokenPair.refreshToken(), redirectUrl);
        } else {
            // 에러: 에러 메시지와 함께 리디렉션
            return String.format("""
                <!DOCTYPE html>
                <html>
                <head>
                    <title>로그인 실패</title>
                </head>
                <body>
                    <script>
                        // 에러 정보를 쿼리 파라미터로 전달
                        window.location.href = '%s';
                    </script>
                    <p>로그인 처리 중...</p>
                </body>
                </html>
                """, redirectUrl);
        }
    }

    @Override
    public ResponseEntity<User> authMeGet() {
        try {
            UUID userId = getUserIdFromRequest();
            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }

            AppUserEntity userEntity = authQueryService.getCurrentUser(userId);
            
            User user = new User();
            user.setUserId(userEntity.getId());
            user.setEmail(userEntity.getEmail());
            user.setName(userEntity.getName());
            
            return ResponseEntity.ok(user);
            
        } catch (AppException e) {
            if ("USER_NOT_FOUND".equals(e.getErrorCode())) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
            }
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }

    private UUID getUserIdFromRequest() {
        try {
            // JWT 필터에서 설정한 userId 가져오기
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder
                    .currentRequestAttributes();
            HttpServletRequest request = attributes.getRequest();
            
            Object userIdObj = request.getAttribute("userId");
            if (userIdObj instanceof UUID) {
                return (UUID) userIdObj;
            }
            
            // Authorization 헤더에서 직접 추출 (fallback)
            String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7);
                if (jwtTokenService.validateToken(token)) {
                    return jwtTokenService.getUserIdFromToken(token);
                }
            }
            
            return null;
        } catch (Exception e) {
            return null;
        }
    }
}


