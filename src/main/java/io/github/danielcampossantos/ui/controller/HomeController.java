package io.github.danielcampossantos.ui.controller;

import io.github.danielcampossantos.model.PdfFileItem;
import io.github.danielcampossantos.service.ApplicationService;
import io.github.danielcampossantos.service.FileImportService;
import io.github.danielcampossantos.ui.cell.PdfFileCell;
import io.github.danielcampossantos.ui.navigation.SceneManager;
import io.github.danielcampossantos.ui.navigation.SceneType;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.VBox;
import lombok.extern.log4j.Log4j2;

import java.io.IOException;
import java.util.List;

@Log4j2
public class HomeController {

    private final FileImportService fileImportService = new FileImportService();

    private final ObservableList<PdfFileItem> selectedFiles = FXCollections.observableArrayList();

    private final ApplicationService applicationService = ApplicationService.getInstance();

    @FXML
    private VBox dropArea;

    @FXML
    private ListView<PdfFileItem> pdfListView;

    @FXML
    private Label pdfCounterLabel;

    @FXML
    private Button clearSelectionButton;

    @FXML
    private Button continueButton;

    @FXML
    public void initialize() {

        pdfListView.setItems(selectedFiles);

        pdfListView.setCellFactory(list ->
                new PdfFileCell(this::removePdf));

        updateView();

    }

    @FXML
    private void onSelectPdf() {

        List<PdfFileItem> importedFiles =
                fileImportService.importPdfFiles(
                        dropArea.getScene().getWindow()
                );

        addFiles(importedFiles);

    }

    @FXML
    private void onClearSelection() {

        applicationService.clearWorkspace();

        selectedFiles.clear();

        updateView();

    }

    @FXML
    private void onContinue() {

        if (selectedFiles.isEmpty()) {

            return;

        }

        try {

            applicationService.startSelection(
                    selectedFiles.stream()
                            .map(PdfFileItem::file)
                            .toList()

            );

            SceneManager
                    .getInstance()
                    .show(SceneType.AREA_SELECTION);

        } catch (IOException exception) {

            log.error(exception);

        }

    }

    @FXML
    private void onDragEntered(DragEvent event) {

        dropArea.getStyleClass().add("drop-area-hover");

    }

    @FXML
    private void onDragExited(DragEvent event) {

        dropArea.getStyleClass().remove("drop-area-hover");

    }

    @FXML
    private void onDragOver(DragEvent event) {

        Dragboard dragboard = event.getDragboard();

        if (dragboard.hasFiles()) {

            event.acceptTransferModes(TransferMode.COPY);

        }

        event.consume();

    }

    @FXML
    private void onDragDropped(DragEvent event) {

        Dragboard dragboard = event.getDragboard();

        if (!dragboard.hasFiles()) {

            event.setDropCompleted(false);

            return;

        }

        List<PdfFileItem> files = dragboard.getFiles()
                .stream()
                .filter(file ->
                        file.getName()
                                .toLowerCase()
                                .endsWith(".pdf"))
                .map(PdfFileItem::new)
                .toList();

        addFiles(files);

        event.setDropCompleted(true);

        event.consume();

    }

    private void addFiles(List<PdfFileItem> files) {

        files.stream()
                .filter(this::isNotSelected)
                .forEach(selectedFiles::add);

        updateView();

    }

    private boolean isNotSelected(PdfFileItem item) {

        return selectedFiles.stream()
                .noneMatch(existing ->
                        existing.file()
                                .equals(item.file()));

    }

    private void removePdf(PdfFileItem item) {

        selectedFiles.remove(item);

        updateView();

    }

    private void updateView() {

        int total = selectedFiles.size();

        continueButton.setDisable(total == 0);

        clearSelectionButton.setDisable(total == 0);

        if (total == 0) {

            pdfCounterLabel.setText("Nenhum PDF selecionado");

            return;

        }

        if (total == 1) {

            pdfCounterLabel.setText("1 PDF selecionado");

            return;

        }

        pdfCounterLabel.setText(total + " PDFs selecionados");

    }

}