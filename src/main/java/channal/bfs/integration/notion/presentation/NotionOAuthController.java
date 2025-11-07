package channal.bfs.integration.notion.presentation;

import channal.bfs.integration.notion.application.NotionOAuthService;
import channal.bfs.integration.notion.dto.NotionOAuthTokenResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;
import java.util.HashMap;
import java.util.Map;

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
    public ResponseEntity<Map<String, String>> login(@RequestParam("userId") UUID userId) {
        log.info("Notion OAuth login requested for user: {}", userId);
        String authUrl = notionOAuthService.getAuthorizationUrl(userId);

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
    public ResponseEntity<?> callback(
            @RequestParam("code") String code,
            @RequestParam("state") String state,
            @RequestParam(value = "error", required = false) String error
    ) {
        log.info("Notion OAuth callback received with code for user (from state): {}", state);
        if (error != null) {
            log.error("Notion OAuth error: {}", error);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Authentication failed: " + error);
            return ResponseEntity.status(400).body(response);
        }
        UUID userId;
        try {
            userId = UUID.fromString(state); //
            log.info("Successfully parsed state to UUID: {}", userId);
        } catch (IllegalArgumentException e) { //
            log.error("Failed to parse state parameter to UUID. State: {}", state, e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Invalid state parameter.");
            return ResponseEntity.status(400).body(response);
        }

        try {
            // Authorization code를 Access Token으로 교환
            NotionOAuthTokenResponse tokenResponse = notionOAuthService.exchangeCodeForToken(code);

            // 사용자의 토큰 저장
            notionOAuthService.saveToken(userId, tokenResponse);

            log.info("Notion OAuth authentication successful for user: {}", userId);

            String html = "<!DOCTYPE html><html><head><meta charset=\"UTF-8\"></head><body>연동에 성공했습니다. 창을 닫아주세요.</body></html>";
            return ResponseEntity.ok()
                    .contentType(MediaType.valueOf("text/html; charset=UTF-8"))
                    .body(html);
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
     * GET /api/notion/oauth/status
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> status(
            @RequestAttribute("userId") UUID userId
    ) {
        boolean hasToken = notionOAuthService.hasToken(userId);

        Map<String, Object> response = new HashMap<>();
        response.put("connected", hasToken);
        response.put("userId", userId);

        return ResponseEntity.ok(response);
    }
}
