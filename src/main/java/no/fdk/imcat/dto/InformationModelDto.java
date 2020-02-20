package no.fdk.imcat.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.NoArgsConstructor;
import no.dcat.shared.Publisher;

import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class InformationModelDto {
    private String id;
    private Publisher publisher;
    private String harvestSourceUri;
    private HarvestDto harvest;
    private Map<String, String> title;
    private String schema;

    private List<Node> objectTypes;
    private List<Node> codeTypes;
    private List<Node> dataTypes;
    private List<Node> simpleTypes;

    private ModelDescriptionDto informationModelDescription;
}
