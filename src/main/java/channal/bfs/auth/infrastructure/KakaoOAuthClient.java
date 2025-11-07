package channal.bfs.auth.infrastructure;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Component
public class KakaoOAuthClient {

    private final String clientId;
    private final String redirectUri;
    private final String tokenUrl;
    private final String userInfoUrl;
    private final RestTemplate restTemplate;

    public KakaoOAuthClient(
            @Value("${kakao.oauth.client-id}") String clientId,
            @Value("${kakao.oauth.redirect-uri}") String redirectUri,
            @Value("${kakao.oauth.token-url}") String tokenUrl,
            @Value("${kakao.oauth.user-info-url}") String userInfoUrl) {
        this.clientId = clientId;
        this.redirectUri = redirectUri;
        this.tokenUrl = tokenUrl;
        this.userInfoUrl = userInfoUrl;
        this.restTemplate = new RestTemplate();
    }

    public String getAuthorizationUrl(String state) {
        return "https://kauth.kakao.com/oauth/authorize" +
                "?client_id=" + clientId +
                "&redirect_uri=" + redirectUri +
                "&response_type=code" +
                (state != null ? "&state=" + state : "");
    }

    public KakaoTokenResponse getAccessToken(String code) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("grant_type", "authorization_code");
        params.add("client_id", clientId);
        params.add("redirect_uri", redirectUri);
        params.add("code", code);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);
        ResponseEntity<KakaoTokenResponse> response = restTemplate.exchange(
                tokenUrl, HttpMethod.POST, request, KakaoTokenResponse.class);

        return response.getBody();
    }

    public KakaoUserInfo getUserInfo(String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        HttpEntity<String> request = new HttpEntity<>(headers);
        ResponseEntity<KakaoUserInfo> response = restTemplate.exchange(
                userInfoUrl, HttpMethod.GET, request, KakaoUserInfo.class);

        return response.getBody();
    }

    @Data
    public static class KakaoTokenResponse {
        @JsonProperty("access_token")
        private String accessToken;
        @JsonProperty("token_type")
        private String tokenType;
        @JsonProperty("refresh_token")
        private String refreshToken;
        @JsonProperty("expires_in")
        private Integer expiresIn;
        @JsonProperty("refresh_token_expires_in")
        private Integer refreshTokenExpiresIn;
        private String scope;
    }

    @Data
    public static class KakaoUserInfo {
        private Long id;
        @JsonProperty("kakao_account")
        private KakaoAccount kakaoAccount;

        @Data
        public static class KakaoAccount {
            private String email;
            private Profile profile;

            @Data
            public static class Profile {
                private String nickname;
            }
        }
    }
}

