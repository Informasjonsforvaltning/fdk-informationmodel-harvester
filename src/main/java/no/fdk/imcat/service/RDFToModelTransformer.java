package no.fdk.imcat.service;

import no.dcat.shared.HarvestMetadata;
import no.dcat.shared.HarvestMetadataUtil;
import no.dcat.shared.Publisher;
import no.fdk.imcat.dto.*;
import no.fdk.imcat.model.DCATNOINFO;
import no.fdk.imcat.model.InformationModelEnhanced;
import org.apache.jena.rdf.model.*;
import org.apache.jena.sparql.vocabulary.FOAF;
import org.apache.jena.vocabulary.DCAT;
import org.apache.jena.vocabulary.DCTerms;
import org.apache.jena.vocabulary.RDF;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.io.StringReader;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;

@Service
public class RDFToModelTransformer {

    private static final Logger logger = LoggerFactory.getLogger(RDFToModelTransformer.class);
    private final RestTemplate restTemplate;
    private HttpHeaders defaultHeaders;

    private List<Node> nodeList;

    public RDFToModelTransformer() {
        this.restTemplate = new RestTemplate();

        this.defaultHeaders = new HttpHeaders();
        this.defaultHeaders.setAccept(singletonList(MediaType.valueOf("text/turtle")));
    }

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

    List<InformationModelEnhanced> getInformationModelsFromHarvestSource(HarvestDataSource harvestDataSource) {
        String informationModelSource = harvestInformationModels(harvestDataSource.getUrl());

        if (informationModelSource == null) {
            return emptyList();
        }

        try {
            final Model model = ModelFactory.createDefaultModel();
            model.read(new StringReader(informationModelSource), null, "TURTLE");//Base and lang is just untested dummy values
            return getInformationModelsFromRDF(model, harvestDataSource.getUrl());
        } catch (Exception e) {
            logger.info("Got error while reading model: " + e.getMessage());
            return Collections.emptyList();
        }
    }

    private Publisher extractPublisher(Resource resource) {
        try {
            Statement propertyStmt = resource.getProperty(DCTerms.publisher);
            if (propertyStmt != null) {
                Resource publisherResource = resource.getModel().getResource(propertyStmt.getObject().asResource().getURI());
                String orgNr = publisherResource.getProperty(DCTerms.identifier).getLiteral().getString();
                Map<String, String> name = extractLanguageLiteralFromResource(publisherResource, FOAF.name);
                return new Publisher(orgNr, publisherResource.getURI(), name);
            }
        } catch (Exception e) {
            logger.warn("Error when extracting property {} from resource {}", DCTerms.publisher, resource.getURI(), e);
        }

        return null;
    }

//    private List<SimpleType> extractSimpleTypes(List<Statement> attributes) {
//        return attributes.stream().map(attribute -> {
//            SimpleType simpleType = new SimpleType();
//
//            Resource attributeType = attribute.getResource().getProperty(DCATNOINFO.type).getResource();
//
//            Predicate<Statement> isRestriction = p -> p.getPredicate().getURI().contains("XMLSchema");
//            simpleType.setRestrictions(attributeType
//                    .listProperties()
//                    .filterKeep(isRestriction).toList().stream()
//                    .collect(Collectors.toMap(s -> s.getPredicate().getLocalName(), s -> s.getLiteral().getString())));
//
//            simpleType.setModelElementType(attributeType.getProperty(DCATNOINFO.modelElementType).getLiteral().getString());
//
//            simpleType.setName(extractLanguageLiteralFromResource(attributeType, DCATNOINFO.name));
//
//            simpleType.setIsDescribedByUri(attributeType.hasProperty(DCATNOINFO.isDescribedBy) ? attributeType.getProperty(DCATNOINFO.isDescribedBy).getLiteral().getString() : null);
//            simpleType.setTypeDefinitionReference(attributeType.getProperty(DCATNOINFO.typeDefinitionReference).getLiteral().getString());
//
//            return simpleType;
//        }).collect(Collectors.toList());
//    }
//
//    private boolean isObjectType(Statement s) {
//        return s.getResource().getProperty(DCATNOINFO.modelElementType).getLiteral().getString().equals("objekttype");
//    }
//
//    private boolean isRootType(Statement s) {
//        return s.getResource().getProperty(DCATNOINFO.modelElementType).getLiteral().getString().equals("rotObjektstype");
//    }

    private String getPropertyType(Resource r) {
        return r.hasProperty(DCATNOINFO.propertyType) ? r.getProperty(DCATNOINFO.propertyType).getLiteral().getString() : null;
    }

    private String getPropertyLiteralValue(Resource r, Property p) {
        return r.hasProperty(p) ? r.getProperty(p).getLiteral().getString() : null;
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

        // TODO: remove me
        System.out.println(r.toString());

        Node node = new Node();
        List<Prop> attributes = new ArrayList<>();
        List<Prop> roles = new ArrayList<>();
        List<Prop> otherProps = new ArrayList<>();

        node.setLocalUri(r.getURI());

        String elementType = getPropertyLiteralValue(r, DCATNOINFO.modelElementType);
        node.setModelElementType(elementType);

        node.setName(extractLanguageLiteralFromResource(r, DCATNOINFO.name));

        if (r.hasProperty(DCATNOINFO.typeDefinitionReference)) {
            node.setTypeDefinitionReference(getPropertyLiteralValue(r, DCATNOINFO.typeDefinitionReference));
        }

        if (r.hasProperty(DCATNOINFO.isDescribedBy)) {
            node.setIsDescribedByUri(getPropertyLiteralValue(r, DCATNOINFO.isDescribedBy));
        }

        Property propSelector = DCATNOINFO.hasProperty;
        if (elementType != null && elementType.equals("kodeliste")) {
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

    private List<InformationModelEnhanced> convertRecordsToModels(List<Statement> records) {
        List<InformationModelEnhanced> modelsList = new ArrayList<>();

        for (Statement record : records) {
            Resource informationModelResource = record.getProperty(FOAF.primaryTopic).getResource();

            InformationModelEnhanced informationModel = new InformationModelEnhanced();
            informationModel.setId(UUID.randomUUID().toString()); // TODO: create or update in ES5
            informationModel.setTitle(extractLanguageLiteralFromResource(informationModelResource, DCTerms.title));

            InformationModelDocument document = new InformationModelDocument();

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

    private List<InformationModelEnhanced> getInformationModelsFromRDF(Model model, String harvestSourceUri) {

        List<InformationModelEnhanced> modelsList = new ArrayList<>();

        try {
            Resource catalogResource = model.listResourcesWithProperty(RDF.type, DCAT.Catalog).toList().get(0);
            List<Statement> catalogRecords = catalogResource.listProperties(DCAT.record).toList();

            Publisher publisher = extractPublisher(catalogResource);
            HarvestMetadata harvestDate = HarvestMetadataUtil.createOrUpdate(null, new Date(), false); // <-------- TODO: check for already existing

            modelsList.addAll(convertRecordsToModels(catalogRecords).stream().map(record -> {
                record.setPublisher(publisher);
                record.setHarvest(harvestDate);
                record.setHarvestSourceUri(harvestSourceUri);
                return record;
            }).collect(Collectors.toList()));

            System.out.println(modelsList);

        } catch (Exception e) {
            logger.error("Got exception for Elasticsearch: {}", harvestSourceUri, e);
        }

        return modelsList;
    }
}
