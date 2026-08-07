package io.github.danielcampossantos.domain.template;

import java.util.List;

public record TemplateLayout(
        String templateId,
        int version,
        String name,
        String templatePath,
        double slideWidth,
        double slideHeight,
        List<TemplateSlide> slides
) {

    public TemplateLayout {
        slides = List.copyOf(slides);
    }
}