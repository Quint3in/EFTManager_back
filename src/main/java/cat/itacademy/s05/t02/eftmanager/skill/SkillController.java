package cat.itacademy.s05.t02.eftmanager.skill;

import cat.itacademy.s05.t02.eftmanager.common.GameMode;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/skills")
@RequiredArgsConstructor
public class SkillController {

    private final SkillService itemService;

    @GetMapping
    public List<SkillResponse> getSkills(@RequestParam String mode, @RequestParam List<String> ids) {
        GameMode gameMode = GameMode.valueOf(mode.toUpperCase());
        return itemService.getSkills(gameMode, ids);
    }
}