package cat.itacademy.s05.t02.eftmanager.skill;

import cat.itacademy.s05.t02.eftmanager.common.GameMode;
import cat.itacademy.s05.t02.eftmanager.common.TarkovMetadataService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/skills")
@RequiredArgsConstructor
public class SkillController {

    private final SkillService skillService;
    private final TarkovMetadataService tarkovMetadataService;

    @GetMapping
    public List<SkillResponse> getSkills(@RequestParam String mode, @RequestParam List<String> ids,
                                         @RequestParam(required = false) String lang) {
        GameMode gameMode = GameMode.valueOf(mode.toUpperCase());
        return skillService.getSkills(gameMode, ids, tarkovMetadataService.resolveLanguage(lang));
    }
}