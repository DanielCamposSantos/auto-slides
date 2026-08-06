package io.github.danielcampossantos.service;

import io.github.danielcampossantos.model.PdfFileItem;
import javafx.stage.FileChooser;
import javafx.stage.Window;

import java.io.File;
import java.util.List;

public class FileImportService {

    private static final FileChooser.ExtensionFilter PDF_FILTER =
            new FileChooser.ExtensionFilter(
                    "Arquivos PDF",
                    "*.pdf"
            );

    public List<PdfFileItem> importPdfFiles(Window window) {

        FileChooser chooser = new FileChooser();

        chooser.setTitle("Selecionar arquivos PDF");

        chooser.getExtensionFilters().add(PDF_FILTER);

        List<File> files = chooser.showOpenMultipleDialog(window);

        if (files == null) {

            return List.of();

        }

        return files.stream()
                .map(PdfFileItem::new)
                .toList();

    }

}