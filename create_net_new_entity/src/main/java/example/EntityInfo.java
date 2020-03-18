package example;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class EntityInfo {

    private String attributeName;

    private String purpose;
    private String dataType;
    private String description;

    private boolean relation = false;
    private String relationEntityName;
    private String relationColumnName;
    private String jsonPath;
    private String relationDescription;
}