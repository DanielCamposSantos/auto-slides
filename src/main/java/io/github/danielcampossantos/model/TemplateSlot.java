package io.github.danielcampossantos.model;

public record TemplateSlot(
        String slotId,
        String label,
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