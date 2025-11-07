package channal.bfs.integration.notion.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class NotionDatabaseQueryResponse {
    @JsonProperty("object")
    private String object;

    @JsonProperty("results")
    private List<NotionPage> results;

    @JsonProperty("next_cursor")
    private String nextCursor;

    @JsonProperty("has_more")
    private Boolean hasMore;
}
