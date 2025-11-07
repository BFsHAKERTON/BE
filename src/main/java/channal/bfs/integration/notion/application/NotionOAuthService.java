package channal.bfs.integration.notion.application;

import channal.bfs.auth.infrastructure.AppUserEntity;
import channal.bfs.auth.infrastructure.JpaUserRepository;
import channal.bfs.common.domain.IntegrationType;
import channal.bfs.common.exception.AppException;
import channal.bfs.integration.infrastructure.IntegrationEntity;
import channal.bfs.integration.infrastructure.IntegrationJpaRepository;
import channal.bfs.integration.notion.domain.NotionToken;
import channal.bfs.integration.notion.domain.NotionTokenRepository;
import channal.bfs.integration.notion.dto.NotionOAuthTokenRequest;
import channal.bfs.integration.notion.dto.NotionOAuthTokenResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class NotionOAuthService {

    private final NotionTokenRepository notionTokenRepository;
    private final JpaUserRepository userRepository;
    private final IntegrationJpaRepository integrationRepository;
    private final RestTemplate restTemplate;

    @Value("${notion.oauth.client-id}")
    private String clientId;

    @Value("${notion.oauth.client-secret}")
    private String clientSecret;

    @Value("${notion.oauth.redirect-uri}")
    private String redirectUri;

    @Value("${notion.oauth.auth-url}")
    private String authUrl;

    @Value("${notion.oauth.token-url}")
    private String tokenUrl;

    /**
     * OAuth 인증 URL 생성
     */
    public String getAuthorizationUrl(UUID userId) {
        String state = URLEncoder.encode(userId.toString(), StandardCharsets.UTF_8);
        return String.format("%s?client_id=%s&response_type=code&owner=user&redirect_uri=%s&state=%s",
                authUrl, clientId, redirectUri, state);
    }

    /**
     * Authorization Code로 Access Token 교환
     */
    public NotionOAuthTokenResponse exchangeCodeForToken(String code) {
        log.info("Exchanging authorization code for access token");

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            // Basic Auth 헤더 생성
            String auth = clientId + ":" + clientSecret;
            String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes(StandardCharsets.UTF_8));
            headers.set("Authorization", "Basic " + encodedAuth);

            NotionOAuthTokenRequest request = NotionOAuthTokenRequest.builder()
                    .grantType("authorization_code")
                    .code(code)
                    .redirectUri(redirectUri)
                    .build();

            HttpEntity<NotionOAuthTokenRequest> entity = new HttpEntity<>(request, headers);

            ResponseEntity<NotionOAuthTokenResponse> response = restTemplate.exchange(
                    tokenUrl,
                    HttpMethod.POST,
                    entity,
                    NotionOAuthTokenResponse.class
            );

            log.info("Successfully exchanged code for access token");
            return response.getBody();
        } catch (Exception e) {
            log.error("Failed to exchange code for token", e);
            throw new RuntimeException("Failed to exchange authorization code for access token", e);
        }
    }

    /**
     * 사용자의 토큰 저장
     */
    public void saveToken(UUID userId, NotionOAuthTokenResponse tokenResponse) {
        log.info("Saving Notion token for user: {}", userId);
        AppUserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException("USER_NOT_FOUND", "사용자를 찾을 수 없습니다"+ userId));
        NotionToken token = notionTokenRepository.findByUser_Id(userId)
                .orElse(NotionToken.builder()
                        .user(user)
                        .build());

        token.updateToken(
                tokenResponse.getAccessToken(),
                tokenResponse.getWorkspaceId(),
                tokenResponse.getWorkspaceName(),
                tokenResponse.getBotId()
        );

        NotionToken savedToken = notionTokenRepository.save(token);
        log.info("Notion token saved successfully for user: {}", userId);

        IntegrationEntity integration = integrationRepository
                .findByUserIdAndType(userId, IntegrationType.NOTION)
                .orElse(new IntegrationEntity());

        integration.setUser(user);
        integration.setType(IntegrationType.NOTION);
        integration.setNotionToken(savedToken);

        integrationRepository.save(integration);
        log.info("Integration entity updated for Notion user: {}", userId);
    }

    /**
     * 사용자의 토큰 조회
     */
    public String getAccessToken(UUID userId) {
        return notionTokenRepository.findByUser_Id(userId)
                .map(NotionToken::getAccessToken)
                .orElseThrow(() -> new RuntimeException("Notion token not found for user: " + userId));
    }

    /**
     * 사용자의 토큰 존재 여부 확인
     */
    public boolean hasToken(UUID userId) {
        return notionTokenRepository.existsByUser_Id(userId);
    }
}
