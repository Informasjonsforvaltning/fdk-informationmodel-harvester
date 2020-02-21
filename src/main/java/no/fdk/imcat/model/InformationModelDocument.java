package no.fdk.imcat.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import no.dcat.shared.LosTheme;
import no.fdk.imcat.dto.ContactPointDto;
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
    private List<String> keywords;
    private List<LosTheme> themes;
    private ContactPointDto contactPoint;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    @JsonDeserialize(using = LocalDateDeserializer.class)
    @JsonSerialize(using = LocalDateSerializer.class)
    private LocalDate issued;

    public void addAllTypes(List<Node> other) {
        types.addAll(other);
    }
}
