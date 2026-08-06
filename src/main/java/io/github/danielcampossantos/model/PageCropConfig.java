package io.github.danielcampossantos.model;

import java.util.List;

public record PageCropConfig(
        int pageNumber,
        String sourceImage,
        List<CropAreaConfig> selections
) {
}