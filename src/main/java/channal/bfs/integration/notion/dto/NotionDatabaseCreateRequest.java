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
public class NotionDatabaseCreateRequest {
    @JsonProperty("parent")
    private Map<String, Object> parent;

    @JsonProperty("title")
    private Object title; //상위페이지

    @JsonProperty("properties")
    private Map<String, Object> properties; // 데이터베이스 속성 정의
}
