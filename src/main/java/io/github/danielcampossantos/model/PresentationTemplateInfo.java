package io.github.danielcampossantos.model;

import java.nio.file.Path;

public record PresentationTemplateInfo(
        Path path,
        String fileName,
        int slideCount,
        double slideWidth,
        double slideHeight
) {
}