package channal.bfs.inquiry.infrastructure;

import channal.bfs.common.domain.InquiryStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface InquiryJpaRepository extends JpaRepository<InquiryEntity, UUID> {
    Page<InquiryEntity> findByStatus(InquiryStatus status, Pageable pageable);
    
    @Query("SELECT i FROM InquiryEntity i WHERE i.createdAt >= :startDate")
    List<InquiryEntity> findSince(@Param("startDate") OffsetDateTime startDate);
    
    @Query("SELECT COUNT(i) FROM InquiryEntity i WHERE i.createdAt >= :startDate AND i.createdAt < :endDate")
    Long countByDateRange(@Param("startDate") OffsetDateTime startDate, @Param("endDate") OffsetDateTime endDate);
}

