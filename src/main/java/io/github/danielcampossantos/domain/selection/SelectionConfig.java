package io.github.danielcampossantos.domain.selection;

import java.util.List;

public record SelectionConfig(
        List<PdfCropConfig> pdfs
) {

    public SelectionConfig {
        pdfs = List.copyOf(pdfs);
    }
}
