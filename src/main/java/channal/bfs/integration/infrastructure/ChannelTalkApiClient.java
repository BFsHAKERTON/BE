package channal.bfs.integration.infrastructure;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * 채널톡 API 클라이언트
 * API Key 유효성 검사 및 채널톡 API 호출 담당
 */
@Component
public class ChannelTalkApiClient {

    private static final String CHANNEL_TALK_API_BASE_URL = "https://api.channel.io";
    // 채널톡 API Key 검증용 엔드포인트
    // 채널 정보 조회 API를 사용하여 검증합니다.
    private static final String VALIDATION_ENDPOINT = "/open/v5/channel";
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public ChannelTalkApiClient() {
        this.objectMapper = new ObjectMapper();
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    /**
     * 채널톡 API Key 유효성 검사
     * x-access-key와 x-access-secret 헤더를 사용하여 인증합니다.
     * 
     * @param apiKey 채널톡 Access Key
     * @param apiSecret 채널톡 Access Secret
     * @return 유효하면 true, 유효하지 않으면 false
     */
    public boolean validateApiKey(String apiKey, String apiSecret) {
        if (apiKey == null || apiKey.isBlank() || apiSecret == null || apiSecret.isBlank()) {
            return false;
        }

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(CHANNEL_TALK_API_BASE_URL + VALIDATION_ENDPOINT))
                    .header("accept", "application/json")
                    .header("x-access-key", apiKey)
                    .header("x-access-secret", apiSecret)
                    .GET()
                    .timeout(Duration.ofSeconds(10))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            // 디버깅을 위한 로그 출력
            if (response.statusCode() != 200) {
                System.err.println("채널톡 API 응답 상태 코드: " + response.statusCode());
                System.err.println("채널톡 API 응답 본문: " + response.body());
            } else {
                System.err.println("✅ 채널톡 API 인증 성공!");
            }

            // 200 OK면 유효한 API Key
            return response.statusCode() == 200;
        } catch (Exception e) {
            // 네트워크 오류나 기타 예외 발생 시 유효하지 않은 것으로 간주
            System.err.println("채널톡 API 호출 중 오류 발생: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
}

