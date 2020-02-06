package no.fdk.imcat.model;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import no.dcat.shared.HarvestMetadata;
import no.dcat.shared.Publisher;
import no.fdk.imcat.dto.InformationModelDocument;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.hateoas.core.Relation;

import java.util.Map;

@Data
@Relation(value = "informationmodelenhanced", collectionRelation = "informationmodelenhanceds")
@Document(indexName = "imcatenh", type = "informationmodelenhanced")
public class InformationModelEnhanced {
    @ApiModelProperty("The id given by the harvest system")
    private String id;

    @ApiModelProperty("The publisher of the information model")
    private Publisher publisher;

    @ApiModelProperty("The source where the record was harvested from")
    private String harvestSourceUri;

    @ApiModelProperty("Information about when the model was first and last harvested by the system")
    private HarvestMetadata harvest;

    @ApiModelProperty("The title of the information model")
    private Map<String, String> title;

    @ApiModelProperty("The model itself, expressed in JSON-SCHEMA. InformationModelEnhanced's schema is always null, use the model field instead")
    private String schema;

    @ApiModelProperty("The model itself.")
    private InformationModelDocument document;

}
