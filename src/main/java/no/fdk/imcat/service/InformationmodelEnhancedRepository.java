package no.fdk.imcat.service;

import com.google.gson.Gson;
import no.fdk.imcat.model.InformationModelEnhanced;
import org.springframework.data.elasticsearch.annotations.Query;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RepositoryRestResource(itemResourceRel = "informationmodelenhanced", collectionResourceRel = "informationmodelenhanceds", exported = false)
public interface InformationmodelEnhancedRepository extends ElasticsearchRepository<InformationModelEnhanced, String> {

    @Query("{\"bool\":{\"must\":[{\"query_string\":{\"default_field\":\"uniqueUri\",\"query\":\"\\\"?0\\\"\"}}]}}")
    Optional<InformationModelEnhanced> getByUniqueUri(String uniqueUri);

    default void deleteByIds(List<String> ids) {
        for (String id : ids) {
            deleteById(id);
        }
    }

    @Query("{\"bool\":{\"must_not\":[{\"ids\":{\"values\":?0}}]}}")
    List<InformationModelEnhanced> getAllNotHarvested(String idsListString);

    default List<String> getAllIdsNotHarvested(List<String> ids) {
        String idsString = new Gson().toJson(ids);

        List<InformationModelEnhanced> notHarvestedModels = getAllNotHarvested(idsString);

        return notHarvestedModels.stream().map(InformationModelEnhanced::getId).collect(Collectors.toList());
    }
}