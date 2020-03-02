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
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ReferenceDataClient {
    private static final Logger logger = LoggerFactory.getLogger(ReferenceDataClient.class);
    private static final String[] NO_THEMES = {};

    private RestTemplate restTemplate = new RestTemplate();

    @Value("${application.referenceDataUrl}")
    private String apiHost;

    List<LosTheme> getLosThemesByUris(List<String> uris) {
        String url = UriComponentsBuilder.fromUriString(this.apiHost).path("los").queryParam("uris", uris.toArray()).toUriString();
        try {
            ResponseEntity<List<LosTheme>> response = restTemplate.exchange(
                    url, HttpMethod.GET, HttpEntity.EMPTY, new ParameterizedTypeReference<List<LosTheme>>() {
                    }
            );
            return response.hasBody() ? response.getBody() : Collections.emptyList();
        } catch (HttpClientErrorException e) {
            logger.error(String.format("Error looking up publisher from GET / %s. %s (%d)",
                    url, e.getStatusText(), e.getStatusCode().value()));
        } catch (RestClientException e) {
            logger.error(e.getMessage());
        }
        return Collections.emptyList();
    }

    String[] expandLosThemesByPaths(Set<String> themes) {
        String url = UriComponentsBuilder.fromUriString(this.apiHost).path("/loscodes/expandLosThemeByPaths").queryParam("themes", themes.toArray()).toUriString();
        try {
            ResponseEntity<String[]> response = restTemplate.exchange(
                    url, HttpMethod.GET, HttpEntity.EMPTY, String[].class
            );
            return response.hasBody() ? response.getBody() : NO_THEMES;
        } catch (HttpClientErrorException e) {
            logger.error(String.format("Error expanding Los Themes from GET / %s. %s (%d)",
                    url, e.getStatusText(), e.getStatusCode().value()));
        } catch (RestClientException e) {
            logger.error(e.getMessage());
        }
        return NO_THEMES;
    }
}
