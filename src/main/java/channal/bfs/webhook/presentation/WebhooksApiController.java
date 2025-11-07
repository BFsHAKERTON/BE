package channal.bfs.webhook.presentation;

import channal.bfs.api.WebhooksApi;
import channal.bfs.model.WebhooksChannelTalkPost200Response;
import channal.bfs.webhook.application.ChatWebhookService;
import channal.bfs.webhook.domain.ChannelTalkWebhookEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
public class WebhooksApiController implements WebhooksApi {
    private final ObjectMapper objectMapper;
    private final ChatWebhookService chatWebhookService;

    @Override
    public ResponseEntity<WebhooksChannelTalkPost200Response> _webhooksChannelTalkPost(
            @RequestBody Object body,
            @RequestHeader(value = "X-Channel-Signature", required = false) String xChannelSignature) {
        return webhooksChannelTalkPost(body, xChannelSignature);
    }

    @Override
    public ResponseEntity<WebhooksChannelTalkPost200Response> webhooksChannelTalkPost(Object body, String xChannelSignature) {
        try {
            String payload = objectMapper.writeValueAsString(body);
            log.info("Received ChannelTalk webhook: signature={}, payload={}", xChannelSignature, payload);

            // JSON을 ChannelTalkWebhookEvent로 변환
            ChannelTalkWebhookEvent event = objectMapper.readValue(payload, ChannelTalkWebhookEvent.class);

            // 비동기로 웹훅 처리
            chatWebhookService.processWebhook(event);

            log.info("웹훅 처리 시작됨 (비동기)");
        } catch (JsonProcessingException e) {
            log.error("Failed to parse webhook payload", e);
            // 파싱 실패해도 채널톡에는 성공 응답 반환 (재시도 방지)
        } catch (Exception e) {
            log.error("Unexpected error processing webhook", e);
        }

        // 즉시 성공 응답 (채널톡 웹훅은 빠른 응답 필요)
        WebhooksChannelTalkPost200Response response = new WebhooksChannelTalkPost200Response();
        response.setSuccess(true);
        return ResponseEntity.ok(response);
    }
}


