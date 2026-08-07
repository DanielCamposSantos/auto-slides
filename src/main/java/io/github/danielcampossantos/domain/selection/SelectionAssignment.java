package io.github.danielcampossantos.domain.selection;

import io.github.danielcampossantos.domain.pdf.PdfPage;

import java.util.Comparator;
import java.util.Objects;
import java.util.UUID;

public record SelectionAssignment(
        SelectionArea area,
        SelectionDestination destination,
        int selectionOrder
) {

    private static final Comparator<SelectionAssignment> ORDER = Comparator
            .comparingInt((SelectionAssignment assignment) -> assignment.page().pdfNumber())
            .thenComparingInt(assignment -> assignment.page().pageNumber())
            .thenComparingInt(SelectionAssignment::selectionOrder);

    public SelectionAssignment {
        Objects.requireNonNull(area);
        Objects.requireNonNull(destination);

        if (selectionOrder < 1) {
            throw new IllegalArgumentException("A ordem da seleção deve ser maior que zero.");
        }
    }

    public static Comparator<SelectionAssignment> order() {
        return ORDER;
    }

    public UUID id() {
        return area.id();
    }

    public PdfPage page() {
        return area.page();
    }
}
