package io.github.danielcampossantos.model;

import java.io.File;

public record PdfFileItem(File file) {

    public String fileName() {
        return file.getName();
    }

}