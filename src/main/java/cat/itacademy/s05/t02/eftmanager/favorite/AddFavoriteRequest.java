package cat.itacademy.s05.t02.eftmanager.favorite;

import jakarta.validation.constraints.NotBlank;

public record AddFavoriteRequest(@NotBlank String itemId) {}