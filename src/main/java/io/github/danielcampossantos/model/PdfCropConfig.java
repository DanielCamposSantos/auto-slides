package io.github.danielcampossantos.model;

import java.util.List;

public record PdfCropConfig(
        int pdfNumber,
        String fileName,
        List<PageCropConfig> pages
) {
}