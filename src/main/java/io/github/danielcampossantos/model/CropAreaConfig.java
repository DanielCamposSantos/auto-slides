package io.github.danielcampossantos.model;

import java.util.UUID;

public record CropAreaConfig(
        UUID id,
        int x,
        int y,
        int width,
        int height
) {
}