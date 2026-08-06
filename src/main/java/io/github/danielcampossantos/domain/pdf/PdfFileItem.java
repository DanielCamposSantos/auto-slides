package io.github.danielcampossantos.domain.pdf;

import java.io.File;

public record PdfFileItem(File file) {

    public String fileName() {
        return file.getName();
    }

}