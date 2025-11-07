package channal.bfs.webhook.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

/**
 * 채널톡 웹훅 이벤트 DTO
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ChannelTalkWebhookEvent {
    private String event;
    private String type;
    private Entity entity;
    private Refers refers;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Entity {
        private String id;
        private String chatKey;
        private String chatId;
        private Log log;

        @Data
        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class Log {
            private String action;
        }
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Refers {
        private UserChat userChat;
        private User user;

        @Data
        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class UserChat {
            private String id;
            private String channelId;
            private String state;
            private String userId;
            private String name;
            private List<String> tags;
            private String assigneeId;
            private List<String> managerIds;
            private String customerCity;
        }

        @Data
        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class User {
            private String id;
            private String name;
            private String email;
            private Profile profile;
            private String city;
            private String country;

            @Data
            @JsonIgnoreProperties(ignoreUnknown = true)
            public static class Profile {
                private String email;
                private String referrer;
                private String lastReferrer;
            }
        }
    }

    /**
     * 상담 종료 이벤트인지 확인
     */
    public boolean isChatCloseEvent() {
        return "push".equals(event) &&
               entity != null &&
               entity.getLog() != null &&
               "close".equals(entity.getLog().getAction());
    }

    /**
     * 채팅방 ID 추출
     */
    public String getChatId() {
        if (refers != null && refers.getUserChat() != null) {
            return refers.getUserChat().getId();
        }
        if (entity != null) {
            return entity.getChatId();
        }
        return null;
    }

    /**
     * 고객 이메일 추출
     */
    public String getCustomerEmail() {
        if (refers != null && refers.getUser() != null) {
            // profile.email 우선
            if (refers.getUser().getProfile() != null &&
                refers.getUser().getProfile().getEmail() != null) {
                return refers.getUser().getProfile().getEmail();
            }
            // user.email 대체
            return refers.getUser().getEmail();
        }
        return null;
    }

    /**
     * 고객 도시 추출
     */
    public String getCustomerCity() {
        if (refers != null && refers.getUser() != null) {
            return refers.getUser().getCity();
        }
        return null;
    }

    /**
     * 태그 리스트 추출
     */
    public List<String> getTags() {
        if (refers != null && refers.getUserChat() != null) {
            return refers.getUserChat().getTags();
        }
        return null;
    }
}
