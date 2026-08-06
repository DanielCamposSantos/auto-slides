package io.github.danielcampossantos.model;

import java.util.List;

public record TemplateSlide(
        String slideId,
        int slideNumber,
        String title,
        List<TemplateSlot> slots
) {

    public TemplateSlide {
        slots = List.copyOf(slots);
    }

    @Override
    public String toString() {
        return "Slide %d — %s".formatted(slideNumber, title);
    }

}