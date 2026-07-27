package cat.itacademy.s05.t02.eftmanager.skill;

import cat.itacademy.s05.t02.eftmanager.item.ItemService;
import cat.itacademy.s05.t02.eftmanager.common.GameMode;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.List;

@Service
public class SkillService {

    private final ItemService itemService;

    public SkillService(ItemService itemService) {
        this.itemService = itemService;
    }

    public List<SkillResponse> getSkills(GameMode mode, List<String> ids) {
        JsonNode catalog = itemService.getItemsCatalog(mode);
        JsonNode locale = itemService.getItemsLocale(mode).path("data");
        JsonNode skillsNode = catalog.path("data").path("skills");

        List<SkillResponse> result = new ArrayList<>();

        for (JsonNode skill : skillsNode) {
            String skillId = skill.path("id").asString("");
            if (!ids.contains(skillId)) {
                continue;
            }

            String nameKey = skill.path("name").asString(skillId);
            String normalizedName = skill.path("normalizedName").asString("");
            String resolvedName = locale.path(nameKey).asString(normalizedName);

            result.add(new SkillResponse(
                    skillId,
                    resolvedName,
                    normalizedName,
                    skill.path("imageLink").asString(null)
            ));
        }

        return result;
    }
}