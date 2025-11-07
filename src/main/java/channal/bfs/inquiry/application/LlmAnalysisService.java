package channal.bfs.inquiry.application;

import channal.bfs.model.ChatMessage;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * OpenAI API를 사용하여 채팅 메시지를 분석하는 서비스
 * - 부서 분류 (QA팀, 마케팅, 개발팀, 운영팀, 기획팀)
 * - 요약 생성
 * - 주요 피드백 추출
 */
@Service
public class LlmAnalysisService {

    private final String openaiApiKey;
    private final String openaiApiUrl;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public LlmAnalysisService(
            @Value("${openai.api-key:}") String openaiApiKey,
            @Value("${openai.api-url:https://api.openai.com/v1/chat/completions}") String openaiApiUrl) {
        this.openaiApiKey = openaiApiKey;
        this.openaiApiUrl = openaiApiUrl;
        this.objectMapper = new ObjectMapper();
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(30))
                .build();
    }

    /**
     * 채팅 메시지를 분석하여 부서, 요약, 주요 피드백을 추출
     * 
     * @param chatMessages 채팅 메시지 목록
     * @return 분석 결과 (부서, 요약, 주요 피드백)
     */
    public AnalysisResult analyzeChatMessages(List<ChatMessage> chatMessages) {
        if (chatMessages == null || chatMessages.isEmpty()) {
            return AnalysisResult.empty();
        }

        if (openaiApiKey == null || openaiApiKey.isBlank()) {
            // API 키가 없으면 빈 결과 반환
            return AnalysisResult.empty();
        }

        try {
            String prompt = buildPrompt(chatMessages);
            String response = callOpenAI(prompt);
            return parseResponse(response);
        } catch (Exception e) {
            // LLM 실패 시 빈 값으로 저장
            return AnalysisResult.empty();
        }
    }

    /**
     * 프롬프트 생성
     */
    private String buildPrompt(List<ChatMessage> chatMessages) {
        StringBuilder promptBuilder = new StringBuilder();
        promptBuilder.append("다음은 고객 상담 채팅 내용입니다. 다음 3가지 항목을 JSON 형식으로 응답해주세요:\n\n");
        promptBuilder.append("1. department: 이 문의가 어느 부서와 관련된 것인지 판단하세요. (QA팀, 마케팅, 개발팀, 운영팀, 기획팀 중 하나 이상, 배열 형식)\n");
        promptBuilder.append("   - 관련된 부서가 하나면 배열에 하나만 포함하세요.\n");
        promptBuilder.append("   - 여러 부서와 관련되면 모두 포함하세요.\n\n");
        promptBuilder.append("2. summary: 이 문의를 **관련 부서에 맞게** 요약해주세요. (200자 이내)\n");
        promptBuilder.append("   각 부서별 요약 스타일:\n");
        promptBuilder.append("   - **개발팀**: 기술적 용어 사용, 버그/기능/API/에러 코드 등 구체적 기술 정보 포함\n");
        promptBuilder.append("     예: \"로그인 API 호출 시 500 에러 발생 (크롬 브라우저, POST /api/auth/login)\"\n");
        promptBuilder.append("   - **운영팀**: 서비스 안정성, 인프라, 모니터링, 장애 상황 등 운영 관점\n");
        promptBuilder.append("     예: \"서버 접속 불가 상태 지속, 응답 시간 초과 (약 30분간)\"\n");
        promptBuilder.append("   - **QA팀**: 테스트, 품질, 재현 가능성, 버그 리포트 등 QA 관점\n");
        promptBuilder.append("     예: \"결제 프로세스에서 금액 차감 후 주문 실패 버그 재현됨\"\n");
        promptBuilder.append("   - **마케팅팀**: 고객 경험, 브랜딩, 캠페인, 이벤트 등 마케팅 관점\n");
        promptBuilder.append("     예: \"신규 가입 이벤트 혜택 문의, 프로모션 페이지 접근 불가\"\n");
        promptBuilder.append("   - **기획팀**: 기능 요구사항, 사용자 경험, 개선사항, 우선순위 등 기획 관점\n");
        promptBuilder.append("     예: \"다크모드 기능 추가 요청, 사용자 편의성 개선 필요\"\n");
        promptBuilder.append("   - **여러 부서 관련 시**: 각 부서가 이해할 수 있도록 종합적으로 요약\n\n");
        promptBuilder.append("3. keyFeedback: 주요 피드백이 있다면 추출해주세요. 없다면 빈 문자열을 반환하세요.\n");
        promptBuilder.append("   - 부서별로 중요한 정보를 포함하세요.\n");
        promptBuilder.append("   - 개발팀: 에러 메시지, 재현 단계, 기술적 세부사항\n");
        promptBuilder.append("   - 운영팀: 장애 시간, 영향 범위, 복구 상태\n");
        promptBuilder.append("   - QA팀: 버그 재현 방법, 테스트 환경, 예상 결과 vs 실제 결과\n");
        promptBuilder.append("   - 마케팅팀: 고객 불만, 브랜드 이미지, 캠페인 효과\n");
        promptBuilder.append("   - 기획팀: 사용자 요구사항, UX 문제, 개선 제안\n\n");
        promptBuilder.append("응답 형식:\n");
        promptBuilder.append("{\n");
        promptBuilder.append("  \"department\": [\"개발팀\"],\n");
        promptBuilder.append("  \"summary\": \"로그인 API 호출 시 500 에러 발생 (크롬 브라우저, POST /api/auth/login)\",\n");
        promptBuilder.append("  \"keyFeedback\": \"로그인 버튼 클릭 시 서버 에러 발생, 콘솔에 500 Internal Server Error 확인\"\n");
        promptBuilder.append("}\n\n");
        promptBuilder.append("여러 부서 관련 예시:\n");
        promptBuilder.append("{\n");
        promptBuilder.append("  \"department\": [\"개발팀\", \"운영팀\"],\n");
        promptBuilder.append("  \"summary\": \"결제 API 오류 및 서버 응답 지연 (POST /api/payment, 응답 시간 10초 이상)\",\n");
        promptBuilder.append("  \"keyFeedback\": \"결제 버튼 클릭 시 API 호출 실패, 서버 응답 시간 초과로 인한 타임아웃 발생\"\n");
        promptBuilder.append("}\n\n");
        promptBuilder.append("채팅 내용:\n");

        for (ChatMessage msg : chatMessages) {
            String senderLabel = switch (msg.getSender().toString()) {
                case "user" -> "고객";
                case "agent" -> "상담사";
                default -> "시스템";
            };
            promptBuilder.append(String.format("[%s] %s: %s\n", 
                msg.getTimestamp(), senderLabel, msg.getMessage()));
        }

        return promptBuilder.toString();
    }

    /**
     * OpenAI API 호출
     */
    private String callOpenAI(String prompt) throws IOException, InterruptedException {
        String requestBody = objectMapper.writeValueAsString(new OpenAIRequest(
            "gpt-4o-mini",
            List.of(new Message("user", prompt)),
            0.3
        ));

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(openaiApiUrl))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + openaiApiKey)
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .timeout(Duration.ofSeconds(60))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new IOException("OpenAI API 호출 실패: " + response.statusCode() + " - " + response.body());
        }

        return response.body();
    }

    /**
     * OpenAI 응답 파싱
     */
    private AnalysisResult parseResponse(String responseBody) {
        try {
            JsonNode jsonNode = objectMapper.readTree(responseBody);
            String content = jsonNode
                    .path("choices")
                    .get(0)
                    .path("message")
                    .path("content")
                    .asText();

            // JSON 부분만 추출 (마크다운 코드 블록 제거)
            content = content.replaceAll("```json\\s*", "").replaceAll("```\\s*", "").trim();
            
            JsonNode resultNode = objectMapper.readTree(content);
            
            // department를 배열로 파싱
            List<String> departments = new ArrayList<>();
            JsonNode departmentNode = resultNode.path("department");
            if (departmentNode.isArray()) {
                for (JsonNode dept : departmentNode) {
                    String deptName = dept.asText();
                    if (deptName != null && !deptName.isBlank()) {
                        departments.add(deptName);
                    }
                }
            } else if (departmentNode.isTextual()) {
                // 하위 호환성: 문자열로 온 경우도 처리
                String deptName = departmentNode.asText();
                if (deptName != null && !deptName.isBlank()) {
                    departments.add(deptName);
                }
            }
            
            return new AnalysisResult(
                departments,
                resultNode.path("summary").asText(""),
                resultNode.path("keyFeedback").asText("")
            );
        } catch (Exception e) {
            return AnalysisResult.empty();
        }
    }

    /**
     * 분석 결과
     */
    public record AnalysisResult(
        List<String> department,
        String summary,
        String keyFeedback
    ) {
        public static AnalysisResult empty() {
            return new AnalysisResult(new ArrayList<>(), "", "");
        }
    }

    /**
     * OpenAI API 요청 DTO
     */
    private record OpenAIRequest(
        String model,
        List<Message> messages,
        double temperature
    ) {}

    /**
     * OpenAI API 메시지 DTO
     */
    private record Message(
        String role,
        String content
    ) {}
}

