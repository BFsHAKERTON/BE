package channal.bfs.stats.presentation;

import channal.bfs.api.StatsApi;
import channal.bfs.model.InquiryRawResponse;
import channal.bfs.model.InquirySummaryResponse;
import channal.bfs.model.RegionDistributionResponse;
import channal.bfs.model.TagStatListResponse;
import channal.bfs.stats.application.StatsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;

@RequiredArgsConstructor
@RestController
public class StatsApiController implements StatsApi {

    private final StatsService statsService;

    @Override
    public ResponseEntity<TagStatListResponse> statsTagsTopWeeklyGet(Integer limit) {
        return ResponseEntity.ok(statsService.getTopTagsWeekly(limit != null ? limit : 5));
    }

    @Override
    public ResponseEntity<TagStatListResponse> statsTagsMonthlyGet(OffsetDateTime start, OffsetDateTime end) {
        return ResponseEntity.ok(statsService.getTagStats(start, end));
    }

    @Override
    public ResponseEntity<RegionDistributionResponse> statsCustomersRegionsGet(OffsetDateTime start, OffsetDateTime end) {
        return ResponseEntity.ok(statsService.getRegionDistribution(start, end));
    }

    @Override
    public ResponseEntity<InquiryRawResponse> statsInquiriesRawGet(OffsetDateTime start, OffsetDateTime end, Integer limit) {
        return ResponseEntity.ok(statsService.getInquiryRaw(start, end, limit));
    }

    @Override
    public ResponseEntity<InquirySummaryResponse> statsInquiriesSummaryGet(OffsetDateTime start, OffsetDateTime end) {
        return ResponseEntity.ok(statsService.getInquirySummary(start, end));
    }
}


