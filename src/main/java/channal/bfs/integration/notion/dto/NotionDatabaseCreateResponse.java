package channal.bfs.integration.notion.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotionDatabaseCreateResponse {
    @JsonProperty("object")
    private String object;

    @JsonProperty("id")
    private String id;

    @JsonProperty("created_time")
    private String createdAt;

    @JsonProperty("last_edited")
    private String updatedAt;

    @JsonProperty("created_by")
    private Map<String, Object> createdBy;

    @JsonProperty("last_edited_by")
    private Map<String, Object> lastEditedBy;

    @JsonProperty("icon")
    private Map<String, Object> icon;

    @JsonProperty("cover")
    private Map<String, Object> cover;

    @JsonProperty("parent")
    private Map<String, Object> parent;

    @JsonProperty("archived")
    private Boolean archived;

    @JsonProperty("properties")
    private Map<String, Object> properties;

    @JsonProperty("url")
    private String url;

    @JsonProperty("public_url")
    private String publicUrl;

    @JsonProperty("title")
    private Object title; // title은 배열 형태
}
