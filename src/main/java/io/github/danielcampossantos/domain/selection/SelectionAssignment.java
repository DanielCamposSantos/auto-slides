package io.github.danielcampossantos.domain.selection;

import io.github.danielcampossantos.domain.pdf.PdfPage;

import java.util.Objects;
import java.util.UUID;

public record SelectionAssignment(
        SelectionArea area,
        SelectionDestination destination,
        int selectionOrder
) {

    public SelectionAssignment {
        Objects.requireNonNull(area);
        Objects.requireNonNull(destination);

        if (selectionOrder < 1) {
            throw new IllegalArgumentException("A ordem da seleção deve ser maior que zero.");
        }
    }

    public UUID id() {
        return area.id();
    }

    public PdfPage page() {
        return area.page();
    }
}