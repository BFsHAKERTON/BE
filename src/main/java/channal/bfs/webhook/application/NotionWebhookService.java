package channal.bfs.webhook.application;

import channal.bfs.inquiry.application.LlmAnalysisService;
import channal.bfs.inquiry.infrastructure.InquiryEntity;
import channal.bfs.integration.notion.dto.NotionPage;
import channal.bfs.integration.notion.dto.NotionPageCreateRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

/**
 * 노션 웹훅 서비스
 * 시스템 노션 계정으로 페이지를 생성합니다.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NotionWebhookService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${notion.base-url}")
    private String notionBaseUrl;

    @Value("${notion.system.token:}")
    private String systemToken;

    @Value("${notion.system.database-id:}")
    private String systemDatabaseId;

    /**
     * 시스템 노션 계정으로 페이지 생성
     *
     * @param inquiry 문의 엔티티
     * @param analysisResult LLM 분석 결과
     * @return 노션 페이지 URL
     */
    public String createNotionPage(InquiryEntity inquiry, LlmAnalysisService.AnalysisResult analysisResult) {
        if (systemToken == null || systemToken.isBlank()) {
            log.warn("시스템 노션 토큰이 설정되지 않았습니다.");
            throw new IllegalStateException("시스템 노션 토큰이 설정되지 않았습니다.");
        }

        if (systemDatabaseId == null || systemDatabaseId.isBlank()) {
            log.warn("시스템 노션 데이터베이스 ID가 설정되지 않았습니다.");
            throw new IllegalStateException("시스템 노션 데이터베이스 ID가 설정되지 않았습니다.");
        }

        try {
            NotionPageCreateRequest request = buildPageCreateRequest(inquiry, analysisResult);
            NotionPage page = callNotionApi(request);

            String pageUrl = page.getUrl();
            log.info("노션 페이지 생성 완료: {}", pageUrl);
            return pageUrl;
        } catch (Exception e) {
            log.error("노션 페이지 생성 실패", e);
            throw new RuntimeException("노션 페이지 생성 실패", e);
        }
    }

    /**
     * 페이지 생성 요청 빌드
     */
    private NotionPageCreateRequest buildPageCreateRequest(
            InquiryEntity inquiry,
            LlmAnalysisService.AnalysisResult analysisResult) {

        // Parent: 데이터베이스 참조
        Map<String, Object> parent = new HashMap<>();
        parent.put("type", "database_id");
        parent.put("database_id", systemDatabaseId);

        // Properties: 데이터베이스 속성
        Map<String, Object> properties = new HashMap<>();

        // Title 속성 (제목)
        Map<String, Object> titleProp = new HashMap<>();
        titleProp.put("title", List.of(Map.of(
                "type", "text",
                "text", Map.of("content", inquiry.getTitle())
        )));
        properties.put("제목", titleProp);

        // Status 속성
        if (inquiry.getStatus() != null) {
            Map<String, Object> statusProp = new HashMap<>();
            statusProp.put("status", Map.of("name", inquiry.getStatus().name()));
            properties.put("상태", statusProp);
        }

        // 요약 (rich_text)
        if (analysisResult.summary() != null && !analysisResult.summary().isBlank()) {
            Map<String, Object> summaryProp = new HashMap<>();
            summaryProp.put("rich_text", List.of(Map.of(
                    "type", "text",
                    "text", Map.of("content", analysisResult.summary())
            )));
            properties.put("요약", summaryProp);
        }

        // 주요 피드백 (rich_text)
        if (analysisResult.keyFeedback() != null && !analysisResult.keyFeedback().isBlank()) {
            Map<String, Object> feedbackProp = new HashMap<>();
            feedbackProp.put("rich_text", List.of(Map.of(
                    "type", "text",
                    "text", Map.of("content", analysisResult.keyFeedback())
            )));
            properties.put("주요 피드백", feedbackProp);
        }

        // 채널톡 URL (url)
        if (inquiry.getChannelTalkUrl() != null && !inquiry.getChannelTalkUrl().isBlank()) {
            Map<String, Object> urlProp = new HashMap<>();
            urlProp.put("url", inquiry.getChannelTalkUrl());
            properties.put("채널톡 URL", urlProp);
        }

        // 고객 도시 (rich_text)
        if (inquiry.getCustomerCity() != null && !inquiry.getCustomerCity().isBlank()) {
            Map<String, Object> cityProp = new HashMap<>();
            cityProp.put("rich_text", List.of(Map.of(
                    "type", "text",
                    "text", Map.of("content", inquiry.getCustomerCity())
            )));
            properties.put("고객 도시", cityProp);
        }

        // 부서 (multi_select)
        if (analysisResult.department() != null && !analysisResult.department().isEmpty()) {
            Map<String, Object> deptProp = new HashMap<>();
            List<Map<String, String>> multiSelect = new ArrayList<>();
            for (String dept : analysisResult.department()) {
                multiSelect.add(Map.of("name", dept));
            }
            deptProp.put("multi_select", multiSelect);
            properties.put("부서", deptProp);
        }

        // Children: 페이지 본문 (채팅 메시지는 여기 포함 가능)
        List<Map<String, Object>> children = new ArrayList<>();

        // 전체 요약 섹션
        if (analysisResult.generalSummary() != null && !analysisResult.generalSummary().isBlank()) {
            children.add(Map.of(
                    "object", "block",
                    "type", "heading_2",
                    "heading_2", Map.of(
                            "rich_text", List.of(Map.of(
                                    "type", "text",
                                    "text", Map.of("content", "전체 요약")
                            ))
                    )
            ));
            children.add(Map.of(
                    "object", "block",
                    "type", "paragraph",
                    "paragraph", Map.of(
                            "rich_text", List.of(Map.of(
                                    "type", "text",
                                    "text", Map.of("content", analysisResult.generalSummary())
                            ))
                    )
            ));
        }

        // 부서별 요약 섹션
        if (analysisResult.summary() != null && !analysisResult.summary().isBlank()) {
            children.add(Map.of(
                    "object", "block",
                    "type", "heading_2",
                    "heading_2", Map.of(
                            "rich_text", List.of(Map.of(
                                    "type", "text",
                                    "text", Map.of("content", "부서별 요약")
                            ))
                    )
            ));
            children.add(Map.of(
                    "object", "block",
                    "type", "paragraph",
                    "paragraph", Map.of(
                            "rich_text", List.of(Map.of(
                                    "type", "text",
                                    "text", Map.of("content", analysisResult.summary())
                            ))
                    )
            ));
        }

        return NotionPageCreateRequest.builder()
                .parent(parent)
                .properties(properties)
                .children(children)
                .build();
    }

    /**
     * 노션 API 호출
     */
    private NotionPage callNotionApi(NotionPageCreateRequest request) {
        String url = notionBaseUrl + "/pages";

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + systemToken);
        headers.set("Content-Type", "application/json");
        headers.set("Notion-Version", "2022-06-28");

        HttpEntity<NotionPageCreateRequest> entity = new HttpEntity<>(request, headers);

        log.info("노션 API 호출: POST {}", url);
        ResponseEntity<NotionPage> response = restTemplate.exchange(
                url,
                HttpMethod.POST,
                entity,
                NotionPage.class
        );

        if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
            return response.getBody();
        } else {
            throw new RuntimeException("노션 API 호출 실패: " + response.getStatusCode());
        }
    }
}
