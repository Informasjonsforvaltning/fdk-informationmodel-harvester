package no.fdk.imcat.service;

import com.google.gson.Gson;
import no.fdk.imcat.model.InformationModel;
import org.springframework.data.elasticsearch.annotations.Query;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RepositoryRestResource(itemResourceRel = "informationmodel", collectionResourceRel = "informationmodels", exported = false)
public interface InformationmodelRepository
    extends ElasticsearchRepository<InformationModel, String> {

    @Query("{\"term\":{\"harvestSourceUri\":\"?0\"}}")
    Optional<InformationModel> getByHarvestSourceUri(String harvestSourceUri);

    default void deleteByIds(List<String> ids) {
        for (String id : ids) {
            deleteById(id);
        }
    }

    @Query("{\"bool\":{\"must_not\":[{\"ids\":{\"values\":?0}}]}}")
    List<InformationModel> getAllNotHarvested(String idsListString);

    default List<String> getAllIdsNotHarvested(List<String> ids) {
        String idsString = new Gson().toJson(ids);

        List<InformationModel> notHarvestedModels = getAllNotHarvested(idsString);

        return notHarvestedModels.stream().map(InformationModel::getId).collect(Collectors.toList());
    }
}
