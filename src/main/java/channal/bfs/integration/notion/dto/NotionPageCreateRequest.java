package channal.bfs.integration.notion.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class NotionPageCreateRequest {
    @JsonProperty("parent")
    private Map<String, Object> parent;

    @JsonProperty("properties")
    private Map<String, Object> properties;

    @JsonProperty("children")
    private List<Map<String, Object>> children;

    @JsonProperty("icon")
    private Map<String, Object> icon;

    @JsonProperty("cover")
    private Map<String, Object> cover;
}
