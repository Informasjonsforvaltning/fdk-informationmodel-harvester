package no.dcat.shared;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.ToString;
import no.fdk.imcat.dto.OrganizationDto;

import java.util.Map;

@Data
@ToString(includeFieldNames = false)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Publisher {
    private String uri;
    private String id;
    private String name;
    private String orgPath;
    private Map<String, String> prefLabel;

    public Publisher(String orgnr) {
        this.id = orgnr;
    }

    public Publisher(String orgnr, String uri) {
        this.id = orgnr;
        this.uri = uri;
    }

    public Publisher(String id, String uri, String name) {
        this.id = id;
        this.uri = uri;
        this.name = name;
    }

    public Publisher() {
        // Default constructor needed for frameworks
    }

    public static Publisher from(OrganizationDto organizationDto) {
        Publisher p = new Publisher();
        p.setId(organizationDto.getId());
        p.setUri(organizationDto.getUri());
        p.setName(organizationDto.getName());
        p.setOrgPath(organizationDto.getOrgPath());
        p.setPrefLabel(organizationDto.getPrefLabel());
        return p;
    }
}
