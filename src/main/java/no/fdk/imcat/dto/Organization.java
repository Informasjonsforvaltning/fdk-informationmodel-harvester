package no.fdk.imcat.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Organization {
    @JsonProperty("organizationId")
    private String id;
    @JsonProperty("name")
    private String name;
    @JsonProperty("norwegianRegistry")
    private String uri;
    @JsonProperty("orgPath")
    private String orgPath;
    @JsonProperty("prefLabel")
    private Map<String, String> prefLabel;
}