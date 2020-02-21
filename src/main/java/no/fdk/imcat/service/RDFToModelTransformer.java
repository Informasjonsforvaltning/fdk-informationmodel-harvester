package no.fdk.imcat.service;

import lombok.RequiredArgsConstructor;
import no.dcat.shared.Publisher;
import no.fdk.imcat.dto.*;
import no.fdk.imcat.model.InformationModelDocument;
import no.fdk.imcat.model.InformationModelEnhanced;
import no.fdk.imcat.utils.DCATNOINFO;
import org.apache.jena.rdf.model.*;
import org.apache.jena.sparql.vocabulary.FOAF;
import org.apache.jena.vocabulary.DCAT;
import org.apache.jena.vocabulary.DCTerms;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.VCARD4;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import javax.annotation.PostConstruct;
import java.io.StringReader;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static java.util.Collections.singletonList;

@Service
@RequiredArgsConstructor
public class RDFToModelTransformer {
    private static final Logger logger = LoggerFactory.getLogger(RDFToModelTransformer.class);
    private final InformationmodelEnhancedRepository informationmodelEnhancedRepository;
    private final OrganizationCatalogueClient organizationCatalogueClient;
    private final ReferenceDataClient referenceDataClient;
    private RestTemplate restTemplate = new RestTemplate();
    private HttpHeaders defaultHeaders = new HttpHeaders();
    private List<Node> nodeList;

    private static Map<String, String> extractLanguageLiteralFromResource(Resource resource, Property property) {
        Map<String, String> map = new HashMap<>();

        StmtIterator iterator = resource.listProperties(property);

        while (iterator.hasNext()) {
            Statement statement = iterator.next();
            String language = statement.getLanguage();
            if (language == null || language.isEmpty()) {
                language = "nb";
            }
            if (statement.getString() != null && !statement.getString().isEmpty()) {
                map.put(language, statement.getString());
            }
        }

        if (map.keySet().size() > 0) {
            return map;
        }

        return null;
    }

    @PostConstruct
    private void setDefaultHeaders() {
        this.defaultHeaders.setAccept(singletonList(MediaType.valueOf("text/turtle")));
    }

    private String harvestInformationModels(String harvestSourceUrl) {

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    harvestSourceUrl,
                    HttpMethod.GET,
                    new HttpEntity<>(this.defaultHeaders),
                    String.class);

            if (response.hasBody()) {
                return response.getBody();
            } else {
                logger.error("No response body received from {}", harvestSourceUrl);
                return null;
            }

        } catch (HttpClientErrorException e) {
            logger.error("Error fetching harvest urls from GET / {}. {} ({})",
                    harvestSourceUrl, e.getStatusText(), e.getStatusCode().value());
        } catch (RestClientException e) {
            logger.error(e.getMessage());
        }

        return null;
    }

    void getInformationModelsFromHarvestSource(HarvestDataSource harvestDataSource) {
        String informationModelSource = harvestInformationModels(harvestDataSource.getUrl());

        if (informationModelSource == null) {
            return;
        }

        try {
            final Model model = ModelFactory.createDefaultModel();
            model.read(new StringReader(informationModelSource), null, "TURTLE");//Base and lang is just untested dummy values
            getInformationModelsFromRDF(model, harvestDataSource.getUrl());
        } catch (Exception e) {
            logger.info("Got error while reading model: " + e.getMessage());
        }
    }

    private Publisher extractPublisher(Resource resource) {
        try {
            Statement propertyStmt = resource.getProperty(DCTerms.publisher);
            if (propertyStmt != null) {
                Resource publisherResource = resource.getModel().getResource(propertyStmt.getObject().asResource().getURI());
                String orgNr = publisherResource.getProperty(DCTerms.identifier).getLiteral().getString();
                String name = publisherResource.getProperty(FOAF.name).getLiteral().getString();
                return new Publisher(orgNr, publisherResource.getURI(), name);
            }
        } catch (Exception e) {
            logger.warn("Error when extracting property {} from resource {}", DCTerms.publisher, resource.getURI(), e);
        }

        return null;
    }

    private String getPropertyType(Resource r) {
        return r.hasProperty(DCATNOINFO.propertyType) ? r.getProperty(DCATNOINFO.propertyType).getLiteral().getString() : null;
    }

    private String getPropertyLiteralValue(Resource r, Property p) {
        return r.hasProperty(p) ? r.getProperty(p).getLiteral().getString() : null;
    }

    private String getSubclassName(Resource r) {
        return r.getPropertyResourceValue(DCATNOINFO.isSubclassOf).getProperty(DCATNOINFO.name).getLiteral().getString();
    }

    private Map<String, String> getRestrictions(Resource r) {
        Predicate<Statement> isRestriction = p -> p.getPredicate().getURI().contains("XMLSchema");
        return r.listProperties().filterKeep(isRestriction).toList().stream()
                .collect(Collectors.toMap(s -> s.getPredicate().getLocalName(), s -> s.getLiteral().getString()));
    }

    private void parseInformationModel(Resource r) {
        // skip if we have already parsed this type
        if (nodeList.stream().anyMatch(o -> o.getLocalUri().equals(r.getURI()))) {
            return;
        }

        String elementType = getPropertyLiteralValue(r, DCATNOINFO.modelElementType);

        if (elementType == null) {
            return;
        }

        Node node = new Node();
        List<Prop> attributes = new ArrayList<>();
        List<Prop> roles = new ArrayList<>();
        List<Prop> otherProps = new ArrayList<>();

        node.setLocalUri(r.getURI());
        node.setModelElementType(elementType);

        node.setName(extractLanguageLiteralFromResource(r, DCATNOINFO.name));

        if (r.hasProperty(DCATNOINFO.typeDefinitionReference)) {
            node.setTypeDefinitionReference(getPropertyLiteralValue(r, DCATNOINFO.typeDefinitionReference));
        }

        if (r.hasProperty(DCATNOINFO.isDescribedBy)) {
            node.setIsDescribedByUri(getPropertyLiteralValue(r, DCATNOINFO.isDescribedBy));
        }

        if (r.hasProperty(DCATNOINFO.isSubclassOf)) {
            node.setIsSubclassOf(getSubclassName(r));
        }

        Property propSelector = DCATNOINFO.hasProperty;
        if (elementType.equals("kodeliste")) {
            propSelector = DCATNOINFO.containsCodename;
        }

        StmtIterator properties = r.listProperties(propSelector);
        while (properties.hasNext()) {
            // Fill out the properties' metadata
            Resource child = properties.nextStatement().getResource();

            Prop prop = new Prop();
            prop.setName(extractLanguageLiteralFromResource(child, DCATNOINFO.name));
            prop.setRestrictions(getRestrictions(child));
            String propType = getPropertyType(child);
            if (propType != null) {
                switch (propType) {
                    case "rolle":
                        roles.add(prop);
                        break;
                    case "attributt":
                        attributes.add(prop);
                        break;
                    default:
                        otherProps.add(prop);
                        break;
                }
            }

            // continue parsing recursively
            if (child.hasProperty(DCATNOINFO.type)) {
                parseInformationModel(child.getPropertyResourceValue(DCATNOINFO.type));
            }

        }

        node.setAttributes(attributes);
        node.setOtherProps(otherProps);
        node.setRoles(roles);
        nodeList.add(node);
    }

    private List<String> extractLosThemeUris(Resource r) {
        return r.listProperties(DCAT.theme).mapWith((theme) -> theme.getResource().getURI()).toList();
    }

    private String extractContactPhone(Resource r) {
        return r.hasProperty(VCARD4.hasTelephone) ? r.getProperty(VCARD4.hasTelephone).getResource().getURI().replace("tel:", "") : null;
    }

    private String extractContactEmail(Resource r) {
        return r.hasProperty(VCARD4.hasEmail) ? r.getProperty(VCARD4.hasEmail).getResource().getURI().replace("mailto:", "") : null;
    }

    private List<InformationModelEnhanced> convertRDFRecordsToModels(List<Statement> records) {
        List<InformationModelEnhanced> modelsList = new ArrayList<>();

        for (Statement record : records) {
            Resource informationModelResource = record.getProperty(FOAF.primaryTopic).getResource();

            InformationModelEnhanced informationModel = new InformationModelEnhanced();
            informationModel.setUniqueUri(informationModelResource.getURI());
            informationModel.setTitle(extractLanguageLiteralFromResource(informationModelResource, DCTerms.title));

            HarvestDto metadata = new HarvestDto();
            if (informationModelResource.hasProperty(DCTerms.modified)) {
                metadata.setLastChanged(LocalDateTime.parse(
                        informationModelResource.getProperty(DCTerms.modified).getLiteral().getString(),
                        DateTimeFormatter.ISO_OFFSET_DATE_TIME).toLocalDate());
            }
            informationModel.setHarvest(metadata);

            InformationModelDocument document = new InformationModelDocument();
            List<String> themes = extractLosThemeUris(informationModelResource);
            document.setThemes(!themes.isEmpty() ? referenceDataClient.getLosThemesByUris(themes) : null);

            Resource contactPoint = informationModelResource.getProperty(DCAT.contactPoint).getResource();
            if (contactPoint != null) {
                document.setContactPoint(new ContactPointDto(
                        extractLanguageLiteralFromResource(contactPoint, VCARD4.fn),
                        extractContactEmail(contactPoint),
                        extractContactPhone(contactPoint)
                ));
            }
            StmtIterator modelElements = informationModelResource.listProperties(DCATNOINFO.containsModelElement);

            nodeList = new ArrayList<>();
            while (modelElements.hasNext()) {
                parseInformationModel(modelElements.nextStatement().getResource());
            }

            document.addAllTypes(nodeList);
            informationModel.setDocument(document);
            modelsList.add(informationModel);
        }

        return modelsList;
    }

    private void getInformationModelsFromRDF(Model model, String harvestSourceUri) {
        try {
            Resource catalogResource = model.listResourcesWithProperty(RDF.type, DCAT.Catalog).toList().get(0);
            List<Statement> catalogRecords = catalogResource.listProperties(DCAT.record).toList();

            Publisher publisher = extractPublisher(catalogResource);

            convertRDFRecordsToModels(catalogRecords).forEach(record -> {
                record.setId(UUID.randomUUID().toString());
                record.setPublisher(publisher != null ? Publisher.from(organizationCatalogueClient.getPublisher(publisher.getId())) : null);
                record.setHarvestSourceUri(harvestSourceUri);

                Optional<InformationModelEnhanced> existing = informationmodelEnhancedRepository.getByUniqueUri(record.getUniqueUri());

                HarvestDto harvestTime = new HarvestDto();
                harvestTime.setLastChanged(record.getHarvest().getLastChanged());
                harvestTime.setLastHarvested(LocalDate.now());
                harvestTime.setFirstHarvested(LocalDate.now());

                existing.ifPresent(present -> {
                    harvestTime.setFirstHarvested(present.getHarvest().getFirstHarvested());
                    record.setId(present.getId());
                });

                record.setHarvest(harvestTime);
                informationmodelEnhancedRepository.save(record);
            });

        } catch (Exception e) {
            logger.error("Got exception for Elasticsearch: {}", harvestSourceUri, e);
        }
    }
}
