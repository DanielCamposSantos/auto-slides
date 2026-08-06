package io.github.danielcampossantos.domain.selection;

import java.util.List;

public record PdfCropConfig(
        int pdfNumber,
        String fileName,
        List<PageCropConfig> pages
) {
}