package cat.itacademy.s05.t02.eftmanager.hideout;

import cat.itacademy.s05.t02.eftmanager.common.GameMode;
import cat.itacademy.s05.t02.eftmanager.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.JsonNode;
import org.springframework.test.util.ReflectionTestUtils;
import static org.mockito.Mockito.lenient;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HideoutServiceTest {

    @Mock private HideoutProgressRepository hideoutProgressRepository;
    @Mock private UserRepository userRepository;

    private HideoutService hideoutService;
    private final JsonMapper jsonMapper = JsonMapper.builder().build();

    private static final String STASH_CATALOG = """
        {
          "data": {
            "stash-id": {
              "name": "stash_name_key",
              "normalizedName": "stash",
              "imageLink": null,
              "levels": [
                { "level": 1, "constructionTime": 0, "itemRequirements": [], "traderRequirements": [], "stationLevelRequirements": [], "skillRequirements": [] },
                { "level": 2, "constructionTime": 3600, "itemRequirements": [], "traderRequirements": [], "stationLevelRequirements": [], "skillRequirements": [] },
                { "level": 3, "constructionTime": 7200, "itemRequirements": [], "traderRequirements": [], "stationLevelRequirements": [], "skillRequirements": [] }
              ]
            },
            "generator-id": {
              "name": "generator_name_key",
              "normalizedName": "generator",
              "imageLink": null,
              "levels": [
                { "level": 1, "constructionTime": 0, "itemRequirements": [], "traderRequirements": [], "stationLevelRequirements": [], "skillRequirements": [] },
                { "level": 2, "constructionTime": 3600, "itemRequirements": [], "traderRequirements": [], "stationLevelRequirements": [], "skillRequirements": [] }
              ]
            }
          }
        }
        """;

    private static final String LOCALE = """
        { "data": { "stash_name_key": "Alijo", "generator_name_key": "Generador" } }
        """;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = mock(RestClient.Builder.class, RETURNS_DEEP_STUBS);
        JsonNode stashCatalogNode = jsonMapper.readTree(STASH_CATALOG);
        JsonNode localeNode = jsonMapper.readTree(LOCALE);

        lenient().when(builder.baseUrl(anyString()).build()
                        .get().uri(anyString(), any(Object[].class))
                        .retrieve().body(JsonNode.class))
                .thenReturn(stashCatalogNode, localeNode);

        hideoutService = new HideoutService(builder, "https://json.tarkov.dev", hideoutProgressRepository, userRepository, null);
        ReflectionTestUtils.setField(hideoutService, "self", hideoutService);
    }

    @Test
    void updateProgress_onStash_withLevelZero_throwsInvalidHideoutLevelException() {
        lenient().when(hideoutProgressRepository.findByUserIdAndStationIdAndMode(1L, "stash-id", GameMode.PVP))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> hideoutService.updateProgress(1L, GameMode.PVP, "stash-id", 0, "es"))
                .isInstanceOf(InvalidHideoutLevelException.class)
                .hasMessageContaining("mínimo");

        verify(hideoutProgressRepository, never()).save(any());
    }

    @Test
    void updateProgress_onStash_withLevelOne_succeeds() {
        when(hideoutProgressRepository.findByUserIdAndStationIdAndMode(1L, "stash-id", GameMode.PVP))
                .thenReturn(Optional.empty());
        when(userRepository.getReferenceById(1L)).thenReturn(null);
        when(hideoutProgressRepository.save(any(HideoutProgress.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        HideoutStationResponse response = hideoutService.updateProgress(1L, GameMode.PVP, "stash-id", 1, "es");

        assertThat(response.currentLevel()).isEqualTo(1);
        assertThat(response.minLevel()).isEqualTo(1);
    }

    @Test
    void updateProgress_onNonStashStation_withLevelZero_succeeds() {
        when(hideoutProgressRepository.findByUserIdAndStationIdAndMode(1L, "generator-id", GameMode.PVP))
                .thenReturn(Optional.empty());
        when(userRepository.getReferenceById(1L)).thenReturn(null);
        when(hideoutProgressRepository.save(any(HideoutProgress.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        HideoutStationResponse response = hideoutService.updateProgress(1L, GameMode.PVP, "generator-id", 0, "es");

        assertThat(response.currentLevel()).isEqualTo(0);
        assertThat(response.minLevel()).isEqualTo(0);
    }

    @Test
    void updateProgress_withLevelAboveMax_throwsInvalidHideoutLevelException() {
        lenient().when(hideoutProgressRepository.findByUserIdAndStationIdAndMode(1L, "generator-id", GameMode.PVP))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> hideoutService.updateProgress(1L, GameMode.PVP, "generator-id", 5, "es"))
                .isInstanceOf(InvalidHideoutLevelException.class)
                .hasMessageContaining("entre");
    }

    @Test
    void updateProgress_onUnknownStation_throwsStationNotFoundException() {
        assertThatThrownBy(() -> hideoutService.updateProgress(1L, GameMode.PVP, "no-existe", 1, "es"))
                .isInstanceOf(StationNotFoundException.class);

        verifyNoInteractions(hideoutProgressRepository);
    }

    @Test
    void updateProgress_resolvesStationNameFromLocale() {
        when(hideoutProgressRepository.findByUserIdAndStationIdAndMode(1L, "generator-id", GameMode.PVP))
                .thenReturn(Optional.empty());
        when(userRepository.getReferenceById(1L)).thenReturn(null);
        when(hideoutProgressRepository.save(any(HideoutProgress.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        HideoutStationResponse response = hideoutService.updateProgress(1L, GameMode.PVP, "generator-id", 1, "es");

        assertThat(response.name()).isEqualTo("Generador");
    }
}