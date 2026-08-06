package io.github.danielcampossantos.model;

import java.util.UUID;

public record SelectionArea(
        UUID id,
        PdfPage page,
        double x,
        double y,
        double width,
        double height
) {
}