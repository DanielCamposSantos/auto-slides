package io.github.danielcampossantos.ui.preview;

import io.github.danielcampossantos.application.workspace.ApplicationService;
import io.github.danielcampossantos.domain.template.PresentationSlideItem;
import io.github.danielcampossantos.infrastructure.template.PresentationTemplateService;
import io.github.danielcampossantos.infrastructure.template.TemplatePreferencesService;
import io.github.danielcampossantos.ui.common.popup.PopupService;
import io.github.danielcampossantos.ui.common.popup.PopupType;
import io.github.danielcampossantos.ui.navigation.SceneManager;
import io.github.danielcampossantos.ui.navigation.SceneType;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import lombok.extern.log4j.Log4j2;

import java.io.IOException;
import java.nio.file.Path;
import java.util.*;

@Log4j2
public final class PresentationPreviewController {

    private final TemplatePreferencesService templatePreferencesService = TemplatePreferencesService.getInstance();

    private final PresentationTemplateService presentationTemplateService = new PresentationTemplateService();

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
    private VBox slidesContainer;

    @FXML
    private VBox emptyState;

    @FXML
    private Button undoButton;

    @FXML
    private Button undoAllButton;

    @FXML
    private Button finishButton;

    private Path selectedTemplate;

    @FXML
    private void initialize() {
        initializeKeyboardShortcuts();

        if (!loadTemplate()) {
            return;
        }

        loadTemplateSlides();
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

    private boolean loadTemplate() {
        Optional<Path> template = templatePreferencesService.getTemplate();

        if (template.isEmpty()) {
            PopupService.getInstance().show(
                    PopupType.WARNING,
                    "Template não configurado",
                    "Selecione um template nas configurações antes de abrir esta tela.",
                    () -> SceneManager.getInstance().show(SceneType.SETTINGS)
            );

            return false;
        }

        selectedTemplate = template.get();

        templateNameLabel.setText(selectedTemplate.getFileName().toString());
        templatePathLabel.setText(selectedTemplate.toAbsolutePath().toString());

        return true;
    }

    private void loadTemplateSlides() {
        var workspace = ApplicationService.getInstance().getWorkspace();

        if (workspace == null || workspace.getTemporaryDirectory() == null) {
            PopupService.getInstance().error(
                    "Workspace indisponível",
                    "Não foi possível localizar a pasta temporária da execução."
            );

            return;
        }

        try {
            slides.clear();

            slides.addAll(
                    presentationTemplateService.readSlides(
                            selectedTemplate,
                            workspace.getTemporaryDirectory()
                    )
            );
        } catch (IOException exception) {
            log.error("Não foi possível carregar os slides do template.", exception);

            PopupService.getInstance().show(
                    PopupType.ERROR,
                    "Erro ao carregar template",
                    exception.getMessage(),
                    () -> SceneManager.getInstance().show(SceneType.SETTINGS)
            );
        }
    }

    private void renderSlides() {
        slidesContainer.getChildren().clear();
        slideCards.clear();

        for (PresentationSlideItem slide : slides) {
            PresentationSlideCard card = new PresentationSlideCard(
                    slide,
                    this::removeSlide,
                    this::duplicateSlide
            );

            slideCards.put(slide, card);
            slidesContainer.getChildren().add(card);
        }
    }

    private void duplicateSlide(PresentationSlideItem source) {
        int sourceIndex = slides.indexOf(source);

        if (sourceIndex < 0) {
            return;
        }

        PresentationSlideItem copy = presentationTemplateService.duplicate(
                source,
                sourceIndex + 2
        );

        slides.add(sourceIndex + 1, copy);

        renumberSlides();

        renderSlides();
        updateView();

        log.info(
                "Slide {} duplicado visualmente.",
                source.slideNumber()
        );
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

        renumberSlides();
        renderSlides();
        updateView();

        log.info("Slide {} removido visualmente.", slide.slideNumber());
    }

    @FXML
    private void onUndo() {
        if (removalHistory.isEmpty()) {
            return;
        }

        restoreLastRemovedSlide();
    }

    @FXML
    private void onUndoAll() {
        while (!removalHistory.isEmpty()) {
            restoreLastRemovedSlide();
        }
    }

    private void restoreLastRemovedSlide() {
        RemovedSlide removedSlide = removalHistory.pop();
        int insertionIndex = Math.min(removedSlide.index(), slides.size());

        slides.add(insertionIndex, removedSlide.slide());

        renumberSlides();
        renderSlides();
        updateView();

        log.info(
                "Remoção do slide {} desfeita.",
                removedSlide.slide().sourceSlideNumber()
        );
    }

    private void renumberSlides() {
        List<PresentationSlideItem> renumbered = new ArrayList<>();

        for (int index = 0; index < slides.size(); index++) {
            PresentationSlideItem slide = slides.get(index);

            renumbered.add(
                    new PresentationSlideItem(
                            slide.id(),
                            index + 1,
                            slide.sourceSlideNumber(),
                            slide.copyNumber(),
                            slide.title(),
                            slide.description(),
                            slide.thumbnailPath()
                    )
            );
        }

        slides.clear();
        slides.addAll(renumbered);
    }

    private void updateView() {
        int slideCount = slides.size();
        int cropCount = getCropCount();

        slideCounterLabel.setText(
                slideCount == 1
                        ? "1 slide na apresentação"
                        : slideCount + " slides na apresentação"
        );

        cropCounterLabel.setText(
                cropCount == 1
                        ? "1 imagem processada"
                        : cropCount + " imagens processadas"
        );

        boolean hasHistory = !removalHistory.isEmpty();
        boolean empty = slides.isEmpty();

        undoButton.setDisable(!hasHistory);
        undoAllButton.setDisable(!hasHistory);
        finishButton.setDisable(empty);

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
        } catch (IOException exception) {
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
                "Download da apresentação",
                "O botão já representa o fluxo final. A criação do PPTX com Apache POI será conectada na próxima etapa."
        );
    }

    private record RemovedSlide(
            PresentationSlideItem slide,
            int index
    ) {
    }
}