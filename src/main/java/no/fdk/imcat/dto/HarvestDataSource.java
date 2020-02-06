package no.fdk.imcat.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Data;

import java.util.HashMap;
import java.util.Map;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
        "id",
        "dataSourceType",
        "dataType",
        "url",
        "publisherId",
        "description",
        "acceptHeaderValue"
})
public class HarvestDataSource {
    @JsonProperty("id")
    private String id;
    @JsonProperty("dataSourceType")
    private String dataSourceType;
    @JsonProperty("dataType")
    private String dataType;
    @JsonProperty("url")
    private String url;
    @JsonProperty("publisherId")
    private String publisherId;
    @JsonProperty("description")
    private String description;
    @JsonProperty("acceptHeaderValue")
    private String acceptHeaderValue;
    @JsonIgnore
    private Map<String, Object> additionalProperties = new HashMap<>();
}