package cat.itacademy.s05.t02.eftmanager.category;

import cat.itacademy.s05.t02.eftmanager.common.GameMode;
import cat.itacademy.s05.t02.eftmanager.common.TarkovMetadataService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;
    private final TarkovMetadataService tarkovMetadataService;

    @GetMapping
    public List<CategoryResponse> getCategories(@RequestParam String mode, @RequestParam(required = false) String lang) {
        GameMode gameMode = GameMode.valueOf(mode.toUpperCase());
        return categoryService.getCategories(gameMode, tarkovMetadataService.resolveLanguage(lang));
    }
}