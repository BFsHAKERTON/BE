package channal.bfs.inquiry.infrastructure;

import channal.bfs.model.ChatMessage;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

/**
 * 채널톡 API 응답을 우리 시스템의 ChatMessage 형식으로 변환하는 유틸리티
 */
public class ChannelTalkMessageConverter {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 채널톡 API 응답의 messages 배열을 ChatMessage 리스트로 변환
     * 
     * @param channelTalkMessages 채널톡 API 응답의 messages 배열 (JsonNode)
     * @return 변환된 ChatMessage 리스트
     */
    public static List<ChatMessage> convertMessages(JsonNode channelTalkMessages) {
        List<ChatMessage> result = new ArrayList<>();
        
        if (channelTalkMessages == null || !channelTalkMessages.isArray()) {
            return result;
        }

        for (JsonNode messageNode : channelTalkMessages) {
            ChatMessage chatMessage = convertSingleMessage(messageNode);
            if (chatMessage != null) {
                result.add(chatMessage);
            }
        }

        return result;
    }

    /**
     * 단일 채널톡 메시지를 ChatMessage로 변환
     */
    private static ChatMessage convertSingleMessage(JsonNode messageNode) {
        if (messageNode == null) {
            return null;
        }

        ChatMessage chatMessage = new ChatMessage();

        // sender 변환: personType → sender
        String personType = messageNode.has("personType") 
            ? messageNode.get("personType").asText() 
            : null;
        chatMessage.setSender(convertPersonTypeToSenderEnum(personType));

        // message 변환: plainText 우선, 없으면 blocks에서 추출
        String plainText = messageNode.has("plainText") 
            ? messageNode.get("plainText").asText() 
            : null;
        
        if (plainText == null || plainText.isBlank()) {
            plainText = extractTextFromBlocks(messageNode.get("blocks"));
        }

        if (plainText == null || plainText.isBlank()) {
            // 메시지 내용이 없으면 스킵
            return null;
        }

        chatMessage.setMessage(plainText);

        // timestamp 변환: createdAt (밀리초) → ISO 8601
        if (messageNode.has("createdAt")) {
            long createdAtMillis = messageNode.get("createdAt").asLong();
            OffsetDateTime timestamp = OffsetDateTime.ofInstant(
                Instant.ofEpochMilli(createdAtMillis), 
                ZoneOffset.UTC
            );
            chatMessage.setTimestamp(timestamp);
        } else {
            chatMessage.setTimestamp(OffsetDateTime.now(ZoneOffset.UTC));
        }

        return chatMessage;
    }

    /**
     * 채널톡 personType을 ChatMessage.SenderEnum으로 변환
     * - "user" → USER
     * - "manager" → AGENT
     * - 기타 → SYSTEM
     */
    private static ChatMessage.SenderEnum convertPersonTypeToSenderEnum(String personType) {
        if (personType == null) {
            return ChatMessage.SenderEnum.SYSTEM;
        }

        return switch (personType.toLowerCase()) {
            case "user" -> ChatMessage.SenderEnum.USER;
            case "manager" -> ChatMessage.SenderEnum.AGENT;
            default -> ChatMessage.SenderEnum.SYSTEM;
        };
    }

    /**
     * blocks 배열에서 텍스트 추출
     */
    private static String extractTextFromBlocks(JsonNode blocksNode) {
        if (blocksNode == null || !blocksNode.isArray()) {
            return null;
        }

        StringBuilder textBuilder = new StringBuilder();

        for (JsonNode block : blocksNode) {
            if (block.has("value")) {
                String value = block.get("value").asText();
                if (value != null && !value.isBlank()) {
                    if (!textBuilder.isEmpty()) {
                        textBuilder.append(" ");
                    }
                    textBuilder.append(value);
                }
            }
            
            // blocks 내부의 blocks도 재귀적으로 처리
            if (block.has("blocks") && block.get("blocks").isArray()) {
                String nestedText = extractTextFromBlocks(block.get("blocks"));
                if (nestedText != null && !nestedText.isBlank()) {
                    if (!textBuilder.isEmpty()) {
                        textBuilder.append(" ");
                    }
                    textBuilder.append(nestedText);
                }
            }
        }

        return textBuilder.isEmpty() ? null : textBuilder.toString();
    }
}

