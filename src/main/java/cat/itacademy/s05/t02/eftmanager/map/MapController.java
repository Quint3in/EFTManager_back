package cat.itacademy.s05.t02.eftmanager.map;

import cat.itacademy.s05.t02.eftmanager.common.GameMode;
import cat.itacademy.s05.t02.eftmanager.common.TarkovMetadataService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/maps")
@RequiredArgsConstructor
public class MapController {

    private final MapService mapService;
    private final TarkovMetadataService tarkovMetadataService;

    @GetMapping
    public List<MapResponse> getMaps(@RequestParam String mode, @RequestParam List<String> ids,
                                     @RequestParam(required = false) String lang) {
        GameMode gameMode = GameMode.valueOf(mode.toUpperCase());
        return mapService.getMaps(gameMode, ids, tarkovMetadataService.resolveLanguage(lang));
    }
}