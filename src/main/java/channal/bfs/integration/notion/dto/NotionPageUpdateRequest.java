package channal.bfs.integration.notion.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
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
@JsonInclude(JsonInclude.Include.NON_NULL)
public class NotionPageUpdateRequest {
    @JsonProperty("properties")
    private Map<String, Object> properties;

    @JsonProperty("archived")
    private Boolean archived;

    @JsonProperty("icon")
    private Map<String, Object> icon;

    @JsonProperty("cover")
    private Map<String, Object> cover;
}
