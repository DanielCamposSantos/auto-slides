package io.github.danielcampossantos.domain.selection;

import java.util.UUID;

public record CropAreaConfig(
        UUID id,
        int selectionOrder,
        int x,
        int y,
        int width,
        int height,
        String outputImage,
        SelectionDestination destination
) {
}
