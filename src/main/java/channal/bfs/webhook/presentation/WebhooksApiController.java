package channal.bfs.webhook.presentation;

import channal.bfs.api.WebhooksApi;
import channal.bfs.model.WebhooksChannelTalkPost200Response;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
public class WebhooksApiController implements WebhooksApi {
    private final ObjectMapper objectMapper;

    public WebhooksApiController(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

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
        } catch (JsonProcessingException e) {
            log.warn("Failed to serialize webhook payload", e);
        }

        WebhooksChannelTalkPost200Response response = new WebhooksChannelTalkPost200Response();
        response.setSuccess(true);
        return ResponseEntity.ok(response);
    }
}


