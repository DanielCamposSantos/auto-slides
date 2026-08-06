package io.github.danielcampossantos.domain.template;

import java.nio.file.Path;
import java.util.UUID;

public record PresentationSlideItem(
        UUID id,
        int slideNumber,
        int sourceSlideNumber,
        int copyNumber,
        String title,
        String description,
        Path thumbnailPath
) {
}