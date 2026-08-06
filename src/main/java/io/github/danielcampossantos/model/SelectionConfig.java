package io.github.danielcampossantos.model;

import java.util.List;

public record SelectionConfig(
        List<PdfCropConfig> pdfs
) {
}