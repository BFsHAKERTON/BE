package channal.bfs.integration.notion.presentation;

import channal.bfs.api.NotionApi;
import channal.bfs.integration.notion.application.NotionService;
import channal.bfs.integration.notion.dto.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/notion")
@RequiredArgsConstructor
public class NotionApiController implements NotionApi {

    private final NotionService notionService;

    /**
     * 데이터베이스 쿼리 (필터링)
     * POST /api/notion/databases/{databaseId}/query?userId=1
     */
    @PostMapping("/databases/{databaseId}/query")
    public ResponseEntity<NotionDatabaseQueryResponse> queryDatabase(
            @PathVariable String databaseId,
            @RequestBody NotionDatabaseQueryRequest request,
            @RequestParam(value = "userId") UUID userId) {
        NotionDatabaseQueryResponse response = notionService.queryDatabase(userId, databaseId, request);
        return ResponseEntity.ok(response);
    }

    /**
     * 데이터베이스 생성 (작업 관리 보드)
     * POST /api/notion/databases?userId=1
     */
    @PostMapping("/databases")
    public ResponseEntity<NotionDatabaseCreateResponse> createDatabase(
            @RequestBody NotionDatabaseCreateRequest request,
            @RequestParam(value = "userId") UUID userId) {
        NotionDatabaseCreateResponse response = notionService.createDatabase(userId, request);
        return ResponseEntity.ok(response);
    }

    /**
     * 데이터베이스 업데이트
     * PATCH /api/notion/databases/{databaseId}?userId=1
     */
    @PatchMapping("/databases/{databaseId}")
    public ResponseEntity<NotionDatabaseCreateResponse> updateDatabase(
            @PathVariable String databaseId,
            @RequestBody NotionDatabaseUpdateRequest request,
            @RequestParam(value = "userId") UUID userId) {
        NotionDatabaseCreateResponse response = notionService.updateDatabase(userId, databaseId, request);
        return ResponseEntity.ok(response);
    }

    /**
     * 페이지 생성
     * POST /api/notion/pages?userId=1
     */
    @PostMapping("/pages")
    public ResponseEntity<NotionPage> createPage(
            @RequestBody NotionPageCreateRequest request,
            @RequestParam(value = "userId") UUID userId) {
        NotionPage page = notionService.createPage(userId, request);
        return ResponseEntity.ok(page);
    }

    /**
     * 페이지 조회
     * GET /api/notion/pages/{pageId}?userId=1
     */
    @GetMapping("/pages/{pageId}")
    public ResponseEntity<NotionPage> getPage(
            @PathVariable String pageId,
            @RequestParam(value = "userId") UUID userId) {
        NotionPage page = notionService.getPage(userId, pageId);
        return ResponseEntity.ok(page);
    }

    /**
     * 페이지 업데이트
     * PATCH /api/notion/pages/{pageId}?userId=1
     */
    @PatchMapping("/pages/{pageId}")
    public ResponseEntity<NotionPage> updatePage(
            @PathVariable String pageId,
            @RequestBody NotionPageUpdateRequest request,
            @RequestParam(value = "userId") UUID userId) {
        NotionPage page = notionService.updatePage(userId, pageId, request);
        return ResponseEntity.ok(page);
    }
}


