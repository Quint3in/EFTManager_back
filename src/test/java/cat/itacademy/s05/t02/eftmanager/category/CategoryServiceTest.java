package cat.itacademy.s05.t02.eftmanager.category;

import cat.itacademy.s05.t02.eftmanager.item.ItemService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    @Mock private ItemService itemService;
    private CategoryService categoryService;
    private final JsonMapper jsonMapper = JsonMapper.builder().build();

    private static final String CATALOG = """
        { "data": { "itemCategories": {
            "cat-parent": { "name": "parentKey", "normalizedName": "weapons", "parent": null, "children": ["cat-child"] },
            "cat-child": { "name": "childKey", "normalizedName": "rifles", "parent": "cat-parent", "children": [] }
        }}}
        """;

    private static final String LOCALE = """
        { "data": { "parentKey": "Armas", "childKey": "Rifles" } }
        """;

    @BeforeEach
    void setUp() {
        categoryService = new CategoryService(itemService);
    }

    @Test
    void getCategories_resolvesNamesAndParentChildRelations() {
        when(itemService.getItemsCatalog(any())).thenReturn(jsonMapper.readTree(CATALOG));
        when(itemService.getItemsLocale(any(), anyString())).thenReturn(jsonMapper.readTree(LOCALE));

        List<CategoryResponse> result = categoryService.getCategories(cat.itacademy.s05.t02.eftmanager.common.GameMode.PVP, "es");

        CategoryResponse parent = result.stream().filter(c -> c.id().equals("cat-parent")).findFirst().orElseThrow();
        CategoryResponse child = result.stream().filter(c -> c.id().equals("cat-child")).findFirst().orElseThrow();

        assertThat(parent.name()).isEqualTo("Armas");
        assertThat(parent.parentId()).isNull();
        assertThat(parent.childrenIds()).containsExactly("cat-child");
        assertThat(child.name()).isEqualTo("Rifles");
        assertThat(child.parentId()).isEqualTo("cat-parent");
    }

    private static cat.itacademy.s05.t02.eftmanager.common.GameMode any() {
        return org.mockito.ArgumentMatchers.any();
    }

    private static String anyString() {
        return org.mockito.ArgumentMatchers.anyString();
    }
}