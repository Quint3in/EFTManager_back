package cat.itacademy.s05.t02.eftmanager.controller;

import cat.itacademy.s05.t02.eftmanager.dto.ItemResponse;
import cat.itacademy.s05.t02.eftmanager.service.GameMode;
import cat.itacademy.s05.t02.eftmanager.service.ItemService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/items")
@RequiredArgsConstructor
public class ItemController {

    private final ItemService itemService;

    @GetMapping
    public List<ItemResponse> getItems(@RequestParam String mode, @RequestParam List<String> ids) {
        GameMode gameMode = GameMode.valueOf(mode.toUpperCase());
        return itemService.getItems(gameMode, ids);
    }
}