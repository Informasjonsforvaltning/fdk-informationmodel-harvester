package no.fdk.imcat.service;

import lombok.RequiredArgsConstructor;
import no.dcat.shared.Publisher;
import no.fdk.imcat.dto.Property;
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

    private static Map<String, String> extractLanguageLiteralFromResource(Resource resource, org.apache.jena.rdf.model.Property property) {
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

        return !map.isEmpty() ? map : null;
    }

    private static Map<String, List<String>> extractLanguageArrayLiteralFromResource(Resource resource, org.apache.jena.rdf.model.Property property) {
        return resource.listProperties(property).toList().stream()
                .collect(Collectors.groupingBy(Statement::getLanguage, Collectors.mapping(Statement::getString, Collectors.toList())));
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
            if (resource.hasProperty(DCTerms.publisher)) {
                Statement publisher = resource.getProperty(DCTerms.publisher);
                String orgNr = publisher.getProperty(DCTerms.identifier).getLiteral().getString();
                String name = publisher.getProperty(FOAF.name).getLiteral().getString();
                return new Publisher(orgNr, publisher.getResource().getURI(), name);
            }
        } catch (Exception e) {
            logger.warn("Error when extracting property {} from resource {}", DCTerms.publisher, resource.getURI(), e);
        }

        return null;
    }

    private String getPropertyType(Resource r) {
        return r.hasProperty(DCATNOINFO.propertyType) ? r.getProperty(DCATNOINFO.propertyType).getLiteral().getString() : "";
    }

    private String getPropertyLiteralValue(Resource r, org.apache.jena.rdf.model.Property p) {
        return r.hasProperty(p) ? r.getProperty(p).getLiteral().getString() : null;
    }

    private String getIsDescribedBy(Resource r) {
        return r.hasProperty(DCATNOINFO.isDescribedBy) ? r.getProperty(DCATNOINFO.isDescribedBy).getString() : null;
    }

    private PropertyType getSubclassOf(Resource r) {
        if (r.hasProperty(DCATNOINFO.isSubclassOf)) {
            Resource parentResource = r.getPropertyResourceValue(DCATNOINFO.isSubclassOf);
            return new PropertyType(parentResource.getURI(), extractLanguageLiteralFromResource(parentResource, DCATNOINFO.name));
        }
        return null;
    }

    private String extractVersion(Resource r) {
        return r.hasProperty(DCATNOINFO.version) ? r.getProperty(DCATNOINFO.version).getString() : null;
    }

    private LocalDateTime extractDateFromTemporalResource(Resource r, org.apache.jena.rdf.model.Property p) {
        return r.hasProperty(DCTerms.temporal) ? extractDate(r.getPropertyResourceValue(DCTerms.temporal), p) : null;
    }

    private LocalDateTime extractDate(Resource r, org.apache.jena.rdf.model.Property p) {
        return r.hasProperty(p)
                ? LocalDateTime.parse(r.getProperty(p).getLiteral().getString(), DateTimeFormatter.ISO_OFFSET_DATE_TIME)
                : null;
    }

    private Map<String, String> getRestrictions(Resource r) {
        Predicate<Statement> isRestriction = p -> p.getPredicate().getURI().contains("XMLSchema");
        return r.listProperties().filterKeep(isRestriction).toList().stream()
                .collect(Collectors.toMap(s -> s.getPredicate().getLocalName(), s -> s.getLiteral().getString()));
    }

    private PropertyType extractPropertyType(Resource r) {
        if (r.hasProperty(DCATNOINFO.type)) {
            Resource propertyTypeResource = r.getPropertyResourceValue(DCATNOINFO.type);
            return new PropertyType(propertyTypeResource.getURI(), extractLanguageLiteralFromResource(propertyTypeResource, DCATNOINFO.name));
        }

        return null;
    }

    private void parseInformationModel(Resource r) {
        // skip if we have already parsed this type
        if (nodeList.stream().anyMatch(o -> o.getIdentifier().equals(r.getURI()))) {
            return;
        }

        String elementType = getPropertyLiteralValue(r, DCATNOINFO.modelElementType);

        if (elementType == null) {
            return;
        }

        Node node = new Node();
        List<Property> attributes = new ArrayList<>();
        List<Property> roles = new ArrayList<>();
        List<Property> otherProps = new ArrayList<>();

        node.setIdentifier(r.getURI());
        node.setModelElementType(elementType);
        node.setName(extractLanguageLiteralFromResource(r, DCATNOINFO.name));
        node.setTypeDefinitionReference(getPropertyLiteralValue(r, DCATNOINFO.typeDefinitionReference));
        node.setIsDescribedByUri(getPropertyLiteralValue(r, DCATNOINFO.isDescribedBy));
        node.setIsSubclassOf(getSubclassOf(r));
        node.setCodeListReference(getCodeListReference(r));

        StmtIterator properties = r.listProperties(elementType.equals("kodeliste") ? DCATNOINFO.containsCodename : DCATNOINFO.hasProperty);
        while (properties.hasNext()) {
            // Fill out the properties' metadata
            Resource child = properties.nextStatement().getResource();

            Property property = new Property();
            property.setIdentifier(child.getURI());
            property.setName(extractLanguageLiteralFromResource(child, DCATNOINFO.name));
            property.setParameters(getRestrictions(child));
            property.setIsDescribedByUri(getIsDescribedBy(child));
            property.setType(extractPropertyType(child));

            String propType = getPropertyType(child);
            switch (propType) {
                case "rolle":
                    roles.add(property);
                    break;
                case "attributt":
                    attributes.add(property);
                    break;
                default:
                    otherProps.add(property);
                    break;
            }


            // continue parsing recursively
            if (child.hasProperty(DCATNOINFO.type)) {
                parseInformationModel(child.getPropertyResourceValue(DCATNOINFO.type));
            }

        }

        node.setAttributes(attributes);
        node.setProperties(otherProps);
        node.setRoles(roles);
        nodeList.add(node);
    }

    private String getCodeListReference(Resource r) {
        return r.hasProperty(DCATNOINFO.codeListReference) ? r.getProperty(DCATNOINFO.codeListReference).getString() : null;
    }

    private String extractLandingPage(Resource r) {
        if (r.hasProperty(DCAT.landingPage)) {
            Resource landingPageResource = r.getPropertyResourceValue(DCAT.landingPage);
            return landingPageResource.isURIResource() ? landingPageResource.getURI() : null;
        }
        return null;
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

            Harvest metadata = new Harvest();
            if (informationModelResource.hasProperty(DCTerms.modified)) {
                metadata.setLastChanged(LocalDateTime.parse(
                        informationModelResource.getProperty(DCTerms.modified).getLiteral().getString(),
                        DateTimeFormatter.ISO_OFFSET_DATE_TIME));
            }
            informationModel.setHarvest(metadata);

            InformationModelDocument document = new InformationModelDocument();
            document.setIdentifier(informationModelResource.getURI());
            document.setTitle(extractLanguageLiteralFromResource(informationModelResource, DCTerms.title));
            document.setDescription(extractLanguageLiteralFromResource(informationModelResource, DCTerms.description));
            document.setModelDescription(extractLanguageLiteralFromResource(informationModelResource, DCATNOINFO.description));
            document.setName(extractLanguageLiteralFromResource(informationModelResource, DCATNOINFO.name));
            document.setKeywords(extractLanguageArrayLiteralFromResource(informationModelResource, DCAT.keyword));
            document.setVersion(extractVersion(informationModelResource));
            document.setLandingPage(extractLandingPage(informationModelResource));
            document.setValidFromIncluding(extractDateFromTemporalResource(informationModelResource, DCAT.startDate));
            document.setValidToIncluding(extractDateFromTemporalResource(informationModelResource, DCAT.endDate));
            document.setIssued(extractDate(informationModelResource, DCTerms.issued));

            List<String> themes = extractLosThemeUris(informationModelResource);
            document.setThemes(!themes.isEmpty() ? referenceDataClient.getLosThemesByUris(themes) : null);

            Resource contactPoint = informationModelResource.getProperty(DCAT.contactPoint).getResource();
            if (contactPoint != null) {
                document.setContactPoint(new ContactPoint(
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
            ResIterator catalogResources = model.listResourcesWithProperty(RDF.type, DCAT.Catalog);
            while (catalogResources.hasNext()) {
                Resource catalogResource = catalogResources.nextResource();
                List<Statement> catalogRecords = catalogResource.listProperties(DCAT.record).toList();
                Publisher publisher = extractPublisher(catalogResource);

                convertRDFRecordsToModels(catalogRecords).forEach(record -> {
                    record.setId(UUID.randomUUID().toString());
                    record.setPublisher(publisher != null ? Publisher.from(organizationCatalogueClient.getOrganization(publisher.getId())) : null);
                    record.setHarvestSourceUri(harvestSourceUri);

                    Optional<InformationModelEnhanced> existing = informationmodelEnhancedRepository.getByUniqueUri(record.getUniqueUri());

                    Harvest harvestTime = new Harvest();
                    harvestTime.setLastChanged(record.getHarvest().getLastChanged());
                    harvestTime.setLastHarvested(LocalDateTime.now());
                    harvestTime.setFirstHarvested(LocalDateTime.now());

                    existing.ifPresent(present -> {
                        harvestTime.setFirstHarvested(present.getHarvest().getFirstHarvested());
                        record.setId(present.getId());
                    });

                    record.setHarvest(harvestTime);
                    informationmodelEnhancedRepository.save(record);
                });
            }
        } catch (Exception e) {
            logger.error("Got exception for Elasticsearch: {}", harvestSourceUri, e);
        }
    }
}
