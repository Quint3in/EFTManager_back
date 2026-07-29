package cat.itacademy.s05.t02.eftmanager.item;

import cat.itacademy.s05.t02.eftmanager.common.GameMode;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/items")
@RequiredArgsConstructor
public class ItemController {

    private final ItemService itemService;

    @GetMapping
    public List<ItemResponse> getItems(@RequestParam String mode, @RequestParam List<String> ids) {
        GameMode gameMode = GameMode.valueOf(mode.toUpperCase());
        return itemService.getItems(gameMode, ids);
    }

    @GetMapping("/search")
    public PagedResponse<ItemSummaryResponse> searchItems(
            @RequestParam String mode,
            @RequestParam(required = false, defaultValue = "") String query,
            @RequestParam(required = false) String categoryId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "30") int size) {
        GameMode gameMode = GameMode.valueOf(mode.toUpperCase());
        return itemService.searchItems(gameMode, query, categoryId, page, size);
    }
}