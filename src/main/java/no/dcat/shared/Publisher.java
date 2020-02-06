package no.dcat.shared;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.HashMap;
import java.util.Map;

@Data
@NoArgsConstructor
@ToString(includeFieldNames = false)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Publisher {
    private String uri;
    private String identifier;
    private Map<String, String> name;
    private String orgPath;
    private Map<String, String> prefLabel;

    public Publisher(String identifier) {
        this.identifier = identifier;
    }

    public Publisher(String identifier, String uri) {
        this.identifier = identifier;
        this.uri = uri;
    }

    public Publisher(String identifier, String uri, Map<String, String> name) {
        this.identifier = identifier;
        this.uri = uri;
        this.name = name;
    }
}
