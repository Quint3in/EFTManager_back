package cat.itacademy.s05.t02.eftmanager.common;

import cat.itacademy.s05.t02.eftmanager.common.exception.ExternalApiException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import tools.jackson.databind.JsonNode;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Service
public class TarkovMetadataService {

    private static final Set<String> FALLBACK_LANGUAGES = Set.of("en", "es");

    private final RestClient restClient;

    public TarkovMetadataService(RestClient.Builder restClientBuilder,
                                 @Value("${tarkov.api.base-url}") String tarkovApiBaseUrl) {
        this.restClient = restClientBuilder.baseUrl(tarkovApiBaseUrl).build();
    }

    @Cacheable("supportedLanguages")
    public Set<String> getSupportedLanguages() {
        try {
            JsonNode response = restClient.get()
                    .uri("/endpoints")
                    .retrieve()
                    .body(JsonNode.class);

            Set<String> languages = new HashSet<>();
            for (JsonNode lang : response.path("data").path("languages")) {
                languages.add(lang.asString(""));
            }

            return languages.isEmpty() ? FALLBACK_LANGUAGES : languages;
        } catch (RestClientException ex) {
            return FALLBACK_LANGUAGES;
        }
    }

    @CacheEvict("supportedLanguages")
    @Scheduled(fixedRate = 24, timeUnit = TimeUnit.HOURS)
    public void evictLanguagesCache() {
    }

    public String resolveLanguage(String requested) {
        if (requested == null) return "es";
        String lower = requested.toLowerCase();
        return getSupportedLanguages().contains(lower) ? lower : "es";
    }
}