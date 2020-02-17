package no.fdk.imcat.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@ToString(includeFieldNames = false)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Node {
    private String modelElementType;
    private Map<String, String> name;
    private String isDescribedByUri;
    private String typeDefinitionReference;
    private String isSubclassOf;
    private List<Prop> attributes;
    private List<Prop> roles;
    private List<Prop> otherProps;
    private String localUri;
}
