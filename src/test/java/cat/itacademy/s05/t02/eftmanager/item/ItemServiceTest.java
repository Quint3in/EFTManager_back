package cat.itacademy.s05.t02.eftmanager.item;

import cat.itacademy.s05.t02.eftmanager.common.GameMode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ItemServiceTest {

    private ItemService itemService;
    private final JsonMapper jsonMapper = JsonMapper.builder().build();

    private static final String ITEMS_CATALOG = """
        {
          "data": {
            "items": {
              "item-a": { "name": "keyA", "normalizedName": "item-a", "iconLink": null, "avg24hPrice": 300, "lastLowPrice": 250, "changeLast48h": 10, "changeLast48hPercent": 3.3, "basePrice": 100, "minLevelForFlea": 0, "types": [], "categories": ["cat-1"] },
              "item-b": { "name": "keyB", "normalizedName": "item-b", "iconLink": null, "avg24hPrice": 100, "lastLowPrice": 90, "changeLast48h": -5, "changeLast48hPercent": -2.1, "basePrice": 50, "minLevelForFlea": 0, "types": ["noFlea"], "categories": ["cat-2"] },
              "item-c": { "name": "keyC", "normalizedName": "item-c", "iconLink": null, "avg24hPrice": 200, "lastLowPrice": 180, "changeLast48h": 0, "changeLast48hPercent": 0, "basePrice": 75, "minLevelForFlea": 5, "types": [], "categories": ["cat-1"] }
            }
          }
        }
        """;

    private static final String LOCALE = """
        { "data": { "keyA": "Alpha Item", "keyB": "Bravo Item", "keyC": "Charlie Item" } }
        """;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = mock(RestClient.Builder.class, RETURNS_DEEP_STUBS);
        JsonNode catalogNode = jsonMapper.readTree(ITEMS_CATALOG);
        JsonNode localeNode = jsonMapper.readTree(LOCALE);

        lenient().when(builder.baseUrl(anyString()).build()
                        .get().uri(anyString(), any(Object[].class))
                        .retrieve().body(JsonNode.class))
                .thenReturn(catalogNode, localeNode);

        itemService = new ItemService(builder, "https://json.tarkov.dev", null);
        ReflectionTestUtils.setField(itemService, "self", itemService);
    }

    @Test
    void searchItems_sortedByPriceDescending_returnsCorrectOrder() {
        PagedResponse<ItemSummaryResponse> result =
                itemService.searchItems(GameMode.PVP, "", null, "price_desc", 0, 10, "es");

        assertThat(result.content())
                .extracting(ItemSummaryResponse::id)
                .containsExactly("item-a", "item-c", "item-b");
    }

    @Test
    void searchItems_filteredByCategory_returnsOnlyMatchingItems() {
        PagedResponse<ItemSummaryResponse> result =
                itemService.searchItems(GameMode.PVP, "", "cat-1", "name_asc", 0, 10, "es");

        assertThat(result.content())
                .extracting(ItemSummaryResponse::id)
                .containsExactlyInAnyOrder("item-a", "item-c");
    }

    @Test
    void searchItems_itemWithNoFleaType_isMarkedAsCannotSell() {
        PagedResponse<ItemSummaryResponse> result =
                itemService.searchItems(GameMode.PVP, "", null, "name_asc", 0, 10, "es");

        ItemSummaryResponse itemB = result.content().stream()
                .filter(i -> i.id().equals("item-b"))
                .findFirst().orElseThrow();

        assertThat(itemB.canSellOnFlea()).isFalse();
    }

    @Test
    void searchItems_resolvesTranslatedNamesFromLocale() {
        PagedResponse<ItemSummaryResponse> result =
                itemService.searchItems(GameMode.PVP, "alpha", null, "name_asc", 0, 10, "es");

        assertThat(result.content()).hasSize(1);
        assertThat(result.content().get(0).name()).isEqualTo("Alpha Item");
    }
}