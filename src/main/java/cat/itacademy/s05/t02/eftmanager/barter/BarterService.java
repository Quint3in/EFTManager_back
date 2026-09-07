package cat.itacademy.s05.t02.eftmanager.barter;

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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
public class BarterService {

    private final RestClient restClient;

    public BarterService(RestClient.Builder restClientBuilder,
                         @Value("${tarkov.api.base-url}") String tarkovApiBaseUrl) {
        this.restClient = restClientBuilder.baseUrl(tarkovApiBaseUrl).build();
    }

    @Cacheable(value = "bartersData", key = "#mode")
    public JsonNode getBartersCatalog(GameMode mode) {
        try {
            return restClient.get()
                    .uri("/{externalMode}/barters", mode.getExternalPath())
                    .retrieve()
                    .body(JsonNode.class);
        } catch (RestClientException ex) {
            throw new ExternalApiException("No se pudo obtener el catálogo de barters", ex);
        }
    }

    @CacheEvict(value = "bartersData", allEntries = true)
    @Scheduled(fixedRate = 24, timeUnit = TimeUnit.HOURS)
    public void evictBartersCache() {
    }

    public List<BarterOption> getBartersForItem(GameMode mode, String itemId) {
        JsonNode catalog = getBartersCatalog(mode);
        JsonNode bartersNode = catalog.path("data");

        List<BarterOption> result = new ArrayList<>();

        for (JsonNode barter : bartersNode) {
            JsonNode offeredItem = barter.path("offeredItem");
            if (!itemId.equals(offeredItem.path("item").asString(""))) {
                continue;
            }

            List<BarterRequiredItem> requiredItems = new ArrayList<>();
            for (JsonNode req : barter.path("requiredItems")) {
                requiredItems.add(new BarterRequiredItem(
                        req.path("item").asString(""),
                        req.path("count").asDouble(0)
                ));
            }

            JsonNode taskUnlockNode = barter.path("taskUnlock");
            String taskUnlockId = (taskUnlockNode.isMissingNode() || taskUnlockNode.isNull())
                    ? null
                    : taskUnlockNode.asString(null);

            JsonNode buyLimitNode = barter.path("buyLimit");
            Integer buyLimit = (buyLimitNode.isMissingNode() || buyLimitNode.isNull())
                    ? null
                    : buyLimitNode.asInt();

            result.add(new BarterOption(
                    barter.path("trader").asString(""),
                    barter.path("minTraderLevel").asInt(0),
                    requiredItems,
                    offeredItem.path("count").asInt(1),
                    buyLimit,
                    taskUnlockId
            ));
        }

        return result;
    }
}