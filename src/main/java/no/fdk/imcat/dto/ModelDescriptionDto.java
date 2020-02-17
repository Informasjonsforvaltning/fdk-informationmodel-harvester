package no.fdk.imcat.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
public class ModelDescriptionDto {
    private Map<String, String> name;
    private Map<String, String> description;
    private String identifier;
    private String belongsToModule;

}
