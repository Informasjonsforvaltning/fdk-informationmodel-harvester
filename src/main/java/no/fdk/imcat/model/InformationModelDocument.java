package no.fdk.imcat.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import no.fdk.imcat.dto.Node;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@ToString(includeFieldNames = false)
public class InformationModelDocument {
    List<Node> types = new ArrayList<>();

    private Map<String, String> description;
    private Map<String, String> name;
    private Map<String, String> title;
    private String version;
    private String versionInfo;
    private String language;
    private String status;
    private String contactPoint;
    private List<String> keywords;
    private List<String> themes;
    private String landingPage;
    //    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    //    @JsonDeserialize(using = LocalDateDeserializer.class)
    //    @JsonSerialize(using = LocalDateSerializer.class)
    private LocalDate issued;
    //
    //    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    //    @JsonDeserialize(using = LocalDateDeserializer.class)
    //    @JsonSerialize(using = LocalDateSerializer.class)
    private LocalDate modified;


    public void addAllTypes(List<Node> other) {
        types.addAll(other);
    }
}
