package cat.itacademy.s05.t02.eftmanager.price;

import cat.itacademy.s05.t02.eftmanager.common.GameMode;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/prices")
@RequiredArgsConstructor
public class PriceController {

    private final PriceService priceService;

    @GetMapping("/{itemId}")
    public List<PricePointResponse> getPriceHistory(@PathVariable String itemId, @RequestParam String mode) {
        GameMode gameMode = GameMode.valueOf(mode.toUpperCase());
        return priceService.getPriceHistory(gameMode, itemId);
    }
}