package channal.bfs.inquiry.application;

import channal.bfs.auth.infrastructure.AppUserEntity;
import channal.bfs.auth.infrastructure.JpaUserRepository;
import channal.bfs.common.domain.InquiryStatus;
import channal.bfs.common.exception.AppException;
import channal.bfs.inquiry.infrastructure.ChatMessageEntity;
import channal.bfs.inquiry.infrastructure.InquiryEntity;
import channal.bfs.inquiry.infrastructure.InquiryJpaRepository;
import channal.bfs.model.ChatMessage;
import channal.bfs.model.InquiryCreateRequest;
import channal.bfs.tag.infrastructure.DepartmentEntity;
import channal.bfs.tag.infrastructure.DepartmentJpaRepository;
import channal.bfs.tag.infrastructure.TagEntity;
import channal.bfs.tag.infrastructure.TagJpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 문의 생성 및 수정을 담당하는 서비스
 */
@Service
@Transactional
public class InquiryCommandService {

    private final InquiryJpaRepository inquiryRepository;
    private final JpaUserRepository userRepository;
    private final TagJpaRepository tagRepository;
    private final DepartmentJpaRepository departmentRepository;
    private final LlmAnalysisService llmAnalysisService;

    public InquiryCommandService(
            InquiryJpaRepository inquiryRepository,
            JpaUserRepository userRepository,
            TagJpaRepository tagRepository,
            DepartmentJpaRepository departmentRepository,
            LlmAnalysisService llmAnalysisService) {
        this.inquiryRepository = inquiryRepository;
        this.userRepository = userRepository;
        this.tagRepository = tagRepository;
        this.departmentRepository = departmentRepository;
        this.llmAnalysisService = llmAnalysisService;
    }

    /**
     * 문의 생성
     */
    public UUID createInquiry(InquiryCreateRequest request) {
        // Requester 조회
        AppUserEntity requester = userRepository.findById(request.getRequesterId())
                .orElseThrow(() -> new AppException("USER_NOT_FOUND", "요청자를 찾을 수 없습니다."));

        // Inquiry 엔티티 생성
        InquiryEntity inquiry = new InquiryEntity();
        inquiry.setTitle(request.getTitle());
        inquiry.setStatus(request.getStatus() != null 
            ? InquiryStatus.valueOf(request.getStatus().toString()) 
            : InquiryStatus.NEW);
        inquiry.setRequester(requester);
        inquiry.setChannelTalkUrl(request.getChannelTalkUrl() != null 
            ? request.getChannelTalkUrl().toString() 
            : null);
        inquiry.setNotionPageUrl(request.getNotionPageUrl() != null 
            ? request.getNotionPageUrl().toString() 
            : null);

        // LLM 분석 수행 (chatMessages가 있는 경우)
        if (request.getChatMessages() != null && !request.getChatMessages().isEmpty()) {
            List<ChatMessage> chatMessages = request.getChatMessages();
            LlmAnalysisService.AnalysisResult analysisResult = llmAnalysisService.analyzeChatMessages(chatMessages);

            // 요약 및 주요 피드백 설정
            if (analysisResult.summary() != null && !analysisResult.summary().isBlank()) {
                inquiry.setSummary(analysisResult.summary());
            }
            if (analysisResult.keyFeedback() != null && !analysisResult.keyFeedback().isBlank()) {
                inquiry.setKeyFeedback(analysisResult.keyFeedback());
            }

            // 부서 분류 (LLM 결과가 있으면 부서 찾기 또는 생성)
            if (analysisResult.department() != null && !analysisResult.department().isEmpty()) {
                for (String deptName : analysisResult.department()) {
                    if (deptName != null && !deptName.isBlank()) {
                        DepartmentEntity department = findOrCreateDepartment(deptName);
                        inquiry.getDepartments().add(department);
                    }
                }
            }
        }

        // 요청에서 직접 전달된 값들도 설정 (LLM 결과보다 우선)
        if (request.getSummary() != null && !request.getSummary().isBlank()) {
            inquiry.setSummary(request.getSummary());
        }
        if (request.getKeyFeedback() != null && !request.getKeyFeedback().isBlank()) {
            inquiry.setKeyFeedback(request.getKeyFeedback());
        }

        // 태그 설정
        if (request.getTagIds() != null && !request.getTagIds().isEmpty()) {
            List<TagEntity> tags = tagRepository.findByIdIn(request.getTagIds());
            inquiry.setTags(new HashSet<>(tags));
        }

        // 담당자 설정
        if (request.getAssigneeIds() != null && !request.getAssigneeIds().isEmpty()) {
            List<AppUserEntity> assignees = request.getAssigneeIds().stream()
                    .map(id -> userRepository.findById(id)
                            .orElseThrow(() -> new AppException("USER_NOT_FOUND", "담당자를 찾을 수 없습니다: " + id)))
                    .collect(Collectors.toList());
            inquiry.setAssignees(new HashSet<>(assignees));
        }

        // 부서 설정 (요청에서 직접 전달된 경우)
        if (request.getDepartmentIds() != null && !request.getDepartmentIds().isEmpty()) {
            List<DepartmentEntity> departments = departmentRepository.findByIdIn(request.getDepartmentIds());
            inquiry.getDepartments().addAll(departments);
        }

        // Inquiry 저장
        InquiryEntity savedInquiry = inquiryRepository.save(inquiry);

        // 채팅 메시지 저장
        if (request.getChatMessages() != null && !request.getChatMessages().isEmpty()) {
            for (ChatMessage msg : request.getChatMessages()) {
                ChatMessageEntity entity = new ChatMessageEntity();
                entity.setInquiry(savedInquiry);
                
                // SenderEnum을 ChatSenderType으로 변환
                String senderValue = msg.getSender().getValue(); // "user", "agent", "system"
                entity.setSender(channal.bfs.common.domain.ChatSenderType.valueOf(senderValue));
                
                entity.setMessage(msg.getMessage());
                entity.setTimestamp(msg.getTimestamp() != null 
                    ? msg.getTimestamp() 
                    : OffsetDateTime.now());
                
                savedInquiry.getChatMessages().add(entity);
            }
            
            inquiryRepository.save(savedInquiry);
        }

        return savedInquiry.getId();
    }

    /**
     * 부서를 이름으로 찾거나 없으면 생성
     */
    private DepartmentEntity findOrCreateDepartment(String departmentName) {
        return departmentRepository.findByName(departmentName)
                .orElseGet(() -> {
                    DepartmentEntity newDepartment = new DepartmentEntity();
                    newDepartment.setName(departmentName);
                    return departmentRepository.save(newDepartment);
                });
    }
}

