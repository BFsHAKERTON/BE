package channal.bfs.integration.notion.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.Map;

@Data
public class NotionOAuthTokenResponse {
    @JsonProperty("access_token")
    private String accessToken;

    @JsonProperty("token_type")
    private String tokenType;

    @JsonProperty("bot_id")
    private String botId;

    @JsonProperty("workspace_name")
    private String workspaceName;

    @JsonProperty("workspace_icon")
    private String workspaceIcon;

    @JsonProperty("workspace_id")
    private String workspaceId;

    @JsonProperty("owner")
    private Map<String, Object> owner;

    @JsonProperty("duplicated_template_id")
    private String duplicatedTemplateId;
}
