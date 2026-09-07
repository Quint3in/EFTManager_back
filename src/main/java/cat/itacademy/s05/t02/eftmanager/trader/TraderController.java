package cat.itacademy.s05.t02.eftmanager.trader;

import cat.itacademy.s05.t02.eftmanager.common.GameMode;
import cat.itacademy.s05.t02.eftmanager.common.TarkovMetadataService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/traders")
@RequiredArgsConstructor
public class TraderController {

    private final TradersService tradersService;
    private final TarkovMetadataService tarkovMetadataService;

    @GetMapping
    public List<TraderResponse> getTraders(@RequestParam String mode, @RequestParam List<String> ids,
                                           @RequestParam(required = false) String lang) {
        GameMode gameMode = GameMode.valueOf(mode.toUpperCase());
        return tradersService.getTraders(gameMode, ids, tarkovMetadataService.resolveLanguage(lang));
    }
}