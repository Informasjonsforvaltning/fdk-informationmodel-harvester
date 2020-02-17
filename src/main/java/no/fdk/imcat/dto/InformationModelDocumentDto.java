package no.fdk.imcat.dto;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;
import lombok.NoArgsConstructor;
import no.dcat.shared.Publisher;

import java.util.Map;

@Data
@NoArgsConstructor
public class InformationModelDocumentDto {
    private String id;
    private Publisher publisher;
    private String harvestSourceUri;
    private HarvestDto harvest;
    private Map<String, String> title;
    private String schema;

    private JsonNode rootObject;
    private JsonNode objectTypes;
    private JsonNode codeTypes;
    private JsonNode dataTypes;
    private JsonNode simpleTypes;

    private JsonNode informationModelDescription;
}
