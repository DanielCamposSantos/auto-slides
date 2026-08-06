package io.github.danielcampossantos.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.danielcampossantos.model.SelectionAssignment;
import io.github.danielcampossantos.model.SelectionDestination;
import io.github.danielcampossantos.model.TemplateLayout;
import io.github.danielcampossantos.model.TemplateSlide;
import io.github.danielcampossantos.model.TemplateSlot;

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

    public boolean isAvailable(
            TemplateSlide slide,
            TemplateSlot slot,
            List<SelectionAssignment> assignments
    ) {
        long usageCount = assignments.stream()
                .map(SelectionAssignment::destination)
                .filter(destination -> destination.slideId().equals(slide.slideId()))
                .filter(destination -> destination.slotId().equals(slot.slotId()))
                .count();

        return usageCount < slot.maxImages();
    }

    public SelectionDestination createDestination(TemplateSlide slide, TemplateSlot slot) {
        return new SelectionDestination(
                slide.slideId(),
                slide.slideNumber(),
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
}