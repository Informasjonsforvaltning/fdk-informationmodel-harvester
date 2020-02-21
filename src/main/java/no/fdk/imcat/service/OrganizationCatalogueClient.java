package no.fdk.imcat.service;

import no.fdk.imcat.dto.OrganizationDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import static java.lang.String.format;
import static java.util.Collections.singletonList;

@Service
public class OrganizationCatalogueClient {
    private static final Logger logger = LoggerFactory.getLogger(OrganizationCatalogueClient.class);

    private final RestTemplate restTemplate;
    private HttpHeaders defaultHeaders;

    @Value("${application.organizationCatalogueUrl}")
    private String apiHost;

    public OrganizationCatalogueClient() {
        this.restTemplate = new RestTemplate();

        this.defaultHeaders = new HttpHeaders();
        defaultHeaders.setAccept(singletonList(MediaType.APPLICATION_JSON));
        defaultHeaders.setContentType(MediaType.APPLICATION_JSON);
    }

    public OrganizationDto getOrganization(String orgNr) {
        String url = format("%s/organizations/%s", this.apiHost, orgNr);

        try {
            ResponseEntity<OrganizationDto> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    new HttpEntity(defaultHeaders),
                    OrganizationDto.class
            );

            return response.hasBody() ? response.getBody() : new OrganizationDto();

        } catch (HttpClientErrorException e) {
            logger.error(String.format("Error looking up publisher from GET / %s. %s (%d)",
                    url, e.getStatusText(), e.getStatusCode().value()));
        } catch (RestClientException e) {
            logger.error(e.getMessage());
        }

        return new OrganizationDto();
    }
}
