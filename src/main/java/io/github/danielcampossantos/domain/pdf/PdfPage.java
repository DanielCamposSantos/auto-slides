package io.github.danielcampossantos.domain.pdf;

import java.nio.file.Path;

public record PdfPage(
        int pdfNumber,
        int pageNumber,
        Path imagePath
) {
}