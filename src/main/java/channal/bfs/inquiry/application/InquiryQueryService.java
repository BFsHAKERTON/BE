package channal.bfs.inquiry.application;

import channal.bfs.common.exception.AppException;
import channal.bfs.inquiry.infrastructure.ChatMessageEntity;
import channal.bfs.inquiry.infrastructure.InquiryEntity;
import channal.bfs.inquiry.infrastructure.InquiryJpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 문의 조회를 담당하는 서비스
 */
@Service
@Transactional(readOnly = true)
public class InquiryQueryService {

    private final InquiryJpaRepository inquiryRepository;

    public InquiryQueryService(InquiryJpaRepository inquiryRepository) {
        this.inquiryRepository = inquiryRepository;
    }

    /**
     * 문의 상세 조회
     */
    public InquiryEntity getInquiryById(UUID inquiryId) {
        return inquiryRepository.findById(inquiryId)
                .orElseThrow(() -> new AppException("INQUIRY_NOT_FOUND", "문의를 찾을 수 없습니다."));
    }

    /**
     * 문의 리스트 조회 (페이지네이션)
     */
    public Page<InquiryEntity> getInquiries(int page, int limit) {
        Pageable pageable = PageRequest.of(page - 1, limit, Sort.by(Sort.Direction.DESC, "createdAt"));
        return inquiryRepository.findAll(pageable);
    }

    /**
     * 문의의 채팅 메시지 조회
     */
    public List<ChatMessageEntity> getChatMessages(UUID inquiryId) {
        InquiryEntity inquiry = inquiryRepository.findById(inquiryId)
                .orElseThrow(() -> new AppException("INQUIRY_NOT_FOUND", "문의를 찾을 수 없습니다."));
        
        // LAZY 로딩을 위해 chatMessages 접근 (트랜잭션 내에서)
        return inquiry.getChatMessages().stream()
                .sorted((a, b) -> a.getTimestamp().compareTo(b.getTimestamp()))
                .collect(Collectors.toList());
    }
}

