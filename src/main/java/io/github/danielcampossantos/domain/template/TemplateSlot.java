package io.github.danielcampossantos.domain.template;

public record TemplateSlot(
        String slotId,
        String label,
        String shapeName,
        double x,
        double y,
        double width,
        double height,
        boolean required,
        int maxImages,
        SlotFitMode fitMode
) {

    @Override
    public String toString() {
        return label;
    }
}