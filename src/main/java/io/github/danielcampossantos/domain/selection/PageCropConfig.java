package io.github.danielcampossantos.domain.selection;

import java.util.List;

public record PageCropConfig(
        int pageNumber,
        String sourceImage,
        List<CropAreaConfig> selections
) {

    public PageCropConfig {
        selections = List.copyOf(selections);
    }
}
