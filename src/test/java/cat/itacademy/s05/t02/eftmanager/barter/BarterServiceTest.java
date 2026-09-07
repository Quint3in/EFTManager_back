package cat.itacademy.s05.t02.eftmanager.barter;

import cat.itacademy.s05.t02.eftmanager.common.GameMode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BarterServiceTest {

    private BarterService barterService;
    private final JsonMapper jsonMapper = JsonMapper.builder().build();

    private static final String BARTERS_CATALOG = """
        { "data": [
            {
              "id": "barter-1",
              "trader": "trader-a",
              "minTraderLevel": 2,
              "buyLimit": 5,
              "taskUnlock": null,
              "requiredItems": [ { "item": "item-req-1", "count": 3 } ],
              "offeredItem": { "item": "item-target", "count": 1 }
            },
            {
              "id": "barter-2",
              "trader": "trader-b",
              "minTraderLevel": 1,
              "buyLimit": null,
              "taskUnlock": null,
              "requiredItems": [ { "item": "item-req-2", "count": 1 } ],
              "offeredItem": { "item": "item-other", "count": 1 }
            }
        ]}
        """;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = mock(RestClient.Builder.class, RETURNS_DEEP_STUBS);
        when(builder.baseUrl(anyString()).build()
                .get().uri(anyString(), any(Object[].class))
                .retrieve().body(JsonNode.class))
                .thenReturn(jsonMapper.readTree(BARTERS_CATALOG));

        barterService = new BarterService(builder, "https://json.tarkov.dev");
    }

    @Test
    void getBartersForItem_returnsOnlyMatchingOfferedItem() {
        List<BarterOption> result = barterService.getBartersForItem(GameMode.PVP, "item-target");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).traderId()).isEqualTo("trader-a");
        assertThat(result.get(0).minTraderLevel()).isEqualTo(2);
        assertThat(result.get(0).requiredItems()).extracting("itemId").containsExactly("item-req-1");
    }

    @Test
    void getBartersForItem_withNoMatch_returnsEmptyList() {
        List<BarterOption> result = barterService.getBartersForItem(GameMode.PVP, "item-inexistente");

        assertThat(result).isEmpty();
    }

    @Test
    void getBartersForItem_withNullBuyLimit_returnsNullNotZero() {
        List<BarterOption> result = barterService.getBartersForItem(GameMode.PVP, "item-other");

        assertThat(result.get(0).buyLimit()).isNull();
    }
}