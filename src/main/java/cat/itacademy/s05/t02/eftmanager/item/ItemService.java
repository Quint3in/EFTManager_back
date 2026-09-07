package cat.itacademy.s05.t02.eftmanager.item;

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

import java.util.*;
import java.util.concurrent.TimeUnit;

@Service
public class ItemService {

    private final RestClient restClient;
    private final ItemService self;

    public ItemService(RestClient.Builder restClientBuilder,
                       @Value("${tarkov.api.base-url}") String tarkovApiBaseUrl,
                       @Lazy ItemService self) {
        this.restClient = restClientBuilder.baseUrl(tarkovApiBaseUrl).build();
        this.self = self;
    }

    @Cacheable(value = "itemsData", key = "#mode")
    public JsonNode getItemsCatalog(GameMode mode) {
        try {
            return restClient.get()
                    .uri("/{externalMode}/items", mode.getExternalPath())
                    .retrieve()
                    .body(JsonNode.class);
        } catch (RestClientException ex) {
            throw new ExternalApiException("No se pudo obtener el catálogo de ítems desde Tarkov.dev", ex);
        }
    }

    @Cacheable(value = "itemsLocale", key = "#mode + '-' + #lang")
    public JsonNode getItemsLocale(GameMode mode, String lang) {
        try {
            return restClient.get()
                    .uri("/{externalMode}/items_{lang}", mode.getExternalPath(), lang)
                    .retrieve()
                    .body(JsonNode.class);
        } catch (RestClientException ex) {
            throw new ExternalApiException("No se pudo obtener las traducciones de ítems desde Tarkov.dev", ex);
        }
    }

    @CacheEvict(value = {"itemsData", "itemsLocale", "itemsResolved"}, allEntries = true)
    @Scheduled(fixedRate = 24, timeUnit = TimeUnit.HOURS)
    public void evictItemsCache() {
    }

    @Cacheable(value = "itemsResolved", key = "#mode + '-' + #lang")
    public List<ItemSummaryResponse> getAllItemSummaries(GameMode mode, String lang) {
        JsonNode catalog = self.getItemsCatalog(mode);
        JsonNode locale = self.getItemsLocale(mode, lang).path("data");
        JsonNode itemsNode = catalog.path("data").path("items");

        List<ItemSummaryResponse> all = new ArrayList<>();

        for (Map.Entry<String, JsonNode> entry : itemsNode.properties()) {
            String itemId = entry.getKey();
            JsonNode item = entry.getValue();

            String nameKey = item.path("name").asString("");
            String normalizedName = item.path("normalizedName").asString("");
            String resolvedName = locale.path(nameKey).asString(normalizedName);

            List<String> categoryIds = new ArrayList<>();
            for (JsonNode cat : item.path("categories")) {
                categoryIds.add(cat.asString());
            }

            all.add(new ItemSummaryResponse(
                    itemId,
                    resolvedName,
                    normalizedName,
                    item.path("iconLink").asString(null),
                    item.path("avg24hPrice").asInt(0),
                    item.path("lastLowPrice").asInt(0),
                    item.path("changeLast48h").asInt(0),
                    item.path("changeLast48hPercent").asDouble(0),
                    item.path("basePrice").asInt(0),
                    !isBannedFromFlea(item),
                    item.path("minLevelForFlea").asInt(0),
                    categoryIds
            ));
        }

        return all;
    }

    public PagedResponse<ItemSummaryResponse> searchItems(GameMode mode, String query, String categoryId, String sort,
                                                          int page, int size, String lang) {
        List<ItemSummaryResponse> all = self.getAllItemSummaries(mode, lang);

        String normalizedQuery = query == null ? "" : query.toLowerCase();

        List<ItemSummaryResponse> matches = new ArrayList<>(all.stream()
                .filter(i -> normalizedQuery.isBlank()
                        || i.name().toLowerCase().contains(normalizedQuery)
                        || i.normalizedName().toLowerCase().contains(normalizedQuery))
                .filter(i -> categoryId == null || categoryId.isBlank() || i.categoryIds().contains(categoryId))
                .toList());

        applySort(matches, sort);

        int totalElements = matches.size();
        int totalPages = size > 0 ? (int) Math.ceil((double) totalElements / size) : 0;
        int fromIndex = Math.min(page * size, totalElements);
        int toIndex = Math.min(fromIndex + size, totalElements);

        return new PagedResponse<>(matches.subList(fromIndex, toIndex), page, size, totalElements, totalPages);
    }

    private void applySort(List<ItemSummaryResponse> items, String sort) {
        if (sort == null) return;

        Comparator<ItemSummaryResponse> comparator = switch (sort) {
            case "price_asc" -> Comparator.comparingInt(ItemSummaryResponse::avg24hPrice);
            case "price_desc" -> Comparator.comparingInt(ItemSummaryResponse::avg24hPrice).reversed();
            case "name_asc" -> Comparator.comparing(ItemSummaryResponse::name, String.CASE_INSENSITIVE_ORDER);
            case "name_desc" -> Comparator.comparing(ItemSummaryResponse::name, String.CASE_INSENSITIVE_ORDER).reversed();
            default -> null;
        };

        if (comparator != null) items.sort(comparator);
    }

    public List<ItemResponse> getItems(GameMode mode, List<String> ids, String lang) {
        JsonNode catalog = self.getItemsCatalog(mode);
        JsonNode locale = self.getItemsLocale(mode, lang).path("data");
        JsonNode itemsNode = catalog.path("data").path("items");

        List<ItemResponse> result = new ArrayList<>();

        for (String id : ids) {
            JsonNode item = itemsNode.path(id);
            if (item.isMissingNode()) continue;

            String normalizedName = item.path("normalizedName").asString("");

            String nameKey = item.path("name").asString("");
            String resolvedName = locale.path(nameKey).asString(normalizedName);

            String shortNameKey = item.path("shortName").asString("");
            String resolvedShortName = locale.path(shortNameKey).asString(normalizedName);

            List<ItemBuyOption> buyOptions = new ArrayList<>();
            for (JsonNode buyOption : item.path("buyFromTrader")) {
                JsonNode taskUnlockNode = buyOption.path("taskUnlock");
                String taskUnlockId = (taskUnlockNode.isMissingNode() || taskUnlockNode.isNull())
                        ? null : taskUnlockNode.asString(null);

                JsonNode buyLimitNode = buyOption.path("buyLimit");
                Integer buyLimit = (buyLimitNode.isMissingNode() || buyLimitNode.isNull())
                        ? null : buyLimitNode.asInt();

                buyOptions.add(new ItemBuyOption(
                        buyOption.path("trader").asString(""),
                        buyOption.path("price").asInt(0),
                        buyOption.path("currency").asString(""),
                        buyOption.path("currencyItem").asString(""),
                        buyOption.path("priceRUB").asInt(0),
                        buyOption.path("minTraderLevel").asInt(0),
                        buyLimit,
                        buyOption.path("restockAmount").asInt(0),
                        taskUnlockId
                ));
            }

            List<ItemSellOption> sellOptions = new ArrayList<>();
            for (JsonNode sellOption : item.path("sellToTrader")) {
                sellOptions.add(new ItemSellOption(
                        sellOption.path("trader").asString(""),
                        sellOption.path("price").asInt(0),
                        sellOption.path("currency").asString(""),
                        sellOption.path("currencyItem").asString(""),
                        sellOption.path("priceRUB").asInt(0)
                ));
            }

            boolean canSellOnFlea = !isBannedFromFlea(item);
            int minLevelForFlea = item.path("minLevelForFlea").asInt(0);

            result.add(new ItemResponse(
                    id,
                    resolvedName,
                    resolvedShortName,
                    normalizedName,
                    item.path("weight").asDouble(0),
                    item.path("width").asInt(0),
                    item.path("height").asInt(0),
                    item.path("iconLink").asString(null),
                    canSellOnFlea,
                    minLevelForFlea,
                    buyOptions,
                    sellOptions
            ));
        }

        return result;
    }

    private boolean isBannedFromFlea(JsonNode item) {
        for (JsonNode type : item.path("types")) {
            if ("noFlea".equals(type.asString(""))) {
                return true;
            }
        }
        return false;
    }

    public List<ItemSummaryResponse> getSummariesByIds(GameMode mode, List<String> ids, String lang) {
        List<ItemSummaryResponse> all = self.getAllItemSummaries(mode, lang);
        Set<String> idSet = new HashSet<>(ids);
        return all.stream().filter(i -> idSet.contains(i.id())).toList();
    }
}