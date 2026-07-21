package cat.itacademy.s05.t02.eftmanager.service;

import cat.itacademy.s05.t02.eftmanager.dto.ItemResponse;
import cat.itacademy.s05.t02.eftmanager.exception.ExternalApiException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import tools.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
public class ItemService {

    private final RestClient restClient;
    private final String language;

    public ItemService(RestClient.Builder restClientBuilder,
                       @Value("${tarkov.api.base-url}") String tarkovApiBaseUrl,
                       @Value("${tarkov.api.language}") String language) {
        this.restClient = restClientBuilder
                .baseUrl(tarkovApiBaseUrl)
                .build();
        this.language = language;
    }

    @Cacheable(value = "itemsData", key = "#mode")
    public JsonNode getItemsCatalog(GameMode mode) {
        try {
            return restClient.get()
                    .uri("/{externalMode}/items", mode.getExternalPath())
                    .retrieve()
                    .body(JsonNode.class);
        } catch (RestClientException ex) {
            throw new ExternalApiException(
                    "No se pudo obtener el catálogo de ítems", ex);
        }
    }

    @Cacheable(value = "itemsLocale", key = "#mode")
    public JsonNode getItemsLocale(GameMode mode) {
        try {
            return restClient.get()
                    .uri("/{externalMode}/items_{lang}", mode.getExternalPath(), language)
                    .retrieve()
                    .body(JsonNode.class);
        } catch (RestClientException ex) {
            throw new ExternalApiException(
                    "No se pudo obtener las traducciones de ítems", ex);
        }
    }

    @CacheEvict(value = {"itemsData", "itemsLocale"}, allEntries = true)
    @Scheduled(fixedRate = 24, timeUnit = TimeUnit.HOURS)
    public void evictItemsCache() {
    }

    public List<ItemResponse> getItems(GameMode mode, List<String> ids) {
        JsonNode catalog = getItemsCatalog(mode);
        JsonNode locale = getItemsLocale(mode).path("data");
        JsonNode itemsNode = catalog.path("data").path("items");

        List<ItemResponse> result = new ArrayList<>();

        for (String id : ids) {
            JsonNode item = itemsNode.path(id);
            if (item.isMissingNode()) {
                continue;
            }

            String normalizedName = item.path("normalizedName").asString("");

            String nameKey = item.path("name").asString("");
            String resolvedName = locale.path(nameKey).asString(normalizedName);

            String shortNameKey = item.path("shortName").asString("");
            String resolvedShortName = locale.path(shortNameKey).asString(normalizedName);

            result.add(new ItemResponse(
                    id,
                    resolvedName,
                    resolvedShortName,
                    normalizedName,
                    item.path("weight").asDouble(0),
                    item.path("width").asInt(0),
                    item.path("height").asInt(0),
                    item.path("iconLink").asString(null)
            ));
        }

        return result;
    }
}