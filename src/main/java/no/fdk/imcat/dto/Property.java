package no.fdk.imcat.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
@ToString(includeFieldNames = false)
@JsonInclude(JsonInclude.Include.NON_NULL)
@EqualsAndHashCode
public class Property {
    private String identifier;
    private Map<String, String> name;
    private Map<String, String> description;
    private Map<String, String> parameters;
    private Map<String, String> belongsToModule;
    private String isDescribedByUri;
    private PropertyType type;
}
