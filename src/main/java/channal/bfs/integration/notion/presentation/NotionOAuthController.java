package channal.bfs.integration.notion.presentation;

import channal.bfs.integration.notion.application.NotionOAuthService;
import channal.bfs.integration.notion.dto.NotionOAuthTokenResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/notion/oauth")
@RequiredArgsConstructor
@Slf4j
public class NotionOAuthController {

    private final NotionOAuthService notionOAuthService;

    /**
     * Notion OAuth 로그인 시작
     * GET /api/notion/oauth/login
     */
    @GetMapping("/login")
    public ResponseEntity<Map<String, String>> login() {
        log.info("Notion OAuth login requested");
        String authUrl = notionOAuthService.getAuthorizationUrl();

        Map<String, String> response = new HashMap<>();
        response.put("authorizationUrl", authUrl);
        response.put("message", "Redirect user to this URL to authorize");

        return ResponseEntity.ok(response);
    }

    /**
     * Notion OAuth Callback
     * GET /api/notion/oauth/callback?code=xxx
     */
    @GetMapping("/callback")
    public ResponseEntity<Map<String, Object>> callback(
            @RequestParam("code") String code,
            @RequestParam(value = "userId") UUID userId
    ) {
        log.info("Notion OAuth callback received with code");

        try {
            // Authorization code를 Access Token으로 교환
            NotionOAuthTokenResponse tokenResponse = notionOAuthService.exchangeCodeForToken(code);

            // 사용자의 토큰 저장
            notionOAuthService.saveToken(userId, tokenResponse);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Notion authentication successful");
            response.put("workspaceName", tokenResponse.getWorkspaceName());
            response.put("workspaceId", tokenResponse.getWorkspaceId());

            log.info("Notion OAuth authentication successful for user: {}", userId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Notion OAuth authentication failed", e);

            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Authentication failed: " + e.getMessage());

            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * 사용자의 Notion 연동 상태 확인
     * GET /api/notion/oauth/status?userId=1
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> status(
            @RequestParam(value = "userId") UUID userId
    ) {
        boolean hasToken = notionOAuthService.hasToken(userId);

        Map<String, Object> response = new HashMap<>();
        response.put("connected", hasToken);
        response.put("userId", userId);

        return ResponseEntity.ok(response);
    }
}
