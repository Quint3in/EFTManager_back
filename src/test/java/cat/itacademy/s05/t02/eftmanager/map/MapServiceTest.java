package cat.itacademy.s05.t02.eftmanager.map;

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
class MapServiceTest {

    private MapService mapService;
    private final JsonMapper jsonMapper = JsonMapper.builder().build();

    private static final String CATALOG = """
        { "data": { "maps": {
            "map-1": { "name": "mapKey1", "normalizedName": "customs", "wiki": "https://wiki/customs" }
        }}}
        """;

    private static final String LOCALE = """
        { "data": { "mapKey1": "Aduanas" } }
        """;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = mock(RestClient.Builder.class, RETURNS_DEEP_STUBS);
        lenient().when(builder.baseUrl(anyString()).build()
                        .get().uri(anyString(), any(Object[].class))
                        .retrieve().body(JsonNode.class))
                .thenReturn(jsonMapper.readTree(CATALOG), jsonMapper.readTree(LOCALE));

        mapService = new MapService(builder, "https://json.tarkov.dev", null);
        ReflectionTestUtils.setField(mapService, "self", mapService);
    }

    @Test
    void getMaps_resolvesTranslatedName() {
        List<MapResponse> result = mapService.getMaps(GameMode.PVP, List.of("map-1"), "es");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).name()).isEqualTo("Aduanas");
    }

    @Test
    void getMaps_withUnknownId_returnsEmptyList() {
        List<MapResponse> result = mapService.getMaps(GameMode.PVP, List.of("no-existe"), "es");

        assertThat(result).isEmpty();
    }
}