package io.github.danielcampossantos.model;

import java.nio.file.Path;

public record PdfPage(
        int pdfNumber,
        int pageNumber,
        Path imagePath
) {
}