package io.github.danielcampossantos.domain.template;

import java.nio.file.Path;

public record PresentationTemplateInfo(
        Path path,
        String fileName,
        int slideCount,
        double slideWidth,
        double slideHeight
) {
}