package no.fdk.imcat.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
@ToString(includeFieldNames = false)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Property {
    private String identifier;
    private Map<String, String> name;
    private Map<String, String> parameters;
    private String isDescribedByUri;
    private PropertyType type;
}
