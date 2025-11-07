package channal.bfs.integration.channaltalk.presentation;

import channal.bfs.api.IntegrationsApi;
import channal.bfs.common.exception.AppException;
import channal.bfs.integration.application.IntegrationCommandService;
import channal.bfs.model.ChannelTalkRequest;
import channal.bfs.model.IntegrationsChannelTalkPost200Response;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.UUID;

@RestController
public class IntegrationsApiController implements IntegrationsApi {

    private final IntegrationCommandService integrationCommandService;

    public IntegrationsApiController(IntegrationCommandService integrationCommandService) {
        this.integrationCommandService = integrationCommandService;
    }

    @Override
    public ResponseEntity<IntegrationsChannelTalkPost200Response> integrationsChannelTalkPost(
            ChannelTalkRequest channelTalkRequest) {
        try {
            // 사용자 ID 가져오기 (JWT에서)
            UUID userId = getUserIdFromRequest();
            if (userId == null) {
                throw new AppException("UNAUTHORIZED", "인증이 필요합니다.");
            }

            // API Key 등록
            integrationCommandService.registerChannelTalkApiKey(
                userId, 
                channelTalkRequest.getApiKey(),
                channelTalkRequest.getApiSecret()
            );

            // 응답 생성
            IntegrationsChannelTalkPost200Response response = new IntegrationsChannelTalkPost200Response();
            response.setSuccess(true);
            response.setMessage("채널톡 연동에 성공했습니다.");

            return ResponseEntity.ok(response);

        } catch (AppException e) {
            throw e;
        } catch (Exception e) {
            throw new AppException("CHANNEL_TALK_REGISTRATION_FAILED", 
                    "채널톡 연동에 실패했습니다: " + e.getMessage());
        }
    }

    /**
     * JWT에서 사용자 ID 추출
     */
    private UUID getUserIdFromRequest() {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder
                    .currentRequestAttributes();
            HttpServletRequest request = attributes.getRequest();
            
            Object userIdObj = request.getAttribute("userId");
            if (userIdObj instanceof UUID) {
                return (UUID) userIdObj;
            }
            
            return null;
        } catch (Exception e) {
            return null;
        }
    }
}

