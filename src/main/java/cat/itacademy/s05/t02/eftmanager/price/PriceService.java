package cat.itacademy.s05.t02.eftmanager.price;

import cat.itacademy.s05.t02.eftmanager.common.GameMode;
import cat.itacademy.s05.t02.eftmanager.common.exception.ExternalApiException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import tools.jackson.databind.JsonNode;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
public class PriceService {

    private final RestClient restClient;

    public PriceService(RestClient.Builder restClientBuilder,
                        @Value("${tarkov.api.base-url}") String tarkovApiBaseUrl) {
        this.restClient = restClientBuilder.baseUrl(tarkovApiBaseUrl).build();
    }

    @Cacheable(value = "priceHistory", key = "#mode + '-' + #itemId")
    public List<PricePointResponse> getPriceHistory(GameMode mode, String itemId) {
        JsonNode response;
        try {
            response = restClient.get()
                    .uri("/{externalMode}/prices/{itemId}", mode.getExternalPath(), itemId)
                    .retrieve()
                    .body(JsonNode.class);
        } catch (RestClientException ex) {
            throw new ExternalApiException("No se pudo obtener el histórico de precios", ex);
        }

        List<PricePointResponse> points = new ArrayList<>();
        for (JsonNode point : response.path("data")) {
            points.add(new PricePointResponse(
                    point.path("price").asInt(0),
                    point.path("priceMin").asInt(0),
                    Instant.ofEpochMilli(point.path("timestamp").asLong(0))
            ));
        }
        return points;
    }

    @CacheEvict(value = "priceHistory", allEntries = true)
    @Scheduled(fixedRate = 30, timeUnit = TimeUnit.MINUTES)
    public void evictPriceCache() {
    }
}