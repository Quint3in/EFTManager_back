package cat.itacademy.s05.t02.eftmanager.trader;

import cat.itacademy.s05.t02.eftmanager.common.GameMode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
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
class TradersServiceTest {

    private TradersService tradersService;
    private final JsonMapper jsonMapper = JsonMapper.builder().build();

    private static final String CATALOG = """
        { "data": {
            "trader-a": { "name": "nameKeyA", "normalizedName": "prapor", "description": "descKeyA", "currency": "RUB", "resetTime": "2026-01-01T12:00:00Z", "imageLink": "img-a.webp" },
            "trader-b": { "name": "nameKeyB", "normalizedName": "fence", "description": "descKeyB", "currency": "RUB", "resetTime": null, "imageLink": null }
        }}
        """;

    private static final String LOCALE = """
        { "data": { "nameKeyA": "Prapor", "descKeyA": "El armero", "nameKeyB": "Fence" } }
        """;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = mock(RestClient.Builder.class, RETURNS_DEEP_STUBS);
        JsonNode catalogNode = jsonMapper.readTree(CATALOG);
        JsonNode localeNode = jsonMapper.readTree(LOCALE);

        lenient().when(builder.baseUrl(anyString()).build()
                        .get().uri(anyString(), any(Object[].class))
                        .retrieve().body(JsonNode.class))
                .thenReturn(catalogNode, localeNode);

        tradersService = new TradersService(builder, "https://json.tarkov.dev", null);
        ReflectionTestUtils.setField(tradersService, "self", tradersService);
    }

    @Test
    void getTraders_resolvesNameAndDescriptionFromLocale() {
        List<TraderResponse> result = tradersService.getTraders(GameMode.PVP, List.of("trader-a"), "es");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).name()).isEqualTo("Prapor");
        assertThat(result.get(0).description()).isEqualTo("El armero");
        assertThat(result.get(0).resetTime()).isEqualTo(Instant.parse("2026-01-01T12:00:00Z"));
    }

    @Test
    void getTraders_withNullResetTime_returnsNullWithoutError() {
        List<TraderResponse> result = tradersService.getTraders(GameMode.PVP, List.of("trader-b"), "es");

        assertThat(result.get(0).resetTime()).isNull();
    }

    @Test
    void getTraders_withUnknownId_skipsItSilently() {
        List<TraderResponse> result = tradersService.getTraders(GameMode.PVP, List.of("trader-a", "no-existe"), "es");

        assertThat(result).hasSize(1);
    }
}