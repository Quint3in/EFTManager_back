package cat.itacademy.s05.t02.eftmanager.controller;

import cat.itacademy.s05.t02.eftmanager.dto.TraderResponse;
import cat.itacademy.s05.t02.eftmanager.service.GameMode;
import cat.itacademy.s05.t02.eftmanager.service.TradersService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/traders")
@RequiredArgsConstructor
public class TraderController {

    private final TradersService tradersService;

    @GetMapping
    public List<TraderResponse> getItems(@RequestParam String mode, @RequestParam List<String> ids) {
        GameMode gameMode = GameMode.valueOf(mode.toUpperCase());
        return tradersService.getTraders(gameMode, ids);
    }
}