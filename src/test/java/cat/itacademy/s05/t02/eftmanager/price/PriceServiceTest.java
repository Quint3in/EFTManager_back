package cat.itacademy.s05.t02.eftmanager.price;

import cat.itacademy.s05.t02.eftmanager.common.GameMode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PriceServiceTest {

    private final JsonMapper jsonMapper = JsonMapper.builder().build();

    private static final String PRICE_RESPONSE = """
        { "data": [
            { "price": 64540, "priceMin": 40000, "timestamp": 1721347200000 }
        ]}
        """;

    @Test
    void getPriceHistory_parsesPricePointsCorrectly() {
        RestClient.Builder builder = mock(RestClient.Builder.class, RETURNS_DEEP_STUBS);
        when(builder.baseUrl(anyString()).build()
                .get().uri(anyString(), any(Object[].class))
                .retrieve().body(JsonNode.class))
                .thenReturn(jsonMapper.readTree(PRICE_RESPONSE));

        PriceService priceService = new PriceService(builder, "https://json.tarkov.dev");

        List<PricePointResponse> result = priceService.getPriceHistory(GameMode.PVP, "some-item-id");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).price()).isEqualTo(64540);
        assertThat(result.get(0).priceMin()).isEqualTo(40000);
        assertThat(result.get(0).timestamp()).isEqualTo(Instant.ofEpochMilli(1721347200000L));
    }
}