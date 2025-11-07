package channal.bfs.integration.notion.infrastructure;

import channal.bfs.integration.notion.application.NotionOAuthService;
import channal.bfs.integration.notion.dto.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import java.util.UUID;

import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
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

    public NotionDatabaseCreateResponse createDatabase(UUID userId, NotionDatabaseCreateRequest request){
        String url = baseUrl + "/databases"; //db생성 엔드포인트 주소
        log.info("Creating Notion database at URL: {} for user: {}", url, userId);
        log.debug("Request body: {}", request);

        try {
            HttpHeaders headers = createHeaders(userId);
            HttpEntity<NotionDatabaseCreateRequest> entity = new HttpEntity<>(request, headers); // HTTP 요청 본문과 헤더를 하나로 묶어주는 객체입니다.

            ResponseEntity<NotionDatabaseCreateResponse> response = restTemplate.exchange(
                    url,                //설정한 URL
                    HttpMethod.POST,    // Notion API는 POST 메서드를 통해 데이터베이스를 생성
                    entity,             // 만든 헤더 + 본문
                    NotionDatabaseCreateResponse.class // Notion API가 반환하는 JSON을 NotionDatabaseCreateResponse 자바 객체로 자동 변환
            );

            log.info("Successfully created Notion database with ID: {}", response.getBody().getId());
            return response.getBody();
        } catch (Exception e) {
            log.error("Failed to create Notion database. Error: {}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * 데이터베이스 업데이트
     * @param userId 사용자 ID
     * @param databaseId 데이터베이스 ID
     * @param request 업데이트 요청
     * @return 업데이트된 데이터베이스 정보
     */
    public NotionDatabaseCreateResponse updateDatabase(UUID userId, String databaseId, NotionDatabaseUpdateRequest request) {
        String url = baseUrl + "/databases/" + databaseId;
        log.info("Updating Notion database: {} for user: {}", databaseId, userId);
        log.debug("Request body: {}", request);

        try {
            HttpHeaders headers = createHeaders(userId);
            HttpEntity<NotionDatabaseUpdateRequest> entity = new HttpEntity<>(request, headers);

            ResponseEntity<NotionDatabaseCreateResponse> response = restTemplate.exchange(
                    url,
                    HttpMethod.PATCH,
                    entity,
                    NotionDatabaseCreateResponse.class
            );

            log.info("Successfully updated Notion database with ID: {}", response.getBody().getId());
            return response.getBody();
        } catch (Exception e) {
            log.error("Failed to update Notion database: {}. Error: {}", databaseId, e.getMessage(), e);
            throw e;
        }
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
