package io.github.danielcampossantos.ui.controller;

import io.github.danielcampossantos.model.PresentationSlideItem;
import io.github.danielcampossantos.service.ApplicationService;
import io.github.danielcampossantos.service.PopupService;
import io.github.danielcampossantos.service.TemplatePreferencesService;
import io.github.danielcampossantos.ui.navigation.SceneManager;
import io.github.danielcampossantos.ui.navigation.SceneType;
import io.github.danielcampossantos.ui.view.PresentationSlideCard;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import lombok.extern.log4j.Log4j2;
import org.jspecify.annotations.NonNull;

import java.io.File;
import java.nio.file.Path;
import java.util.*;

@Log4j2
public final class PresentationPreviewController {

    private final TemplatePreferencesService templatePreferencesService = TemplatePreferencesService.getInstance();

    private final List<PresentationSlideItem> slides = new ArrayList<>();

    private final Map<PresentationSlideItem, PresentationSlideCard> slideCards = new LinkedHashMap<>();

    private final Deque<RemovedSlide> removalHistory = new ArrayDeque<>();

    @FXML
    private StackPane rootPane;

    @FXML
    private Label templateNameLabel;

    @FXML
    private Label templatePathLabel;

    @FXML
    private Label slideCounterLabel;

    @FXML
    private Label cropCounterLabel;

    @FXML
    private ScrollPane slidesScrollPane;

    @FXML
    private VBox slidesContainer;

    @FXML
    private VBox emptyState;

    @FXML
    private Button changeTemplateButton;

    @FXML
    private Button undoButton;

    @FXML
    private Button finishButton;

    private Path selectedTemplate;

    @FXML
    private void initialize() {
        initializeKeyboardShortcuts();
        loadSavedTemplate();
        loadPlaceholderSlides();
        renderSlides();
        updateView();
    }

    private void initializeKeyboardShortcuts() {
        rootPane.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (event.isControlDown() && event.getCode() == KeyCode.Z) {
                onUndo();
                event.consume();
            }
        });
    }

    private void loadSavedTemplate() {
        Optional<Path> savedTemplate = templatePreferencesService.getTemplate();

        if (savedTemplate.isPresent()) {
            selectedTemplate = savedTemplate.get();
            updateTemplateInformation();
            return;
        }

        clearTemplateInformation();
    }

    private void loadPlaceholderSlides() {
        slides.clear();

        slides.add(new PresentationSlideItem(
                1,
                "Visão geral",
                "Apresentação inicial dos dados processados."
        ));

        slides.add(new PresentationSlideItem(
                2,
                "Indicadores principais",
                "Espaço reservado para os primeiros recortes gerados."
        ));

        slides.add(new PresentationSlideItem(
                3,
                "Análise comparativa",
                "Comparação visual entre os resultados selecionados."
        ));

        slides.add(new PresentationSlideItem(
                4,
                "Distribuição dos resultados",
                "Organização dos dados extraídos das páginas dos PDFs."
        ));

        slides.add(new PresentationSlideItem(
                5,
                "Detalhamento",
                "Visualização detalhada das imagens selecionadas."
        ));

        slides.add(new PresentationSlideItem(
                6,
                "Conclusões",
                "Resumo visual da apresentação gerada."
        ));
    }

    private void renderSlides() {
        slidesContainer.getChildren().clear();
        slideCards.clear();

        for (PresentationSlideItem slide : slides) {
            PresentationSlideCard card = new PresentationSlideCard(slide, this::removeSlide);

            slideCards.put(slide, card);
            slidesContainer.getChildren().add(card);
        }
    }

    private void removeSlide(PresentationSlideItem slide) {
        PresentationSlideCard card = slideCards.remove(slide);

        if (card == null) {
            return;
        }

        int index = slides.indexOf(slide);

        slides.remove(slide);
        slidesContainer.getChildren().remove(card);

        removalHistory.push(new RemovedSlide(slide, index));

        updateView();

        log.info("Slide {} removido visualmente.", slide.slideNumber());
    }

    @FXML
    private void onUndo() {
        if (removalHistory.isEmpty()) {
            return;
        }

        RemovedSlide removedSlide = getRemovedSlide();

        log.info("Remoção do slide {} desfeita.", removedSlide.slide().slideNumber());
    }


    @FXML
    private void onUndoAll() {
        if (removalHistory.isEmpty()) {
            return;
        }

        for (var _ : removalHistory) {
            getRemovedSlide();
        }


        log.info("Remoção de {} slides desfeita.", removalHistory.size());
    }

    private @NonNull RemovedSlide getRemovedSlide() {
        RemovedSlide removedSlide = removalHistory.pop();

        int insertionIndex = Math.min(removedSlide.index(), slides.size());

        slides.add(insertionIndex, removedSlide.slide());

        PresentationSlideCard card = new PresentationSlideCard(removedSlide.slide(), this::removeSlide);

        slideCards.put(removedSlide.slide(), card);
        slidesContainer.getChildren().add(insertionIndex, card);

        updateView();
        return removedSlide;
    }


    @FXML
    private void onChooseTemplate() {
        FileChooser fileChooser = new FileChooser();

        fileChooser.setTitle("Selecionar template do PowerPoint");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter(
                        "Apresentações do PowerPoint",
                        "*.pptx",
                        "*.ppt"
                )
        );

        if (selectedTemplate != null && selectedTemplate.getParent() != null) {
            File parentDirectory = selectedTemplate.getParent().toFile();

            if (parentDirectory.isDirectory()) {
                fileChooser.setInitialDirectory(parentDirectory);
            }
        }

        File selectedFile = fileChooser.showOpenDialog(rootPane.getScene().getWindow());

        if (selectedFile == null) {
            return;
        }

        selectedTemplate = selectedFile.toPath();

        templatePreferencesService.saveTemplate(selectedTemplate);

        updateTemplateInformation();

        PopupService.getInstance().success(
                "Template selecionado",
                "O arquivo será utilizado como modelo para as próximas apresentações."
        );
    }

    private void updateTemplateInformation() {
        templateNameLabel.setText(selectedTemplate.getFileName().toString());
        templatePathLabel.setText(selectedTemplate.toAbsolutePath().toString());
        changeTemplateButton.setText("Trocar template");
    }

    private void clearTemplateInformation() {
        templateNameLabel.setText("Nenhum template selecionado");
        templatePathLabel.setText("Selecione um arquivo PowerPoint para continuar.");
        changeTemplateButton.setText("Selecionar template");
    }

    private void updateView() {
        int slideCount = slides.size();
        int cropCount = getCropCount();

        slideCounterLabel.setText(slideCount == 1 ? "1 slide na apresentação" : slideCount + " slides na apresentação");
        cropCounterLabel.setText(cropCount == 1 ? "1 imagem processada" : cropCount + " imagens processadas");

        undoButton.setDisable(removalHistory.isEmpty());
        finishButton.setDisable(selectedTemplate == null || slides.isEmpty());

        boolean empty = slides.isEmpty();

        emptyState.setVisible(empty);
        emptyState.setManaged(empty);

        slidesContainer.setVisible(!empty);
        slidesContainer.setManaged(!empty);
    }

    private int getCropCount() {
        var workspace = ApplicationService.getInstance().getWorkspace();

        if (workspace == null || workspace.getTemporaryDirectory() == null) {
            return 0;
        }

        Path cropsDirectory = workspace.getTemporaryDirectory().resolve("crops");

        if (!cropsDirectory.toFile().isDirectory()) {
            return 0;
        }

        try (var files = java.nio.file.Files.walk(cropsDirectory)) {
            return (int) files
                    .filter(java.nio.file.Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().toLowerCase().endsWith(".png"))
                    .count();
        } catch (Exception exception) {
            log.error("Não foi possível contar as imagens recortadas.", exception);
            return 0;
        }
    }

    @FXML
    private void onBack() {
        SceneManager.getInstance().show(SceneType.AREA_SELECTION);
    }

    @FXML
    private void onFinish() {
        PopupService.getInstance().information(
                "Montagem da apresentação",
                "A integração com o PowerPoint será implementada na próxima etapa."
        );
    }

    private record RemovedSlide(
            PresentationSlideItem slide,
            int index
    ) {
    }
}