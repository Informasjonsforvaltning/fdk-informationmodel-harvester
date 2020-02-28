package no.fdk.imcat.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.springframework.data.elasticsearch.annotations.Field;

import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@ToString(includeFieldNames = false)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Node {
    private String identifier;
    private String codeListReference;
    private Map<String, String> name;
    private List<Property> roles;
    private List<Property> attributes;
    private List<Property> properties;
    private String modelElementType;
    private String isDescribedByUri;
    private String typeDefinitionReference;
    private String isSubclassOf;
}
