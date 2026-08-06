package io.github.danielcampossantos.domain.selection;

import io.github.danielcampossantos.domain.template.SlotFitMode;

public record SelectionDestination(
        String slideId,
        int slideNumber,
        String slideTitle,
        String slotId,
        String slotLabel,
        double x,
        double y,
        double width,
        double height,
        SlotFitMode fitMode
) {
}
