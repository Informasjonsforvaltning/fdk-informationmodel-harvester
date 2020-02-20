package no.fdk.imcat.service;

import lombok.RequiredArgsConstructor;
import no.dcat.shared.LosTheme;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ReferenceDataClient {
    private static final Logger logger = LoggerFactory.getLogger(HarvestAdminClient.class);

    private RestTemplate restTemplate = new RestTemplate();

    @Value("${application.referenceDataUrl}")
    private String apiHost;

    List<LosTheme> getLosThemesByUris(List<String> uris) {
        String url = UriComponentsBuilder.fromUriString(this.apiHost).path("los").queryParam("uris", uris.toArray()).toUriString();
        try {
            ResponseEntity<List<LosTheme>> response = restTemplate.exchange(
                    url, HttpMethod.GET, HttpEntity.EMPTY, new ParameterizedTypeReference<List<LosTheme>>() {}
            );
            return response.hasBody() ? response.getBody() : null;
        } catch (HttpClientErrorException e) {
            logger.error(String.format("Error looking up publisher from GET / %s. %s (%d)",
                    url, e.getStatusText(), e.getStatusCode().value()));
        } catch (RestClientException e) {
            logger.error(e.getMessage());
        }
        return null;
    }
}
