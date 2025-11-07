package channal.bfs.webhook.application;

import channal.bfs.common.domain.ChatSenderType;
import channal.bfs.common.domain.InquiryStatus;
import channal.bfs.inquiry.application.LlmAnalysisService;
import channal.bfs.inquiry.infrastructure.ChatMessageEntity;
import channal.bfs.inquiry.infrastructure.ChatMessageJpaRepository;
import channal.bfs.inquiry.infrastructure.InquiryEntity;
import channal.bfs.inquiry.infrastructure.InquiryJpaRepository;
import channal.bfs.integration.infrastructure.ChannelTalkApiClient;
import channal.bfs.model.ChatMessage;
import channal.bfs.tag.infrastructure.DepartmentEntity;
import channal.bfs.tag.infrastructure.DepartmentJpaRepository;
import channal.bfs.tag.infrastructure.TagEntity;
import channal.bfs.tag.infrastructure.TagJpaRepository;
import channal.bfs.webhook.domain.ChannelTalkWebhookEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 채널톡 웹훅 처리 서비스
 * 상담 종료 시 채팅 메시지를 조회하고, DB에 저장하고, LLM 분석 후 노션에 기록
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ChatWebhookService {

    private final ChannelTalkApiClient channelTalkApiClient;
    private final LlmAnalysisService llmAnalysisService;
    private final InquiryJpaRepository inquiryRepository;
    private final ChatMessageJpaRepository chatMessageRepository;
    private final TagJpaRepository tagRepository;
    private final DepartmentJpaRepository departmentRepository;
    private final NotionWebhookService notionWebhookService;

    @Value("${channel-talk.api.base-url:https://api.channel.io}")
    private String channelTalkBaseUrl;

    /**
     * 웹훅 이벤트 처리 (비동기)
     * @param event 채널톡 웹훅 이벤트
     */
    @Async
    @Transactional
    public void processWebhook(ChannelTalkWebhookEvent event) {
        try {
            log.info("웹훅 처리 시작: event={}, chatId={}", event.getEvent(), event.getChatId());

            // 1. 상담 종료 이벤트 검증
            if (!event.isChatCloseEvent()) {
                log.info("상담 종료 이벤트가 아닙니다. 무시합니다.");
                return;
            }

            String chatId = event.getChatId();
            if (chatId == null || chatId.isBlank()) {
                log.warn("chatId가 없습니다. 웹훅 무시.");
                return;
            }

            // 2. 채널톡 API로 채팅 메시지 조회
            log.info("채팅 메시지 조회 시작: chatId={}", chatId);
            List<ChatMessage> chatMessages = channelTalkApiClient.getChatMessages(chatId);
            log.info("채팅 메시지 조회 완료: {} 개", chatMessages.size());

            if (chatMessages.isEmpty()) {
                log.warn("채팅 메시지가 없습니다. 웹훅 무시.");
                return;
            }

            // 3. LLM 분석
            log.info("LLM 분석 시작");
            LlmAnalysisService.AnalysisResult analysisResult = llmAnalysisService.analyzeChatMessages(chatMessages);
            log.info("LLM 분석 완료: departments={}, summary={}",
                    analysisResult.department(),
                    analysisResult.summary());

            // 4. InquiryEntity 생성
            InquiryEntity inquiry = createInquiryEntity(event, analysisResult);
            inquiry = inquiryRepository.save(inquiry);
            log.info("InquiryEntity 저장 완료: id={}", inquiry.getId());

            // 5. ChatMessageEntity 저장
            saveChatMessages(inquiry, chatMessages);
            log.info("ChatMessageEntity 저장 완료: {} 개", chatMessages.size());

            // 6. 태그 저장
            saveTags(inquiry, event.getTags());
            log.info("태그 저장 완료");

            // 7. 부서 저장
            saveDepartments(inquiry, analysisResult.department());
            log.info("부서 저장 완료");

            // 8. 노션 페이지 생성
            try {
                String notionPageUrl = notionWebhookService.createNotionPage(inquiry, analysisResult);
                inquiry.setNotionPageUrl(notionPageUrl);
                inquiryRepository.save(inquiry);
                log.info("노션 페이지 생성 완료: url={}", notionPageUrl);
            } catch (Exception e) {
                log.error("노션 페이지 생성 실패. 계속 진행합니다.", e);
            }

            log.info("웹훅 처리 완료: inquiryId={}", inquiry.getId());
        } catch (Exception e) {
            log.error("웹훅 처리 중 오류 발생", e);
            throw new RuntimeException("웹훅 처리 실패", e);
        }
    }

    /**
     * InquiryEntity 생성
     */
    private InquiryEntity createInquiryEntity(
            ChannelTalkWebhookEvent event,
            LlmAnalysisService.AnalysisResult analysisResult) {

        InquiryEntity inquiry = new InquiryEntity();

        // 제목: 고객 이메일 + 상담 종료 시간
        String customerEmail = event.getCustomerEmail();
        String title = String.format("상담 - %s",
                customerEmail != null ? customerEmail : "고객");
        inquiry.setTitle(title);

        // 상태: 완료
        inquiry.setStatus(InquiryStatus.DONE);

        // 요약 및 피드백
        inquiry.setSummary(analysisResult.summary());
        inquiry.setKeyFeedback(analysisResult.keyFeedback());

        // 채널톡 URL
        String chatUrl = String.format("%s/desk/%s",
                channelTalkBaseUrl.replace("/api.", "/"),
                event.getChatId());
        inquiry.setChannelTalkUrl(chatUrl);

        // 고객 도시
        inquiry.setCustomerCity(event.getCustomerCity());

        // 타임스탬프
        inquiry.setCreatedAt(OffsetDateTime.now());
        inquiry.setUpdatedAt(OffsetDateTime.now());

        return inquiry;
    }

    /**
     * ChatMessageEntity 저장
     */
    private void saveChatMessages(InquiryEntity inquiry, List<ChatMessage> chatMessages) {
        for (ChatMessage message : chatMessages) {
            ChatMessageEntity entity = new ChatMessageEntity();
            entity.setInquiry(inquiry);
            entity.setSender(convertSender(message.getSender()));
            entity.setMessage(message.getMessage());
            entity.setTimestamp(message.getTimestamp());
            entity.setCreatedAt(OffsetDateTime.now());
            chatMessageRepository.save(entity);
        }
    }

    /**
     * ChatMessage.SenderEnum → ChatSenderType 변환
     */
    private ChatSenderType convertSender(ChatMessage.SenderEnum sender) {
        if (sender == null) {
            return ChatSenderType.system;
        }
        return switch (sender) {
            case USER -> ChatSenderType.user;
            case AGENT -> ChatSenderType.agent;
            case SYSTEM -> ChatSenderType.system;
        };
    }

    /**
     * 태그 저장
     */
    private void saveTags(InquiryEntity inquiry, List<String> tagNames) {
        if (tagNames == null || tagNames.isEmpty()) {
            return;
        }

        Set<TagEntity> tags = new HashSet<>();
        for (String tagName : tagNames) {
            if (tagName == null || tagName.isBlank()) {
                continue;
            }
            TagEntity tag = tagRepository.findByName(tagName)
                    .orElseGet(() -> {
                        TagEntity newTag = new TagEntity();
                        newTag.setName(tagName);
                        newTag.setCreatedAt(OffsetDateTime.now());
                        return tagRepository.save(newTag);
                    });
            tags.add(tag);
        }
        inquiry.setTags(tags);
    }

    /**
     * 부서 저장
     */
    private void saveDepartments(InquiryEntity inquiry, List<String> departmentNames) {
        if (departmentNames == null || departmentNames.isEmpty()) {
            return;
        }

        Set<DepartmentEntity> departments = new HashSet<>();
        for (String deptName : departmentNames) {
            if (deptName == null || deptName.isBlank()) {
                continue;
            }
            DepartmentEntity dept = departmentRepository.findByName(deptName)
                    .orElseGet(() -> {
                        DepartmentEntity newDept = new DepartmentEntity();
                        newDept.setName(deptName);
                        newDept.setCreatedAt(OffsetDateTime.now());
                        return departmentRepository.save(newDept);
                    });
            departments.add(dept);
        }
        inquiry.setDepartments(departments);
    }
}
