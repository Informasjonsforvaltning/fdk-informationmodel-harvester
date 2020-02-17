package no.dcat.shared;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.Map;

@Data
@NoArgsConstructor
@ToString(includeFieldNames = false)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Publisher {
    private String uri;
    private String id;
    private Map<String, String> name;
    private String orgPath;
    private Map<String, String> prefLabel;

    public Publisher(String id) {
        this.id = id;
    }

    public Publisher(String id, String uri) {
        this.id = id;
        this.uri = uri;
    }

    public Publisher(String id, String uri, Map<String, String> name) {
        this.id = id;
        this.uri = uri;
        this.name = name;
    }
}
