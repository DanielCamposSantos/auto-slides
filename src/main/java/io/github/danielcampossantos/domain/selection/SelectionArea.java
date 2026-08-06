package io.github.danielcampossantos.domain.selection;

import io.github.danielcampossantos.domain.pdf.PdfPage;

import java.util.UUID;

public record SelectionArea(
        UUID id,
        PdfPage page,
        double x,
        double y,
        double width,
        double height,
        double viewportWidth,
        double viewportHeight
) {
}
