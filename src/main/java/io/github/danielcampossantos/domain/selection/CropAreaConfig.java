package io.github.danielcampossantos.domain.selection;

import java.util.UUID;

public record CropAreaConfig(
        UUID id,
        int x,
        int y,
        int width,
        int height,
        SelectionDestination destination
) {
}