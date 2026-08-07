package io.github.danielcampossantos.ui.preview;

import io.github.danielcampossantos.application.workspace.ApplicationService;
import io.github.danielcampossantos.application.workspace.Workspace;
import io.github.danielcampossantos.domain.template.PresentationSlideItem;
import io.github.danielcampossantos.infrastructure.template.PresentationGenerationService;
import io.github.danielcampossantos.infrastructure.template.PresentationTemplateService;
import io.github.danielcampossantos.ui.common.popup.PopupService;
import io.github.danielcampossantos.ui.navigation.SceneManager;
import io.github.danielcampossantos.ui.navigation.SceneType;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import lombok.extern.log4j.Log4j2;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;

@Log4j2
public final class PresentationPreviewController {

    private final PresentationTemplateService presentationTemplateService = new PresentationTemplateService();
    private final PresentationGenerationService presentationGenerationService = new PresentationGenerationService();
    private final List<PresentationSlideItem> slides = new ArrayList<>();
    private final Deque<RemovedSlide> removalHistory = new ArrayDeque<>();
    private final Set<Integer> removedSlideIndexes = new LinkedHashSet<>();

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

    private Workspace workspace;
    private Path generatedPresentationPath;

    @FXML
    private void initialize() {
        initializeKeyboardShortcuts();
        loadGeneratedPresentation();
    }

    private void initializeKeyboardShortcuts() {
        rootPane.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (event.isControlDown() && event.getCode() == KeyCode.Z) {
                onUndo();
                event.consume();
            }
        });
    }

    private void loadGeneratedPresentation() {
        workspace = ApplicationService.getInstance().getWorkspace();

        if (workspace == null || workspace.getTemporaryDirectory() == null) {
            PopupService.getInstance().error(
                    "Workspace indisponível",
                    "Não foi possível localizar os arquivos temporários da apresentação."
            );
            return;
        }

        generatedPresentationPath = workspace.getGeneratedPresentationPath();

        if (generatedPresentationPath == null) {
            PopupService.getInstance().error(
                    "Apresentação indisponível",
                    "A apresentação ainda não foi gerada. Volte para a seleção e finalize o processamento."
            );
            return;
        }

        templateNameLabel.setText(generatedPresentationPath.getFileName().toString());
        templatePathLabel.setText(generatedPresentationPath.toAbsolutePath().toString());

        try {
            slides.clear();
            slides.addAll(
                    presentationTemplateService.readSlides(
                            generatedPresentationPath,
                            workspace.getTemporaryDirectory()
                    )
            );

            renderSlides();
            updateView();
        } catch (IOException exception) {
            log.error("Não foi possível carregar o preview da apresentação.", exception);

            PopupService.getInstance().error(
                    "Erro ao gerar preview",
                    exception.getMessage() == null
                            ? "Não foi possível renderizar os slides gerados."
                            : exception.getMessage()
            );
        }
    }

    private void renderSlides() {
        slidesContainer.getChildren().clear();
        for (PresentationSlideItem slide : slides) {
            PresentationSlideCard card = new PresentationSlideCard(
                    slide,
                    this::removeSlide
            );

            slidesContainer.getChildren().add(card);
        }
    }

    private void removeSlide(PresentationSlideItem slide) {
        int index = slides.indexOf(slide);

        if (index < 0) {
            return;
        }

        slides.remove(index);
        removedSlideIndexes.add(slide.generatedSlideIndex());
        removalHistory.push(new RemovedSlide(slide, index));

        renumberSlides();
        renderSlides();
        updateView();

        log.info("Slide gerado {} removido visualmente.", slide.generatedSlideIndex() + 1);
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
        removedSlideIndexes.remove(removedSlide.slide().generatedSlideIndex());

        renumberSlides();
        renderSlides();
        updateView();

        log.info(
                "Remoção do slide gerado {} desfeita.",
                removedSlide.slide().generatedSlideIndex() + 1
        );
    }

    private void renumberSlides() {
        List<PresentationSlideItem> renumbered = new ArrayList<>();

        for (int index = 0; index < slides.size(); index++) {
            PresentationSlideItem slide = slides.get(index);

            renumbered.add(new PresentationSlideItem(
                    slide.id(),
                    index + 1,
                    slide.generatedSlideIndex(),
                    slide.title(),
                    slide.description(),
                    slide.thumbnailPath()
            ));
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
        FileChooser chooser = new FileChooser();

        chooser.setTitle("Salvar apresentação final");
        chooser.setInitialFileName("apresentacao-final.pptx");
        chooser.getExtensionFilters().setAll(
                new FileChooser.ExtensionFilter(
                        "Apresentação do PowerPoint",
                        "*.pptx"
                )
        );

        File selectedFile = chooser.showSaveDialog(
                rootPane.getScene().getWindow()
        );

        if (selectedFile == null) {
            return;
        }

        Path destinationPath = ensurePptxExtension(
                selectedFile.toPath()
        );

        try {
            presentationGenerationService.exportWithoutSlides(
                    generatedPresentationPath,
                    Set.copyOf(removedSlideIndexes),
                    destinationPath
            );

            PopupService.getInstance().success(
                    "Apresentação salva",
                    "O arquivo foi criado em:\n\n" + destinationPath.toAbsolutePath()
            );
        } catch (IOException exception) {
            log.error("Não foi possível salvar a apresentação final.", exception);

            PopupService.getInstance().error(
                    "Erro ao salvar apresentação",
                    exception.getMessage() == null
                            ? "Não foi possível gravar o arquivo PowerPoint."
                            : exception.getMessage()
            );
        }
    }

    private Path ensurePptxExtension(Path path) {
        String fileName = path.getFileName().toString();

        if (fileName.toLowerCase().endsWith(".pptx")) {
            return path;
        }

        return path.resolveSibling(fileName + ".pptx");
    }

    private record RemovedSlide(
            PresentationSlideItem slide,
            int index
    ) {
    }
}
