package channal.bfs.integration.notion.infrastructure;

import channal.bfs.integration.notion.application.NotionOAuthService;
import channal.bfs.integration.notion.dto.*;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class NotionClient {

    private final RestTemplate restTemplate;
    private final NotionOAuthService oauthService;

    @Value("${notion.base-url}")
    private String baseUrl;

    /**
     * 사용자 토큰으로 HTTP 헤더 생성
     */
    private HttpHeaders createHeaders(UUID userId) {
        HttpHeaders headers = new HttpHeaders();
        String accessToken = oauthService.getAccessToken(userId);
        headers.set("Authorization", "Bearer " + accessToken);
        headers.set("Content-Type", "application/json");
        headers.set("Notion-Version", "2022-06-28");
        return headers;
    }

    /**
     * 데이터베이스 쿼리 (필터링)
     * @param userId 사용자 ID
     * @param databaseId 데이터베이스 ID
     * @param request 쿼리 요청 (필터, 정렬 등)
     * @return 쿼리 결과
     */
    public NotionDatabaseQueryResponse queryDatabase(UUID userId, String databaseId, NotionDatabaseQueryRequest request) {
        String url = baseUrl + "/databases/" + databaseId + "/query";
        HttpHeaders headers = createHeaders(userId);
        HttpEntity<NotionDatabaseQueryRequest> entity = new HttpEntity<>(request, headers);

        ResponseEntity<NotionDatabaseQueryResponse> response = restTemplate.exchange(
                url, HttpMethod.POST, entity, NotionDatabaseQueryResponse.class
        );
        return response.getBody();
    }

    /**
     * 페이지 생성
     * @param userId 사용자 ID
     * @param request 페이지 생성 요청
     * @return 생성된 페이지 정보
     */
    public NotionPage createPage(UUID userId, NotionPageCreateRequest request) {
        String url = baseUrl + "/pages";
        HttpHeaders headers = createHeaders(userId);
        HttpEntity<NotionPageCreateRequest> entity = new HttpEntity<>(request, headers);

        ResponseEntity<NotionPage> response = restTemplate.exchange(
                url, HttpMethod.POST, entity, NotionPage.class
        );
        return response.getBody();
    }

    /**
     * 페이지 조회
     * @param userId 사용자 ID
     * @param pageId 페이지 ID
     * @return 페이지 정보
     */
    public NotionPage getPage(UUID userId, String pageId) {
        String url = baseUrl + "/pages/" + pageId;
        HttpHeaders headers = createHeaders(userId);
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        ResponseEntity<NotionPage> response = restTemplate.exchange(
                url, HttpMethod.GET, entity, NotionPage.class
        );
        return response.getBody();
    }

    /**
     * 페이지 업데이트
     * @param userId 사용자 ID
     * @param pageId 페이지 ID
     * @param request 업데이트 요청
     * @return 업데이트된 페이지 정보
     */
    public NotionPage updatePage(UUID userId, String pageId, NotionPageUpdateRequest request) {
        String url = baseUrl + "/pages/" + pageId;
        HttpHeaders headers = createHeaders(userId);
        HttpEntity<NotionPageUpdateRequest> entity = new HttpEntity<>(request, headers);

        restTemplate.exchange(url, HttpMethod.PATCH, entity, NotionPage.class);
        return getPage(userId, pageId);
    }
}
