package no.fdk.imcat.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.List;

@Data
@NoArgsConstructor
@ToString(includeFieldNames = false, callSuper = true)
@EqualsAndHashCode(callSuper = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Node extends BaseType {
    private String isDescribedByUri;
    private String typeDefinitionReference;
    private List<Prop> attributes;
    private List<Prop> roles;
    private List<Prop> otherProps;
    private String localUri;
}
