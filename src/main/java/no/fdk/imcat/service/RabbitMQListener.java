package no.fdk.imcat.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RabbitMQListener {

    private static final Logger logger = LoggerFactory.getLogger(RabbitMQListener.class);
    private static List<String> ALLOWED_FIELDS = Arrays.asList("publisherId", "dataType");
    private final HarvestAdminClient harvestAdminClient;
    private final ObjectMapper objectMapper;
    private final RDFToModelTransformer rdfToModelTransformer;

    private Map<String, String> whitelistFields(Map<String, String> params) {
        return params.entrySet()
                .stream()
                .filter(x -> ALLOWED_FIELDS.contains(x.getKey()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    private MultiValueMap<String, String> createQueryParams(Map<String, String> fields) {
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        whitelistFields(fields).forEach(params::add);
        return params;
    }

    private void harvest(Map<String, String> fields) {
        this.harvestAdminClient.getDataSources(createQueryParams(fields))
                .forEach(rdfToModelTransformer::getInformationModelsFromHarvestSource);
    }

    @RabbitListener(queues = "#{queue.name}")
    public void receiveInformationModelHarvestTrigger(@Payload JsonNode body, Message message) {
        String routingKey = message.getMessageProperties().getReceivedRoutingKey().split("\\.")[0];
        logger.info(String.format("Received message from key: %s", routingKey));

        // convert from map to multivaluemap for UriComponentBuilder
        Map<String, String> fields = objectMapper.convertValue(
                body,
                new TypeReference<Map<String, String>>() {});

        harvest(fields);
    }
}
