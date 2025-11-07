package channal.bfs.stats.application;

import channal.bfs.model.*;
import channal.bfs.stats.infrastructure.StatsQueryRepository;
import channal.bfs.stats.infrastructure.StatsQueryRepository.InquiryRawRecord;
import channal.bfs.stats.infrastructure.StatsQueryRepository.RegionCountRecord;
import channal.bfs.stats.infrastructure.StatsQueryRepository.TagCountRecord;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

@Service
public class StatsService {

    private static final int DEFAULT_MONTHLY_RANGE_DAYS = 30;
    private final StatsQueryRepository statsQueryRepository;

    public StatsService(StatsQueryRepository statsQueryRepository) {
        this.statsQueryRepository = statsQueryRepository;
    }

    public TagStatListResponse getTopTagsWeekly(int limit) {
        OffsetDateTime end = OffsetDateTime.now(ZoneOffset.UTC);
        OffsetDateTime start = end.minusDays(7);
        return buildTagStatResponse(start, end, limit);
    }

    public TagStatListResponse getTagStats(OffsetDateTime start, OffsetDateTime end) {
        Range range = resolveRange(start, end, DEFAULT_MONTHLY_RANGE_DAYS);
        return buildTagStatResponse(range.start(), range.end(), null);
    }

    public RegionDistributionResponse getRegionDistribution(OffsetDateTime start, OffsetDateTime end) {
        Range range = resolveRange(start, end, DEFAULT_MONTHLY_RANGE_DAYS);
        List<RegionCountRecord> records = statsQueryRepository.findRegionStats(range.start(), range.end());

        int total = records.stream().mapToInt(r -> (int) r.count()).sum();
        DateRange dateRange = new DateRange().start(range.start()).end(range.end());
        RegionDistributionResponse response = new RegionDistributionResponse();
        response.setRange(dateRange);
        response.setTotal(total);

        List<RegionDistributionItem> items = new ArrayList<>();
        for (RegionCountRecord record : records) {
            RegionDistributionItem item = new RegionDistributionItem()
                    .city(record.city())
                    .count((int) record.count());
            if (total > 0) {
                item.setRatio((double) record.count() / total);
            }
            items.add(item);
        }
        response.setItems(items);
        return response;
    }

    public InquiryRawResponse getInquiryRaw(OffsetDateTime start, OffsetDateTime end, Integer limit) {
        Range range = resolveRange(start, end, DEFAULT_MONTHLY_RANGE_DAYS);
        List<InquiryRawRecord> records = statsQueryRepository.findInquiryRaw(range.start(), range.end(), limit);

        DateRange dateRange = new DateRange().start(range.start()).end(range.end());
        InquiryRawResponse response = new InquiryRawResponse();
        response.setRange(dateRange);

        List<InquiryRawItem> items = new ArrayList<>();
        for (InquiryRawRecord record : records) {
            InquiryRawItem item = new InquiryRawItem()
                    .inquiryId(record.inquiryId())
                    .createdAt(record.createdAt())
                    .tagId(record.tagId())
                    .tagName(record.tagName())
                    .customerCity(record.customerCity())
                    .customerGrade(record.customerGrade())
                    .channelTalkUserId(record.channelTalkUserId());
            items.add(item);
        }
        response.setItems(items);
        return response;
    }

    public InquirySummaryResponse getInquirySummary(OffsetDateTime start, OffsetDateTime end) {
        Range range = resolveRange(start, end, DEFAULT_MONTHLY_RANGE_DAYS);
        long total = statsQueryRepository.countInquiries(range.start(), range.end());

        DateRange dateRange = new DateRange().start(range.start()).end(range.end());
        InquirySummaryResponse response = new InquirySummaryResponse();
        response.setRange(dateRange);
        response.setTotalInquiries((int) total);
        return response;
    }

    private TagStatListResponse buildTagStatResponse(OffsetDateTime start, OffsetDateTime end, Integer limit) {
        List<TagCountRecord> records = statsQueryRepository.findTagStats(start, end, limit);

        DateRange dateRange = new DateRange().start(start).end(end);
        TagStatListResponse response = new TagStatListResponse();
        response.setRange(dateRange);

        List<TagStatItem> items = new ArrayList<>();
        for (TagCountRecord record : records) {
            TagStatItem item = new TagStatItem()
                    .tagId(record.tagId())
                    .tagName(record.tagName())
                    .count((int) record.count());
            items.add(item);
        }
        response.setItems(items);
        return response;
    }

    private Range resolveRange(OffsetDateTime start, OffsetDateTime end, int defaultDays) {
        OffsetDateTime resolvedEnd = end != null ? end : OffsetDateTime.now(ZoneOffset.UTC);
        OffsetDateTime resolvedStart = start != null ? start : resolvedEnd.minusDays(defaultDays);
        if (resolvedStart.isAfter(resolvedEnd)) {
            OffsetDateTime temp = resolvedStart;
            resolvedStart = resolvedEnd;
            resolvedEnd = temp;
        }
        return new Range(resolvedStart, resolvedEnd);
    }

    private record Range(OffsetDateTime start, OffsetDateTime end) {}
}


