package io.github.danielcampossantos.infrastructure.template;

import io.github.danielcampossantos.domain.selection.SelectionAssignment;
import io.github.danielcampossantos.domain.selection.SelectionDestination;
import io.github.danielcampossantos.domain.template.TemplateLayout;
import io.github.danielcampossantos.domain.template.TemplateSlide;
import io.github.danielcampossantos.domain.template.TemplateSlot;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;

public final class TemplateLayoutService {

    private final TemplatePreferencesService templatePreferencesService = TemplatePreferencesService.getInstance();

    private final TemplateLayoutStorageService templateLayoutStorageService = new TemplateLayoutStorageService();

    private TemplateLayout layout;

    private Path loadedLayoutPath;

    public TemplateLayout load() throws IOException {
        Path layoutPath = templatePreferencesService.getLayoutPath()
                .orElseThrow(() -> new IOException(
                        "Nenhuma configuração de template foi selecionada."
                ));

        if (layout != null && layoutPath.equals(loadedLayoutPath)) {
            return layout;
        }

        layout = templateLayoutStorageService.read(layoutPath);
        loadedLayoutPath = layoutPath;

        return layout;
    }

    public void reload() throws IOException {
        layout = null;
        loadedLayoutPath = null;

        load();
    }

    public List<TemplateSlide> getSlides() throws IOException {
        return load().slides();
    }

    public SelectionDestination createDestination(
            TemplateSlide slide,
            TemplateSlot slot
    ) {
        return new SelectionDestination(
                slide.slideId(),
                slide.slideNumber(),
                1,
                slide.title(),
                slot.slotId(),
                slot.label(),
                slot.shapeName(),
                slot.x(),
                slot.y(),
                slot.width(),
                slot.height(),
                slot.fitMode()
        );
    }

    public SelectionDestination resolveDestination(
            SelectionDestination requested,
            List<SelectionAssignment> existingAssignments
    ) {
        List<SelectionAssignment> orderedAssignments = existingAssignments.stream()
                .sorted(
                        Comparator.comparingInt(
                                        (SelectionAssignment assignment) ->
                                                assignment.page().pdfNumber()
                                )
                                .thenComparingInt(
                                        assignment ->
                                                assignment.page().pageNumber()
                                )
                                .thenComparingInt(
                                        SelectionAssignment::selectionOrder
                                )
                )
                .toList();

        long previousUses = orderedAssignments.stream()
                .map(SelectionAssignment::destination)
                .filter(destination ->
                        destination.slideId().equals(requested.slideId())
                )
                .filter(destination ->
                        destination.slotId().equals(requested.slotId())
                )
                .count();

        int slideInstance = Math.toIntExact(previousUses) + 1;

        return new SelectionDestination(
                requested.slideId(),
                requested.sourceSlideNumber(),
                slideInstance,
                requested.slideTitle(),
                requested.slotId(),
                requested.slotLabel(),
                requested.slotShapeName(),
                requested.x(),
                requested.y(),
                requested.width(),
                requested.height(),
                requested.fitMode()
        );
    }

    public boolean isAvailable(
            TemplateSlide slide,
            TemplateSlot slot,
            List<SelectionAssignment> assignments
    ) {
        return true;
    }
}