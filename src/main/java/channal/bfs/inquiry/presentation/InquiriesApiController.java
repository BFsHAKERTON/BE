package channal.bfs.inquiry.presentation;

import channal.bfs.api.InquiriesApi;
import channal.bfs.common.exception.AppException;
import channal.bfs.inquiry.application.InquiryCommandService;
import channal.bfs.inquiry.application.InquiryQueryService;
import channal.bfs.inquiry.application.LlmAnalysisService;
import channal.bfs.inquiry.infrastructure.ChatMessageEntity;
import channal.bfs.inquiry.infrastructure.InquiryEntity;
import channal.bfs.model.*;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
public class InquiriesApiController implements InquiriesApi {

    private final InquiryCommandService inquiryCommandService;
    private final InquiryQueryService inquiryQueryService;
    private final LlmAnalysisService llmAnalysisService;

    public InquiriesApiController(
            InquiryCommandService inquiryCommandService,
            InquiryQueryService inquiryQueryService,
            LlmAnalysisService llmAnalysisService) {
        this.inquiryCommandService = inquiryCommandService;
        this.inquiryQueryService = inquiryQueryService;
        this.llmAnalysisService = llmAnalysisService;
    }

    @Override
    public ResponseEntity<InquiryResponse> inquiriesPost(InquiryCreateRequest inquiryCreateRequest) {
        try {
            UUID inquiryId = inquiryCommandService.createInquiry(inquiryCreateRequest);
            
            InquiryResponse response = new InquiryResponse();
            response.setInquiryId(inquiryId);
            response.setTitle(inquiryCreateRequest.getTitle());
            response.setStatus(inquiryCreateRequest.getStatus());
            response.setRequesterId(inquiryCreateRequest.getRequesterId());
            
            if (inquiryCreateRequest.getTagIds() != null) {
                response.setTagIds(inquiryCreateRequest.getTagIds());
            }
            
            if (inquiryCreateRequest.getAssigneeIds() != null) {
                response.setAssigneeIds(inquiryCreateRequest.getAssigneeIds());
            }
            
            if (inquiryCreateRequest.getDepartmentIds() != null) {
                response.setDepartmentIds(inquiryCreateRequest.getDepartmentIds());
            }
            
            response.setSummary(inquiryCreateRequest.getSummary());
            response.setKeyFeedback(inquiryCreateRequest.getKeyFeedback());
            response.setChannelTalkUrl(inquiryCreateRequest.getChannelTalkUrl());
            response.setNotionPageUrl(inquiryCreateRequest.getNotionPageUrl());
            
            // 실제 저장된 엔티티에서 createdAt 가져오기
            InquiryEntity savedInquiry = inquiryQueryService.getInquiryById(inquiryId);
            response.setCreatedAt(savedInquiry.getCreatedAt());

            HttpHeaders headers = new HttpHeaders();
            headers.setLocation(URI.create("/v1/inquiries/" + inquiryId));
            
            return ResponseEntity.status(HttpStatus.CREATED)
                    .headers(headers)
                    .body(response);
                    
        } catch (AppException e) {
            throw e;
        } catch (Exception e) {
            throw new AppException("INQUIRY_CREATE_FAILED", "문의 생성에 실패했습니다: " + e.getMessage());
        }
    }

    @Override
    public ResponseEntity<PaginatedInquiriesResponse> inquiriesGet(Integer page, Integer limit) {
        try {
            int pageNum = page != null ? page : 1;
            int limitNum = limit != null ? limit : 20;
            
            Page<InquiryEntity> inquiryPage = inquiryQueryService.getInquiries(pageNum, limitNum);
            
            PaginatedInquiriesResponse response = new PaginatedInquiriesResponse();
            
            // PaginationInfo 설정
            PaginationInfo pagination = new PaginationInfo();
            pagination.setCurrentPage(pageNum);
            pagination.setTotalCount((int) inquiryPage.getTotalElements());
            pagination.setTotalPages(inquiryPage.getTotalPages());
            response.setPagination(pagination);
            
            // InquirySummary 리스트 변환
            List<InquirySummary> summaries = inquiryPage.getContent().stream()
                    .map(this::toInquirySummary)
                    .collect(Collectors.toList());
            response.setData(summaries);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            throw new AppException("INQUIRY_LIST_FAILED", "문의 리스트 조회에 실패했습니다: " + e.getMessage());
        }
    }

    @Override
    public ResponseEntity<ChatAnalysisResponse> inquiriesAnalyzePost(ChatAnalysisRequest chatAnalysisRequest) {
        try {
            // 요청 검증
            if (chatAnalysisRequest == null || 
                chatAnalysisRequest.getChatMessages() == null || 
                chatAnalysisRequest.getChatMessages().isEmpty()) {
                throw new AppException("INVALID_REQUEST", "chatMessages는 필수이며 최소 1개 이상이어야 합니다.");
            }

            // LLM 분석 수행
            LlmAnalysisService.AnalysisResult analysisResult = llmAnalysisService.analyzeChatMessages(
                chatAnalysisRequest.getChatMessages()
            );

            // 응답 생성
            ChatAnalysisResponse response = new ChatAnalysisResponse();
            response.setDepartment(analysisResult.department() != null ? analysisResult.department() : new ArrayList<>());
            response.setGeneralSummary(analysisResult.generalSummary() != null ? analysisResult.generalSummary() : "");
            response.setSummary(analysisResult.summary() != null ? analysisResult.summary() : "");
            response.setKeyFeedback(analysisResult.keyFeedback() != null ? analysisResult.keyFeedback() : "");

            return ResponseEntity.ok(response);

        } catch (AppException e) {
            throw e;
        } catch (Exception e) {
            // LLM 분석 실패 시 500 에러
            throw new AppException("LLM_ANALYSIS_FAILED", "LLM 분석에 실패했습니다: " + e.getMessage());
        }
    }

    @Override
    public ResponseEntity<InquiryDetail> inquiriesInquiryIdGet(UUID inquiryId) {
        try {
            InquiryEntity inquiry = inquiryQueryService.getInquiryById(inquiryId);
            List<ChatMessageEntity> chatMessages = inquiryQueryService.getChatMessages(inquiryId);
            
            InquiryDetail detail = new InquiryDetail();
            detail.setInquiryId(inquiry.getId());
            detail.setCreatedAt(inquiry.getCreatedAt());
            detail.setNotionPageUrl(inquiry.getNotionPageUrl() != null 
                ? URI.create(inquiry.getNotionPageUrl()) 
                : null);
            
            // Requester 이름 설정
            if (inquiry.getRequester() != null) {
                detail.setUserName(inquiry.getRequester().getName());
            }
            
            // 태그 설정
            List<String> tagNames = inquiry.getTags().stream()
                    .map(tag -> tag.getName())
                    .collect(Collectors.toList());
            detail.setTags(tagNames);
            
            // 채팅 히스토리 설정
            List<ChatMessage> chatHistory = chatMessages.stream()
                    .map(this::toChatMessage)
                    .collect(Collectors.toList());
            detail.setChatHistory(chatHistory);
            
            return ResponseEntity.ok(detail);
            
        } catch (AppException e) {
            throw e;
        } catch (Exception e) {
            throw new AppException("INQUIRY_DETAIL_FAILED", "문의 상세 조회에 실패했습니다: " + e.getMessage());
        }
    }

    /**
     * InquiryEntity를 InquirySummary로 변환
     */
    private InquirySummary toInquirySummary(InquiryEntity entity) {
        InquirySummary summary = new InquirySummary();
        summary.setInquiryId(entity.getId());
        summary.setCreatedAt(entity.getCreatedAt());
        
        if (entity.getRequester() != null) {
            summary.setUserName(entity.getRequester().getName());
        }
        
        List<String> tagNames = entity.getTags().stream()
                .map(tag -> tag.getName())
                .collect(Collectors.toList());
        summary.setTags(tagNames);
        
        return summary;
    }

    /**
     * ChatMessageEntity를 ChatMessage로 변환
     */
    private ChatMessage toChatMessage(ChatMessageEntity entity) {
        ChatMessage message = new ChatMessage();
        // ChatSenderType (user, agent, system)을 SenderEnum으로 변환
        String senderValue = entity.getSender().toString().toLowerCase();
        message.setSender(ChatMessage.SenderEnum.fromValue(senderValue));
        message.setMessage(entity.getMessage());
        message.setTimestamp(entity.getTimestamp());
        return message;
    }
}
