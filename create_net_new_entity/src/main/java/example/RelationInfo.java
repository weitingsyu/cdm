package example;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class RelationInfo {
    private String entityName;
    private String columnName;
    private String jsonPath;
    private String description;

}