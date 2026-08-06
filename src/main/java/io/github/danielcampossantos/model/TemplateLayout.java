package io.github.danielcampossantos.model;

import java.util.List;

public record TemplateLayout(
        String templateId,
        int version,
        String name,
        List<TemplateSlide> slides
) {

    public TemplateLayout {
        slides = List.copyOf(slides);
    }

}