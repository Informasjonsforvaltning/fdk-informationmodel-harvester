package no.fdk.imcat.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import no.dcat.client.elasticsearch5.Elasticsearch5Client;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.elasticsearch.core.ElasticsearchTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.IOException;

@Service
public class ElasticsearchService {
    private static Logger logger = LoggerFactory.getLogger(ElasticsearchService.class);

    private ElasticsearchTemplate elasticsearchTemplate;
    private ObjectMapper mapper;

    @Autowired
    ElasticsearchService(ElasticsearchTemplate elasticsearchTemplate, ObjectMapper mapper) {
        this.elasticsearchTemplate = elasticsearchTemplate;
        this.mapper = mapper;
    }

    @PostConstruct
    void init() {
        logger.debug("ElasticsearchService for Informationmodel-cat init()");
        Elasticsearch5Client elasticClient = new Elasticsearch5Client(elasticsearchTemplate.getClient());
        try {
            elasticClient.registerSetting("imcat", mapper.readTree(new ClassPathResource("imcat.settings.json").getInputStream()).toString());
            elasticClient.registerMapping("imcat", "informationmodel", mapper.readTree(new ClassPathResource("informationmodel.mapping.json").getInputStream()).get("informationmodel").toString());
            elasticClient.initializeAliasAndIndexMapping("imcat");

            elasticClient.registerSetting("imcatenh", mapper.readTree(new ClassPathResource("imcat.settings.json").getInputStream()).toString());
            elasticClient.registerMapping("imcatenh", "informationmodelenhanced", mapper.readTree(new ClassPathResource("informationmodelenhanced.mapping.json").getInputStream()).get("informationmodelenhanced").toString());
            elasticClient.initializeAliasAndIndexMapping("imcatenh");
        } catch (IOException e) {
            throw new RuntimeException("Unable to initialize elasticsearch index for Informationmodel-cat", e);
        }
    }
}
