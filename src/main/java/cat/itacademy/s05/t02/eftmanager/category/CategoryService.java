package cat.itacademy.s05.t02.eftmanager.category;

import cat.itacademy.s05.t02.eftmanager.common.GameMode;
import cat.itacademy.s05.t02.eftmanager.item.ItemService;
import cat.itacademy.s05.t02.eftmanager.item.ItemSummaryResponse;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class CategoryService {

    private final ItemService itemService;

    public CategoryService(ItemService itemService) {
        this.itemService = itemService;
    }

    public List<CategoryResponse> getCategories(GameMode mode, String lang) {
        JsonNode catalog = itemService.getItemsCatalog(mode);
        JsonNode locale = itemService.getItemsLocale(mode, lang).path("data");
        JsonNode categoriesNode = catalog.path("data").path("itemCategories");

        Map<String, Integer> counts = new HashMap<>();
        for (ItemSummaryResponse item : itemService.getAllItemSummaries(mode, lang)) {
            for (String catId : item.categoryIds()) {
                counts.merge(catId, 1, Integer::sum);
            }
        }

        List<CategoryResponse> result = new ArrayList<>();

        for (Map.Entry<String, JsonNode> entry : categoriesNode.properties()) {
            String categoryId = entry.getKey();
            JsonNode category = entry.getValue();

            String nameKey = category.path("name").asString(categoryId);
            String normalizedName = category.path("normalizedName").asString("");
            String resolvedName = locale.path(nameKey).asString(normalizedName);

            JsonNode parentNode = category.path("parent");
            String parentId = (parentNode.isMissingNode() || parentNode.isNull())
                    ? null : parentNode.asString(null);

            List<String> childrenIds = new ArrayList<>();
            for (JsonNode child : category.path("children")) {
                childrenIds.add(child.asString());
            }

            result.add(new CategoryResponse(
                    categoryId, resolvedName, normalizedName, parentId, childrenIds,
                    counts.getOrDefault(categoryId,0)
            ));
        }

        return result;
    }
}