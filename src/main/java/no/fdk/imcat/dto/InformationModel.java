package no.fdk.imcat.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.NoArgsConstructor;
import no.dcat.shared.LosTheme;
import no.dcat.shared.Publisher;
import org.springframework.hateoas.core.Relation;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Data
@NoArgsConstructor
@Relation(collectionRelation = "informationmodels")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class InformationModel {
    private String id;
    private String identifier;
    private Map<String, String> title;
    private Map<String, List<String>> keywords;
    private Publisher publisher;
    private Harvest harvest;
    private String version;
    private LocalDateTime validFromIncluding;
    private LocalDateTime validToIncluding;
    private LocalDateTime issued;
    private LocalDateTime modified;
    private String harvestSourceUri;
    private String landingPage;
    private String category;
    private String status;
    private String schema;
    private Set<String> languages;

    private ContactPoint contactPoint;

    private List<Node> objectTypes;
    private List<Node> codeTypes;
    private List<Node> dataTypes;
    private List<Node> simpleTypes;

    private Map<String, String> name;
    private Map<String, String> description;
    private Map<String, String> modelDescription;
    private List<LosTheme> themes;
}
