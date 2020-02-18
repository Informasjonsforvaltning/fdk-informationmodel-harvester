package no.fdk.imcat.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ModelDescriptionDto {
    private Map<String, String> name;
    private Map<String, String> description;
    private List<String> themes;

}
