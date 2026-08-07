package io.github.danielcampossantos.infrastructure.template;

import io.github.danielcampossantos.domain.selection.SelectionAssignment;
import io.github.danielcampossantos.domain.selection.SelectionDestination;
import io.github.danielcampossantos.domain.template.TemplateLayout;
import io.github.danielcampossantos.domain.template.TemplateSlide;
import io.github.danielcampossantos.domain.template.TemplateSlot;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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
        long previousUses = existingAssignments.stream()
                .map(SelectionAssignment::destination)
                .filter(destination -> destination.slideId().equals(requested.slideId()))
                .filter(destination -> destination.slotId().equals(requested.slotId()))
                .count();

        return withInstance(
                requested,
                Math.toIntExact(previousUses) + 1
        );
    }

    public List<SelectionAssignment> normalizeAssignments(
            List<SelectionAssignment> assignments
    ) {
        List<SelectionAssignment> orderedAssignments = assignments.stream()
                .sorted(SelectionAssignment.order())
                .toList();

        Map<DestinationKey, Integer> usages = new LinkedHashMap<>();
        List<SelectionAssignment> normalized = new ArrayList<>();

        for (SelectionAssignment assignment : orderedAssignments) {
            SelectionDestination destination = assignment.destination();
            DestinationKey key = new DestinationKey(
                    destination.slideId(),
                    destination.slotId()
            );

            int instance = usages.merge(key, 1, Integer::sum);

            normalized.add(new SelectionAssignment(
                    assignment.area(),
                    withInstance(destination, instance),
                    assignment.selectionOrder()
            ));
        }

        return List.copyOf(normalized);
    }

    private SelectionDestination withInstance(
            SelectionDestination destination,
            int instance
    ) {
        return new SelectionDestination(
                destination.slideId(),
                destination.sourceSlideNumber(),
                instance,
                destination.slideTitle(),
                destination.slotId(),
                destination.slotLabel(),
                destination.slotShapeName(),
                destination.x(),
                destination.y(),
                destination.width(),
                destination.height(),
                destination.fitMode()
        );
    }

    private record DestinationKey(
            String slideId,
            String slotId
    ) {
    }
}
