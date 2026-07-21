package cat.itacademy.s05.t02.eftmanager.service;

import cat.itacademy.s05.t02.eftmanager.dto.TraderResponse;
import cat.itacademy.s05.t02.eftmanager.exception.ExternalApiException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import tools.jackson.databind.JsonNode;

import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
public class TradersService {

    private final RestClient restClient;
    private final String language;

    public TradersService(RestClient.Builder restClientBuilder,
                          @Value("${tarkov.api.base-url}") String tarkovApiBaseUrl,
                          @Value("${tarkov.api.language}") String language) {
        this.restClient = restClientBuilder.baseUrl(tarkovApiBaseUrl).build();
        this.language = language;
    }

    @Cacheable(value = "tradersData", key = "#mode")
    public JsonNode getTradersCatalog(GameMode mode) {
        try {
            return restClient.get()
                    .uri("/{externalMode}/traders", mode.getExternalPath())
                    .retrieve()
                    .body(JsonNode.class);
        } catch (RestClientException ex) {
            throw new ExternalApiException("No se pudo obtener el catálogo de traders", ex);
        }
    }

    @Cacheable(value = "tradersLocale", key = "#mode")
    public JsonNode getTradersLocale(GameMode mode) {
        try {
            return restClient.get()
                    .uri("/{externalMode}/traders_{lang}", mode.getExternalPath(), language)
                    .retrieve()
                    .body(JsonNode.class);
        } catch (RestClientException ex) {
            throw new ExternalApiException("No se pudo obtener las traducciones de traders", ex);
        }
    }

    @CacheEvict(value = {"tradersData", "tradersLocale"}, allEntries = true)
    @Scheduled(fixedRate = 24, timeUnit = TimeUnit.HOURS)
    public void evictTradersCache() {
    }

    public List<TraderResponse> getTraders(GameMode mode, List<String> ids) {
        JsonNode catalog = getTradersCatalog(mode);
        JsonNode locale = getTradersLocale(mode).path("data");
        JsonNode tradersNode = catalog.path("data");

        List<TraderResponse> result = new ArrayList<>();

        for (String id : ids) {
            JsonNode trader = tradersNode.path(id);
            if (trader.isMissingNode()) {
                continue;
            }

            String normalizedName = trader.path("normalizedName").asString("");

            String nameKey = trader.path("name").asString("");
            String resolvedName = locale.path(nameKey).asString(normalizedName);

            String descriptionKey = trader.path("description").asString("");
            String resolvedDescription = locale.path(descriptionKey).asString("");

            Instant resetTime = parseResetTime(trader.path("resetTime").asString(null));

            result.add(new TraderResponse(
                    id,
                    resolvedName,
                    resolvedDescription,
                    normalizedName,
                    trader.path("currency").asString(""),
                    resetTime,
                    trader.path("imageLink").asString(null)
            ));
        }

        return result;
    }

    private Instant parseResetTime(String rawResetTime) {
        if (rawResetTime == null || rawResetTime.isBlank()) {
            return null;
        }
        try {
            return Instant.parse(rawResetTime);
        } catch (DateTimeParseException ex) {
            return null;
        }
    }
}