package io.github.danielcampossantos.domain.selection;

import io.github.danielcampossantos.domain.pdf.PdfPage;

import java.util.Objects;
import java.util.UUID;

public record SelectionAssignment(
        SelectionArea area,
        SelectionDestination destination
) {

    public SelectionAssignment {
        Objects.requireNonNull(area);
        Objects.requireNonNull(destination);
    }

    public UUID id() {
        return area.id();
    }

    public PdfPage page() {
        return area.page();
    }
}
