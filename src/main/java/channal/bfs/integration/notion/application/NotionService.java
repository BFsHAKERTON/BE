package channal.bfs.integration.notion.application;

import channal.bfs.integration.notion.dto.*;
import channal.bfs.integration.notion.infrastructure.NotionClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotionService {

    private final NotionClient notionClient;

    /**
     * 데이터베이스 쿼리 (필터링)
     * @param userId 사용자 ID
     * @param databaseId 데이터베이스 ID
     * @param request 쿼리 요청
     * @return 쿼리 결과
     */
    public NotionDatabaseQueryResponse queryDatabase(UUID userId, String databaseId, NotionDatabaseQueryRequest request) {
        log.info("Querying Notion database: {} for user: {}", databaseId, userId);
        try {
            return notionClient.queryDatabase(userId, databaseId, request);
        } catch (Exception e) {
            log.error("Failed to query database: {} for user: {}", databaseId, userId, e);
            throw new RuntimeException("Failed to query Notion database", e);
        }
    }

    /**
     * 페이지 생성
     * @param userId 사용자 ID
     * @param request 페이지 생성 요청
     * @return 생성된 페이지 정보
     */
    public NotionPage createPage(UUID userId, NotionPageCreateRequest request) {
        log.info("Creating Notion page for user: {}", userId);
        try {
            return notionClient.createPage(userId, request);
        } catch (Exception e) {
            log.error("Failed to create page for user: {}", userId, e);
            throw new RuntimeException("Failed to create Notion page", e);
        }
    }

    /**
     * 페이지 조회
     * @param userId 사용자 ID
     * @param pageId 페이지 ID
     * @return 페이지 정보
     */
    public NotionPage getPage(UUID userId, String pageId) {
        log.info("Getting Notion page: {} for user: {}", pageId, userId);
        try {
            return notionClient.getPage(userId, pageId);
        } catch (Exception e) {
            log.error("Failed to get page: {} for user: {}", pageId, userId, e);
            throw new RuntimeException("Failed to get Notion page", e);
        }
    }

    /**
     * 페이지 업데이트
     * @param userId 사용자 ID
     * @param pageId 페이지 ID
     * @param request 업데이트 요청
     * @return 업데이트된 페이지 정보
     */
    public NotionPage updatePage(UUID userId, String pageId, NotionPageUpdateRequest request) {
        log.info("Updating Notion page: {} for user: {}", pageId, userId);
        try {
            return notionClient.updatePage(userId, pageId, request);
        } catch (Exception e) {
            log.error("Failed to update page: {} for user: {}", pageId, userId, e);
            throw new RuntimeException("Failed to update Notion page", e);
        }
    }
}
