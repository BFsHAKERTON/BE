package channal.bfs.stats.infrastructure;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.springframework.stereotype.Repository;

import java.math.BigInteger;
import java.sql.Timestamp;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Repository
public class StatsQueryRepository {

    private final EntityManager entityManager;

    public StatsQueryRepository(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    public record TagCountRecord(UUID tagId, String tagName, long count) {}

    public List<TagCountRecord> findTagStats(OffsetDateTime start, OffsetDateTime end, Integer limit) {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT t.id, t.name, COUNT(*) AS cnt \n")
           .append("FROM inquiry_tags it \n")
           .append("JOIN tag t ON t.id = it.tag_id \n")
           .append("JOIN inquiry i ON i.id = it.inquiry_id \n")
           .append("WHERE i.created_at >= :start AND i.created_at < :end \n")
           .append("GROUP BY t.id, t.name \n")
           .append("ORDER BY cnt DESC");

        if (limit != null) {
            sql.append(" LIMIT :limit");
        }

        Query query = entityManager.createNativeQuery(sql.toString());
        query.setParameter("start", start);
        query.setParameter("end", end);
        if (limit != null) {
            query.setParameter("limit", limit);
        }

        List<Object[]> rows = query.getResultList();
        List<TagCountRecord> results = new ArrayList<>();
        for (Object[] row : rows) {
            UUID tagId = (UUID) row[0];
            String tagName = (String) row[1];
            Number count = (Number) row[2];
            results.add(new TagCountRecord(tagId, tagName, count.longValue()));
        }
        return results;
    }

    public record RegionCountRecord(String city, long count) {}

    public List<RegionCountRecord> findRegionStats(OffsetDateTime start, OffsetDateTime end) {
        Query query = entityManager.createNativeQuery(
                "SELECT i.customer_city, COUNT(*) AS cnt \n" +
                "FROM inquiry i \n" +
                "WHERE i.customer_city IS NOT NULL \n" +
                "  AND i.created_at >= :start AND i.created_at < :end \n" +
                "GROUP BY i.customer_city \n" +
                "ORDER BY cnt DESC"
        );
        query.setParameter("start", start);
        query.setParameter("end", end);

        List<Object[]> rows = query.getResultList();
        List<RegionCountRecord> results = new ArrayList<>();
        for (Object[] row : rows) {
            String city = (String) row[0];
            Number count = (Number) row[1];
            results.add(new RegionCountRecord(city, count.longValue()));
        }
        return results;
    }

    public record InquiryRawRecord(UUID inquiryId, OffsetDateTime createdAt, UUID tagId, String tagName,
                                   String customerCity, String customerGrade, String channelTalkUserId) {}

    public List<InquiryRawRecord> findInquiryRaw(OffsetDateTime start, OffsetDateTime end, Integer limit) {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT i.id, i.created_at, t.id AS tag_id, t.name AS tag_name, \n")
           .append("       i.customer_city, NULL AS customer_grade, NULL AS channel_talk_user_id \n")
           .append("FROM inquiry i \n")
           .append("JOIN inquiry_tags it ON it.inquiry_id = i.id \n")
           .append("JOIN tag t ON t.id = it.tag_id \n")
           .append("WHERE i.created_at >= :start AND i.created_at < :end \n")
           .append("ORDER BY i.created_at DESC");

        if (limit != null) {
            sql.append(" LIMIT :limit");
        }

        Query query = entityManager.createNativeQuery(sql.toString());
        query.setParameter("start", start);
        query.setParameter("end", end);
        if (limit != null) {
            query.setParameter("limit", limit);
        }

        List<Object[]> rows = query.getResultList();
        List<InquiryRawRecord> results = new ArrayList<>();
        for (Object[] row : rows) {
            UUID inquiryId = (UUID) row[0];
            Object createdAtObj = row[1];
            OffsetDateTime createdAt;
            if (createdAtObj instanceof OffsetDateTime offsetDateTime) {
                createdAt = offsetDateTime;
            } else if (createdAtObj instanceof Timestamp timestamp) {
                createdAt = timestamp.toInstant().atOffset(ZoneOffset.UTC);
            } else {
                createdAt = null;
            }
            UUID tagId = (UUID) row[2];
            String tagName = (String) row[3];
            String customerCity = (String) row[4];
            String customerGrade = (String) row[5];
            String channelTalkUserId = (String) row[6];
            results.add(new InquiryRawRecord(inquiryId, createdAt, tagId, tagName, customerCity, customerGrade, channelTalkUserId));
        }
        return results;
    }

    public long countInquiries(OffsetDateTime start, OffsetDateTime end) {
        Query query = entityManager.createNativeQuery(
                "SELECT COUNT(*) FROM inquiry i WHERE i.created_at >= :start AND i.created_at < :end"
        );
        query.setParameter("start", start);
        query.setParameter("end", end);
        Object result = query.getSingleResult();
        if (result instanceof BigInteger bigInteger) {
            return bigInteger.longValue();
        }
        if (result instanceof Number number) {
            return number.longValue();
        }
        return 0L;
    }
}


