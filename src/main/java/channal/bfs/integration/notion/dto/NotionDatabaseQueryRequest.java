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
public class NotionDatabaseQueryRequest {
    @JsonProperty("filter")
    private Map<String, Object> filter;

    @JsonProperty("sorts")
    private List<Map<String, Object>> sorts;

    @JsonProperty("start_cursor")
    private String startCursor;

    @JsonProperty("page_size")
    private Integer pageSize;
}
