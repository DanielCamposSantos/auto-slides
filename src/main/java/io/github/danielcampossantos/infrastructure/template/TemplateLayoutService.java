package io.github.danielcampossantos.infrastructure.template;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.danielcampossantos.domain.selection.SelectionAssignment;
import io.github.danielcampossantos.domain.selection.SelectionDestination;
import io.github.danielcampossantos.domain.template.TemplateLayout;
import io.github.danielcampossantos.domain.template.TemplateSlide;
import io.github.danielcampossantos.domain.template.TemplateSlot;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Objects;

public final class TemplateLayoutService {

    private static final String CONFIG_PATH = "/config/template-layout.json";

    private final ObjectMapper objectMapper = new ObjectMapper();

    private TemplateLayout layout;

    public TemplateLayout load() throws IOException {
        if (layout != null) {
            return layout;
        }

        try (InputStream input = Objects.requireNonNull(
                TemplateLayoutService.class.getResourceAsStream(CONFIG_PATH),
                "Configuração do template não encontrada: " + CONFIG_PATH
        )) {
            layout = objectMapper.readValue(input, TemplateLayout.class);

            return layout;
        }
    }

    public List<TemplateSlide> getSlides() throws IOException {
        return load().slides();
    }

    public SelectionDestination createDestination(TemplateSlide slide, TemplateSlot slot) {
        return new SelectionDestination(
                slide.slideId(),
                slide.slideNumber(),
                1,
                slide.title(),
                slot.slotId(),
                slot.label(),
                slot.x(),
                slot.y(),
                slot.width(),
                slot.height(),
                slot.fitMode()
        );
    }

    public SelectionDestination resolveDestination(
            SelectionDestination requestedDestination,
            List<SelectionAssignment> assignments
    ) throws IOException {
        TemplateSlide slide = findSlide(requestedDestination.slideId());
        TemplateSlot slot = findSlot(slide, requestedDestination.slotId());

        int copyNumber = resolveAvailableCopyNumber(slide, slot, assignments);

        return new SelectionDestination(
                slide.slideId(),
                slide.slideNumber(),
                copyNumber,
                slide.title(),
                slot.slotId(),
                slot.label(),
                slot.x(),
                slot.y(),
                slot.width(),
                slot.height(),
                slot.fitMode()
        );
    }

    public boolean isAvailable(
            TemplateSlide slide,
            TemplateSlot slot,
            List<SelectionAssignment> assignments
    ) {
        int copyNumber = resolveAvailableCopyNumber(slide, slot, assignments);

        return copyNumber == 1;
    }

    private int resolveAvailableCopyNumber(
            TemplateSlide slide,
            TemplateSlot slot,
            List<SelectionAssignment> assignments
    ) {
        int copyNumber = 1;

        while (true) {
            int currentCopy = copyNumber;

            long usageCount = assignments.stream()
                    .map(SelectionAssignment::destination)
                    .filter(destination -> destination.slideId().equals(slide.slideId()))
                    .filter(destination -> destination.slotId().equals(slot.slotId()))
                    .filter(destination -> destination.slideCopyNumber() == currentCopy)
                    .count();

            if (usageCount < slot.maxImages()) {
                return copyNumber;
            }

            copyNumber++;
        }
    }

    private TemplateSlide findSlide(String slideId) throws IOException {
        return getSlides()
                .stream()
                .filter(slide -> slide.slideId().equals(slideId))
                .findFirst()
                .orElseThrow(() -> new IOException("Slide não encontrado no layout: " + slideId));
    }

    private TemplateSlot findSlot(TemplateSlide slide, String slotId) throws IOException {
        return slide.slots()
                .stream()
                .filter(slot -> slot.slotId().equals(slotId))
                .findFirst()
                .orElseThrow(() -> new IOException("Espaço não encontrado no layout: " + slotId));
    }
}