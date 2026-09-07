package cat.itacademy.s05.t02.eftmanager.skill;

import cat.itacademy.s05.t02.eftmanager.common.GameMode;
import cat.itacademy.s05.t02.eftmanager.item.ItemService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SkillServiceTest {

    @Mock private ItemService itemService;
    private SkillService skillService;
    private final JsonMapper jsonMapper = JsonMapper.builder().build();

    private static final String CATALOG = """
        { "data": { "skills": [
            { "id": "Endurance", "name": "enduranceKey", "normalizedName": "endurance", "imageLink": "endurance.webp" },
            { "id": "Strength", "name": "strengthKey", "normalizedName": "strength", "imageLink": "strength.webp" }
        ]}}
        """;

    private static final String LOCALE = """
        { "data": { "enduranceKey": "Resistencia" } }
        """;

    @BeforeEach
    void setUp() {
        skillService = new SkillService(itemService);
    }

    @Test
    void getSkills_returnsOnlyRequestedIds_withResolvedNames() {
        when(itemService.getItemsCatalog(any())).thenReturn(jsonMapper.readTree(CATALOG));
        when(itemService.getItemsLocale(any(), anyString())).thenReturn(jsonMapper.readTree(LOCALE));

        List<SkillResponse> result = skillService.getSkills(GameMode.PVP, List.of("Endurance"), "es");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).name()).isEqualTo("Resistencia");
    }

    @Test
    void getSkills_withNoLocaleMatch_fallsBackToNormalizedName() {
        when(itemService.getItemsCatalog(any())).thenReturn(jsonMapper.readTree(CATALOG));
        when(itemService.getItemsLocale(any(), anyString())).thenReturn(jsonMapper.readTree(LOCALE));

        List<SkillResponse> result = skillService.getSkills(GameMode.PVP, List.of("Strength"), "es");

        assertThat(result.get(0).name()).isEqualTo("strength");
    }
}