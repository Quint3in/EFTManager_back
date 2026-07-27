package cat.itacademy.s05.t02.eftmanager.category;

import cat.itacademy.s05.t02.eftmanager.common.GameMode;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    @GetMapping
    public List<CategoryResponse> getCategories(@RequestParam String mode) {
        GameMode gameMode = GameMode.valueOf(mode.toUpperCase());
        return categoryService.getCategories(gameMode);
    }
}