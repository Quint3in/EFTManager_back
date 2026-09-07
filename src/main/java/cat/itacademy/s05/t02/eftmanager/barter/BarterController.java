package cat.itacademy.s05.t02.eftmanager.barter;

import cat.itacademy.s05.t02.eftmanager.common.GameMode;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/barters")
@RequiredArgsConstructor
public class BarterController {

    private final BarterService barterService;

    @GetMapping("/{itemId}")
    public List<BarterOption> getBarters(@PathVariable String itemId, @RequestParam String mode) {
        GameMode gameMode = GameMode.valueOf(mode.toUpperCase());
        return barterService.getBartersForItem(gameMode, itemId);
    }
}