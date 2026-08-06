package io.github.danielcampossantos.ui.home;

import io.github.danielcampossantos.application.workspace.ApplicationService;
import io.github.danielcampossantos.domain.pdf.PdfFileItem;
import io.github.danielcampossantos.infrastructure.pdf.FileImportService;
import io.github.danielcampossantos.infrastructure.template.TemplatePreferencesService;
import io.github.danielcampossantos.ui.common.popup.PopupService;
import io.github.danielcampossantos.ui.navigation.Reloadable;
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
import javafx.scene.layout.StackPane;
import lombok.extern.log4j.Log4j2;
import org.jspecify.annotations.NonNull;
import org.kordamp.ikonli.javafx.FontIcon;
import org.kordamp.ikonli.materialdesign2.MaterialDesignC;

import java.io.IOException;
import java.util.List;

@Log4j2
public final class HomeController implements Reloadable {

    private final FileImportService fileImportService = new FileImportService();

    private final ObservableList<PdfFileItem> selectedFiles = FXCollections.observableArrayList();

    private final ApplicationService applicationService = ApplicationService.getInstance();

    private final TemplatePreferencesService templatePreferencesService = TemplatePreferencesService.getInstance();

    @FXML
    private StackPane dropArea;

    @FXML
    private ListView<PdfFileItem> pdfListView;

    @FXML
    private Label pdfCounterLabel;

    @FXML
    private Label templateStatusLabel;

    @FXML
    private Button clearSelectionButton;

    @FXML
    private Button continueButton;

    @FXML
    private Button settingsButton;

    @FXML
    private void initialize() {
        FontIcon settingsIcon = new FontIcon(MaterialDesignC.COG);

        settingsIcon.setIconSize(22);

        settingsButton.setGraphic(settingsIcon);

        pdfListView.setItems(selectedFiles);
        pdfListView.setCellFactory(list -> new PdfFileCell(this::removePdf));

        reload();
    }

    @Override
    public void reload() {
        updateView();
    }

    @FXML
    private void onSelectPdf() {
        List<PdfFileItem> importedFiles = fileImportService.importPdfFiles(
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
        if (!templatePreferencesService.hasTemplate()) {
            PopupService.getInstance().warning(
                    "Template não configurado",
                    "Abra as configurações e selecione um template PowerPoint antes de continuar."
            );

            return;
        }

        if (selectedFiles.isEmpty()) {
            return;
        }

        try {
            applicationService.startSelection(
                    selectedFiles.stream()
                            .map(PdfFileItem::file)
                            .toList()
            );

            SceneManager.getInstance().show(SceneType.AREA_SELECTION);
        } catch (IOException exception) {
            log.error("Não foi possível iniciar a seleção.", exception);

            PopupService.getInstance().error(
                    "Erro ao processar os PDFs",
                    exception.getMessage() == null
                            ? "Não foi possível preparar os arquivos selecionados."
                            : exception.getMessage()
            );
        }
    }

    @FXML
    private void onOpenSettings() {
        SceneManager.getInstance().show(SceneType.SETTINGS);
    }

    @FXML
    private void onDragEntered(DragEvent event) {
        if (!dropArea.getStyleClass().contains(getStyleClass())) {
            dropArea.getStyleClass().add(getStyleClass());
        }
    }

    private static @NonNull String getStyleClass() {
        return "drop-area-hover";
    }

    @FXML
    private void onDragExited(DragEvent event) {
        dropArea.getStyleClass().remove(getStyleClass());
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
            event.consume();
            return;
        }

        List<PdfFileItem> files = dragboard.getFiles()
                .stream()
                .filter(file -> file.getName().toLowerCase().endsWith(".pdf"))
                .map(PdfFileItem::new)
                .toList();

        addFiles(files);

        dropArea.getStyleClass().remove(getStyleClass());

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
                .noneMatch(existing -> existing.file().equals(item.file()));
    }

    private void removePdf(PdfFileItem item) {
        selectedFiles.remove(item);

        updateView();
    }

    private void updateView() {
        int total = selectedFiles.size();
        boolean hasTemplate = templatePreferencesService.hasTemplate();

        clearSelectionButton.setDisable(total == 0);
        continueButton.setDisable(total == 0 || !hasTemplate);

        updatePdfCounter(total);
        updateTemplateStatus(hasTemplate);
    }

    private void updatePdfCounter(int total) {
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

    private void updateTemplateStatus(boolean hasTemplate) {
        templateStatusLabel.getStyleClass().removeAll(
                "template-status-ready",
                "template-status-missing"
        );

        if (hasTemplate) {
            String templateName = templatePreferencesService.getTemplate()
                    .map(path -> path.getFileName().toString())
                    .orElse("Template configurado");

            templateStatusLabel.setText("Template: " + templateName);
            templateStatusLabel.getStyleClass().add("template-status-ready");

            return;
        }

        templateStatusLabel.setText("Configure um template para continuar");
        templateStatusLabel.getStyleClass().add("template-status-missing");
    }
}