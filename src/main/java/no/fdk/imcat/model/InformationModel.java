package no.fdk.imcat.model;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import no.dcat.shared.Publisher;
import no.fdk.imcat.dto.Harvest;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.hateoas.core.Relation;

@Data
@Relation(value = "informationmodel", collectionRelation = "informationmodels")
@Document(indexName = "imcat", type = "informationmodel")
public class InformationModel {
    @ApiModelProperty("The id given by the harvest system")
    private String id;

    @ApiModelProperty("The publisher of the information model")
    private Publisher publisher;

    @ApiModelProperty("The source where the record was harvested from")
    private String harvestSourceUri;

    @ApiModelProperty("Information about when the model was first and last harvested by the system")
    private Harvest harvest;

    @ApiModelProperty("The title of the information model")
    private String title;

    @ApiModelProperty("The model itself, expressed in JSON-SCHEMA")
    private String schema;
}
