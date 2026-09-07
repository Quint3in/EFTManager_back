package cat.itacademy.s05.t02.eftmanager.item;

import cat.itacademy.s05.t02.eftmanager.common.GameMode;
import cat.itacademy.s05.t02.eftmanager.common.TarkovMetadataService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/items")
@RequiredArgsConstructor
public class ItemController {

    private final ItemService itemService;
    private final TarkovMetadataService tarkovMetadataService;

    @GetMapping
    public List<ItemResponse> getItems(@RequestParam String mode, @RequestParam List<String> ids,
                                       @RequestParam(required = false) String lang) {
        GameMode gameMode = GameMode.valueOf(mode.toUpperCase());
        return itemService.getItems(gameMode, ids, tarkovMetadataService.resolveLanguage(lang));
    }

    @GetMapping("/search")
    public PagedResponse<ItemSummaryResponse> searchItems(
            @RequestParam String mode,
            @RequestParam(required = false, defaultValue = "") String query,
            @RequestParam(required = false) String categoryId,
            @RequestParam(required = false) String sort,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "60") int size,
            @RequestParam(required = false) String lang) {
        GameMode gameMode = GameMode.valueOf(mode.toUpperCase());
        return itemService.searchItems(gameMode, query, categoryId, sort, page, size, tarkovMetadataService.resolveLanguage(lang));
    }

    @GetMapping("/by-ids")
    public List<ItemSummaryResponse> getItemSummariesByIds(@RequestParam String mode, @RequestParam List<String> ids,
                                                           @RequestParam(required = false) String lang) {
        GameMode gameMode = GameMode.valueOf(mode.toUpperCase());
        return itemService.getSummariesByIds(gameMode, ids, tarkovMetadataService.resolveLanguage(lang));
    }
}