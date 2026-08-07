package io.github.danielcampossantos.ui.selection;

import io.github.danielcampossantos.application.workspace.ApplicationService;
import io.github.danielcampossantos.application.workspace.Workspace;
import io.github.danielcampossantos.domain.pdf.PdfPage;
import io.github.danielcampossantos.domain.selection.SelectionArea;
import io.github.danielcampossantos.domain.selection.SelectionAssignment;
import io.github.danielcampossantos.domain.selection.SelectionDestination;
import io.github.danielcampossantos.domain.template.TemplateSlide;
import io.github.danielcampossantos.domain.template.TemplateSlot;
import io.github.danielcampossantos.infrastructure.selection.ImageService;
import io.github.danielcampossantos.infrastructure.selection.SelectionConfigService;
import io.github.danielcampossantos.infrastructure.template.TemplateLayoutService;
import io.github.danielcampossantos.ui.common.popup.PopupService;
import io.github.danielcampossantos.ui.common.popup.PopupType;
import io.github.danielcampossantos.ui.navigation.SceneManager;
import io.github.danielcampossantos.ui.navigation.SceneType;
import io.github.danielcampossantos.ui.selection.popup.DestinationSelectionPopup;
import javafx.animation.AnimationTimer;
import javafx.fxml.FXML;
import javafx.scene.Cursor;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.VBox;
import lombok.extern.log4j.Log4j2;
import org.jspecify.annotations.NonNull;

import java.io.IOException;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Log4j2
public final class AreaSelectionController {

    private static final double AUTO_SCROLL_DEAD_ZONE = 14;
    private static final double AUTO_SCROLL_SPEED_FACTOR = 5;
    private static final double AUTO_SCROLL_MAX_SPEED = 2400;
    private static final double RIGHT_DRAG_SPEED_MULTIPLIER = 2.5;

    private final SelectionConfigService selectionConfigService = new SelectionConfigService();
    private final ImageService imageService = new ImageService();
    private final TemplateLayoutService templateLayoutService = new TemplateLayoutService();
    private final DestinationSelectionPopup destinationPopup = new DestinationSelectionPopup(templateLayoutService);

    private final TreeItem<SelectionTreeNode> root = new TreeItem<>();
    private final Map<Integer, TreeItem<SelectionTreeNode>> pdfNodes = new LinkedHashMap<>();
    private final Map<PdfPage, TreeItem<SelectionTreeNode>> pageNodes = new LinkedHashMap<>();
    private final Map<UUID, TreeItem<SelectionTreeNode>> selectionNodes = new LinkedHashMap<>();
    private final Map<UUID, SelectionAssignment> assignments = new LinkedHashMap<>();
    private final Map<PdfPage, PdfPageView> pageViews = new LinkedHashMap<>();

    private final AnimationTimer autoScrollTimer = new AnimationTimer() {

        private long previousFrame;

        @Override
        public void handle(long now) {
            if (!autoScrollActive) {
                previousFrame = now;
                return;
            }

            if (previousFrame == 0) {
                previousFrame = now;
                return;
            }

            double elapsedSeconds = (now - previousFrame) / 1_000_000_000.0;
            double distance = autoScrollPointerY - autoScrollAnchorY;

            previousFrame = now;

            updateAutoScrollCursor(distance);

            if (Math.abs(distance) <= AUTO_SCROLL_DEAD_ZONE) {
                return;
            }

            double direction = Math.signum(distance);
            double effectiveDistance = Math.abs(distance) - AUTO_SCROLL_DEAD_ZONE;
            double speed = Math.min(
                    effectiveDistance * AUTO_SCROLL_SPEED_FACTOR,
                    AUTO_SCROLL_MAX_SPEED
            );

            scrollVerticallyByPixels(
                    direction * speed * elapsedSeconds
            );
        }

        private void updateAutoScrollCursor(double distance) {
            if (distance < -AUTO_SCROLL_DEAD_ZONE) {
                pagesScrollPane.setCursor(Cursor.N_RESIZE);
                return;
            }

            if (distance > AUTO_SCROLL_DEAD_ZONE) {
                pagesScrollPane.setCursor(Cursor.S_RESIZE);
                return;
            }

            pagesScrollPane.setCursor(Cursor.V_RESIZE);
        }

        private void scrollVerticallyByPixels(double pixelMovement) {
            double scrollableDistance = getVerticalScrollableDistance();

            if (scrollableDistance <= 0) {
                return;
            }

            double valueMovement = pixelMovement / scrollableDistance;
            double newValue = pagesScrollPane.getVvalue() + valueMovement;

            pagesScrollPane.setVvalue(clamp(newValue));
        }

    };

    @FXML
    private ScrollPane pagesScrollPane;

    @FXML
    private VBox pagesContainer;

    @FXML
    private TreeView<SelectionTreeNode> selectionTreeView;

    @FXML
    private Label selectionCounter;

    private Workspace workspace;
    private int selectionCount;
    private int nextSelectionOrder = 1;
    private boolean autoScrollActive;
    private double autoScrollAnchorY;
    private double autoScrollPointerY;
    private boolean rightMouseDragging;
    private double rightDragStartX;
    private double rightDragStartY;
    private double rightDragInitialHorizontalValue;
    private double rightDragInitialVerticalValue;

    @FXML
    private void initialize() {
        workspace = ApplicationService.getInstance().getWorkspace();

        selectionTreeView.setRoot(root);

        initializeMouseNavigation();
        loadPages();
        updateSelectionCounter();
    }

    private void initializeMouseNavigation() {
        pagesScrollPane.addEventFilter(
                MouseEvent.MOUSE_PRESSED,
                this::handleNavigationPressed
        );

        pagesScrollPane.addEventFilter(
                MouseEvent.MOUSE_DRAGGED,
                this::handleNavigationDragged
        );

        pagesScrollPane.addEventFilter(
                MouseEvent.MOUSE_RELEASED,
                this::handleNavigationReleased
        );

        pagesScrollPane.addEventFilter(
                MouseEvent.MOUSE_MOVED,
                this::handleNavigationMoved
        );

        pagesScrollPane.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (event.getCode() == KeyCode.ESCAPE && autoScrollActive) {
                stopAutoScroll();
                event.consume();
            }
        });

        pagesScrollPane.setFocusTraversable(true);
    }

    private void handleNavigationPressed(MouseEvent event) {
        if (event.getButton() == MouseButton.MIDDLE) {
            pagesScrollPane.requestFocus();

            if (autoScrollActive) {
                stopAutoScroll();
            } else {
                startAutoScroll(event);
            }

            event.consume();
            return;
        }

        if (event.getButton() == MouseButton.SECONDARY) {
            stopAutoScroll();

            rightMouseDragging = true;
            rightDragStartX = event.getSceneX();
            rightDragStartY = event.getSceneY();
            rightDragInitialHorizontalValue = pagesScrollPane.getHvalue();
            rightDragInitialVerticalValue = pagesScrollPane.getVvalue();

            pagesScrollPane.setCursor(Cursor.CLOSED_HAND);

            event.consume();
            return;
        }

        if (event.getButton() == MouseButton.PRIMARY && autoScrollActive) {
            stopAutoScroll();
            event.consume();
        }
    }

    private void handleNavigationDragged(MouseEvent event) {
        if (autoScrollActive) {
            autoScrollPointerY = event.getSceneY();

            event.consume();
            return;
        }

        if (!rightMouseDragging || !event.isSecondaryButtonDown()) {
            return;
        }

        double horizontalDelta = event.getSceneX() - rightDragStartX;
        double verticalDelta = event.getSceneY() - rightDragStartY;

        double horizontalScrollableDistance = getHorizontalScrollableDistance();
        double verticalScrollableDistance = getVerticalScrollableDistance();

        if (horizontalScrollableDistance > 0) {
            double horizontalMovement = horizontalDelta * RIGHT_DRAG_SPEED_MULTIPLIER;

            double horizontalValue = rightDragInitialHorizontalValue
                    - horizontalMovement / horizontalScrollableDistance;

            pagesScrollPane.setHvalue(clamp(horizontalValue));
        }

        if (verticalScrollableDistance > 0) {
            double verticalMovement = verticalDelta * RIGHT_DRAG_SPEED_MULTIPLIER;

            double verticalValue = rightDragInitialVerticalValue
                    - verticalMovement / verticalScrollableDistance;

            pagesScrollPane.setVvalue(clamp(verticalValue));
        }

        event.consume();
    }

    private void handleNavigationReleased(MouseEvent event) {
        if (event.getButton() != MouseButton.SECONDARY || !rightMouseDragging) {
            return;
        }

        rightMouseDragging = false;
        pagesScrollPane.setCursor(Cursor.DEFAULT);

        event.consume();
    }

    private void handleNavigationMoved(MouseEvent event) {
        if (!autoScrollActive) {
            return;
        }

        autoScrollPointerY = event.getSceneY();

        event.consume();
    }

    private void startAutoScroll(MouseEvent event) {
        autoScrollActive = true;
        autoScrollAnchorY = event.getSceneY();
        autoScrollPointerY = event.getSceneY();

        pagesScrollPane.setCursor(Cursor.V_RESIZE);

        autoScrollTimer.start();
    }

    private void stopAutoScroll() {
        autoScrollActive = false;

        autoScrollTimer.stop();

        pagesScrollPane.setCursor(Cursor.DEFAULT);
    }


    private double getHorizontalScrollableDistance() {
        if (pagesScrollPane.getContent() == null) {
            return 0;
        }

        double contentWidth = pagesScrollPane
                .getContent()
                .getBoundsInLocal()
                .getWidth();

        double viewportWidth = pagesScrollPane
                .getViewportBounds()
                .getWidth();

        return Math.max(0, contentWidth - viewportWidth);
    }

    private double getVerticalScrollableDistance() {
        if (pagesScrollPane.getContent() == null) {
            return 0;
        }

        double contentHeight = pagesScrollPane
                .getContent()
                .getBoundsInLocal()
                .getHeight();

        double viewportHeight = pagesScrollPane
                .getViewportBounds()
                .getHeight();

        return Math.max(0, contentHeight - viewportHeight);
    }

    private double clamp(double value) {
        return Math.clamp(value, 0, 1);
    }

    private void loadPages() {
        pagesContainer.getChildren().clear();
        pageViews.clear();

        for (PdfPage page : workspace.getPages()) {
            PdfPageView pageView = new PdfPageView(page);

            pageView.setOnSelectionCreated(
                    area -> onSelectionDrawn(pageView, area)
            );

            pageView.setOnSelectionRemoved(
                    this::onSelectionRemoved
            );

            pageViews.put(page, pageView);
            pagesContainer.getChildren().add(pageView);
        }
    }

    private void onSelectionDrawn(
            PdfPageView pageView,
            SelectionArea area
    ) {
        destinationPopup.show(
                this::isDestinationAvailable,
                destination -> confirmSelection(area, destination),
                () -> pageView.removeSelection(area)
        );
    }

    private boolean isDestinationAvailable(
            SelectionDestination destination
    ) {
        try {
            TemplateSlide slide = templateLayoutService.getSlides()
                    .stream()
                    .filter(item -> item.slideId().equals(destination.slideId()))
                    .findFirst()
                    .orElse(null);

            if (slide == null) {
                return false;
            }

            TemplateSlot slot = slide.slots()
                    .stream()
                    .filter(item -> item.slotId().equals(destination.slotId()))
                    .findFirst()
                    .orElse(null);

            if (slot == null) {
                return false;
            }

            return templateLayoutService.isAvailable(
                    slide,
                    slot,
                    List.copyOf(assignments.values())
            );
        } catch (IOException exception) {
            log.error(
                    "Não foi possível validar o destino da seleção.",
                    exception
            );

            return false;
        }
    }

    private void confirmSelection(
            SelectionArea area,
            SelectionDestination destination
    ) {
        SelectionDestination resolvedDestination = templateLayoutService.resolveDestination(
                destination,
                List.copyOf(assignments.values())
        );

        int selectionOrder = nextSelectionOrder++;

        SelectionAssignment assignment = new SelectionAssignment(
                area,
                resolvedDestination,
                selectionOrder
        );

        assignments.put(
                assignment.id(),
                assignment
        );

        addAssignmentToTree(assignment);

        selectionCount++;

        updateSelectionCounter();
    }

    private void addAssignmentToTree(
            SelectionAssignment assignment
    ) {
        PdfPage page = assignment.page();

        TreeItem<SelectionTreeNode> pdfNode = pdfNodes.computeIfAbsent(
                page.pdfNumber(),
                number -> {
                    TreeItem<SelectionTreeNode> item = new TreeItem<>(
                            new SelectionTreeNode(
                                    NodeType.PDF,
                                    number,
                                    getPdfName(number)
                            )
                    );

                    root.getChildren().add(item);

                    return item;
                }
        );

        TreeItem<SelectionTreeNode> pageNode = pageNodes.computeIfAbsent(
                page,
                currentPage -> {
                    TreeItem<SelectionTreeNode> item = new TreeItem<>(
                            new SelectionTreeNode(
                                    NodeType.PAGE,
                                    currentPage,
                                    "Página " + currentPage.pageNumber()
                            )
                    );

                    pdfNode.getChildren().add(item);

                    return item;
                }
        );

        TreeItem<SelectionTreeNode> selectionNode = getSelectionTreeNodeTreeItem(assignment);

        selectionNodes.put(
                assignment.id(),
                selectionNode
        );

        pageNode.getChildren().add(selectionNode);

        pdfNode.setExpanded(true);
        pageNode.setExpanded(true);
    }

    private static @NonNull TreeItem<SelectionTreeNode> getSelectionTreeNodeTreeItem(SelectionAssignment assignment) {
        SelectionDestination destination = assignment.destination();

        String instanceSuffix = destination.slideInstance() > 1
                ? " cópia " + destination.slideInstance()
                : "";

        String nodeText = "Seleção %d → Slide %d%s / %s".formatted(
                assignment.selectionOrder(),
                destination.sourceSlideNumber(),
                instanceSuffix,
                destination.slotLabel()
        );

        return new TreeItem<>(
                new SelectionTreeNode(
                        NodeType.SELECTION,
                        assignment,
                        nodeText
                )
        );
    }

    private void onSelectionRemoved(SelectionArea area) {
        SelectionAssignment removedAssignment = assignments.remove(
                area.id()
        );

        TreeItem<SelectionTreeNode> selectionNode = selectionNodes.remove(
                area.id()
        );

        if (removedAssignment == null && selectionNode == null) {
            return;
        }

        if (selectionNode != null) {
            TreeItem<SelectionTreeNode> pageNode = selectionNode.getParent();

            if (pageNode != null) {
                pageNode.getChildren().remove(selectionNode);

                if (pageNode.getChildren().isEmpty()) {
                    removeEmptyPage(
                            area.page(),
                            pageNode
                    );
                }
            }
        }

        selectionCount = Math.max(
                0,
                selectionCount - 1
        );

        updateSelectionCounter();
    }

    private void removeEmptyPage(
            PdfPage page,
            TreeItem<SelectionTreeNode> pageNode
    ) {
        TreeItem<SelectionTreeNode> pdfNode = pageNode.getParent();

        pageNodes.remove(page);

        if (pdfNode == null) {
            return;
        }

        pdfNode.getChildren().remove(pageNode);

        if (pdfNode.getChildren().isEmpty()) {
            pdfNodes.remove(page.pdfNumber());
            root.getChildren().remove(pdfNode);
        }
    }

    private List<SelectionAssignment> getAssignments() {
        return assignments.values()
                .stream()
                .sorted(
                        java.util.Comparator
                                .comparingInt(
                                        (SelectionAssignment assignment) ->
                                                assignment.page().pdfNumber()
                                )
                                .thenComparingInt(
                                        assignment ->
                                                assignment.page().pageNumber()
                                )
                                .thenComparingInt(
                                        SelectionAssignment::selectionOrder
                                )
                )
                .toList();
    }

    private String getPdfName(int pdfNumber) {
        int index = pdfNumber - 1;

        if (index < 0 || index >= workspace.getSelectedPdfs().size()) {
            return "PDF " + pdfNumber;
        }

        return workspace.getSelectedPdfs()
                .get(index)
                .getName();
    }

    private void updateSelectionCounter() {
        selectionCounter.setText(
                selectionCount == 1
                        ? "1 área atribuída"
                        : selectionCount + " áreas atribuídas"
        );
    }

    @FXML
    private void onBack() {
        stopAutoScroll();

        SceneManager.getInstance().show(
                SceneType.HOME
        );
    }

    @FXML
    private void onFinish() {
        stopAutoScroll();

        List<SelectionAssignment> currentAssignments = getAssignments();

        if (currentAssignments.isEmpty()) {
            PopupService.getInstance().warning(
                    "Nenhuma área atribuída",
                    "Crie uma seleção e escolha seu destino antes de finalizar."
            );

            return;
        }

        try {
            Path configPath = selectionConfigService.write(
                    workspace,
                    currentAssignments
            );

            List<Path> generatedFiles = imageService.crop(
                    workspace.getTemporaryDirectory(),
                    configPath
            );

            PopupService.getInstance().show(
                    PopupType.SUCCESS,
                    "Processamento concluído",
                    "%d recortes foram criados e associados ao template."
                            .formatted(generatedFiles.size()),
                    () -> SceneManager.getInstance().show(
                            SceneType.PRESENTATION_PREVIEW
                    )
            );

            log.info(
                    "Processamento concluído. Configuração: {}",
                    configPath
            );

            log.info(
                    "Recortes criados: {}",
                    generatedFiles.size()
            );
        } catch (IOException | IllegalArgumentException exception) {
            log.error(
                    "Erro ao gerar a configuração ou recortar as imagens.",
                    exception
            );

            PopupService.getInstance().error(
                    "Erro no processamento",
                    exception.getMessage() == null
                            ? "Ocorreu um erro inesperado durante o processamento."
                            : exception.getMessage()
            );
        }
    }
}