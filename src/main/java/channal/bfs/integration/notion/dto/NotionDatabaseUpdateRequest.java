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
public class NotionDatabaseUpdateRequest {
    @JsonProperty("title")
    private Object title; // 데이터베이스 제목

    @JsonProperty("properties")
    private Map<String, Object> properties; // 데이터베이스 속성 정의

    @JsonProperty("description")
    private Object description; // 데이터베이스 설명 (선택)
}
