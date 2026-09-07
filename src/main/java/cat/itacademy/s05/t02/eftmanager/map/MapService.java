package cat.itacademy.s05.t02.eftmanager.map;

import cat.itacademy.s05.t02.eftmanager.common.GameMode;
import cat.itacademy.s05.t02.eftmanager.common.exception.ExternalApiException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import tools.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
public class MapService {

    private final RestClient restClient;
    private final MapService self;

    public MapService(RestClient.Builder restClientBuilder,
                      @Value("${tarkov.api.base-url}") String tarkovApiBaseUrl,
                      @Lazy MapService self) {
        this.restClient = restClientBuilder.baseUrl(tarkovApiBaseUrl).build();
        this.self = self;
    }

    @Cacheable(value = "mapsData", key = "#mode")
    public JsonNode getMapsCatalog(GameMode mode) {
        try {
            return restClient.get()
                    .uri("/{externalMode}/maps", mode.getExternalPath())
                    .retrieve()
                    .body(JsonNode.class);
        } catch (RestClientException ex) {
            throw new ExternalApiException("No se pudo obtener el catálogo de mapas desde Tarkov.dev", ex);
        }
    }

    @Cacheable(value = "mapsLocale", key = "#mode + '-' + #lang")
    public JsonNode getMapsLocale(GameMode mode, String lang) {
        try {
            return restClient.get()
                    .uri("/{externalMode}/maps_{lang}", mode.getExternalPath(), lang)
                    .retrieve()
                    .body(JsonNode.class);
        } catch (RestClientException ex) {
            throw new ExternalApiException("No se pudo obtener las traducciones de mapas", ex);
        }
    }

    @CacheEvict(value = {"mapsData", "mapsLocale"}, allEntries = true)
    @Scheduled(fixedRate = 24, timeUnit = TimeUnit.HOURS)
    public void evictMapsCache() {
    }

    public List<MapResponse> getMaps(GameMode mode, List<String> ids, String lang) {
        JsonNode catalog = self.getMapsCatalog(mode);
        JsonNode locale = self.getMapsLocale(mode, lang).path("data");
        JsonNode mapsNode = catalog.path("data").path("maps");

        List<MapResponse> result = new ArrayList<>();
        for (String id : ids) {
            JsonNode map = mapsNode.path(id);
            if (map.isMissingNode()) continue;

            String normalizedName = map.path("normalizedName").asString("");
            String nameKey = map.path("name").asString(id);
            String resolvedName = locale.path(nameKey).asString(normalizedName);

            result.add(new MapResponse(id, resolvedName, normalizedName, map.path("wiki").asString(null)));
        }
        return result;
    }
}