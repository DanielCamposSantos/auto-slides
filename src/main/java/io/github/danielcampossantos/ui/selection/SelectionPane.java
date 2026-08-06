package io.github.danielcampossantos.ui.selection;

import io.github.danielcampossantos.model.PdfPage;
import io.github.danielcampossantos.model.SelectionArea;
import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import lombok.Setter;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

public final class SelectionPane extends Pane {

    private static final double MIN_SELECTION_SIZE = 5;

    private final PdfPage page;

    private final Map<SelectionArea, SelectionView> selections = new LinkedHashMap<>();

    private Point2D startPoint;

    private SelectionView currentSelection;

    @Setter
    private Consumer<SelectionArea> onSelectionCreated;

    @Setter
    private Consumer<SelectionArea> onSelectionRemoved;

    public SelectionPane(PdfPage page) {
        this.page = page;
        initialize();
    }

    private void initialize() {
        setPickOnBounds(true);

        addEventFilter(MouseEvent.MOUSE_PRESSED, this::handleMousePressed);
        addEventFilter(MouseEvent.MOUSE_DRAGGED, this::handleMouseDragged);
        addEventFilter(MouseEvent.MOUSE_RELEASED, this::handleMouseReleased);
    }

    private void handleMousePressed(MouseEvent event) {
        if (event.getButton() != MouseButton.PRIMARY) {
            return;
        }

        if (isSelectionTarget(event.getTarget())) {
            return;
        }

        startPoint = new Point2D(event.getX(), event.getY());
        currentSelection = new SelectionView();
        currentSelection.setBounds(startPoint.getX(), startPoint.getY(), 0, 0);

        getChildren().add(currentSelection);

        event.consume();
    }

    private void handleMouseDragged(MouseEvent event) {
        if (currentSelection == null || startPoint == null) {
            return;
        }

        double x = Math.min(startPoint.getX(), event.getX());
        double y = Math.min(startPoint.getY(), event.getY());
        double width = Math.abs(event.getX() - startPoint.getX());
        double height = Math.abs(event.getY() - startPoint.getY());

        currentSelection.setBounds(x, y, width, height);

        event.consume();
    }

    private void handleMouseReleased(MouseEvent event) {
        if (currentSelection == null) {
            return;
        }

        double x = currentSelection.getRectangle().getX();
        double y = currentSelection.getRectangle().getY();
        double width = currentSelection.getWidth();
        double height = currentSelection.getHeight();

        if (width < MIN_SELECTION_SIZE || height < MIN_SELECTION_SIZE) {
            getChildren().remove(currentSelection);
            resetDrawingState();
            event.consume();
            return;
        }

        SelectionArea area = new SelectionArea(
                UUID.randomUUID(),
                page,
                x,
                y,
                width,
                height,
                getWidth(),
                getHeight()
        );

        SelectionView selection = currentSelection;

        selection.setOnRemove(() -> removeSelection(area));
        selections.put(area, selection);

        if (onSelectionCreated != null) {
            onSelectionCreated.accept(area);
        }

        resetDrawingState();

        event.consume();
    }

    private boolean isSelectionTarget(Object target) {
        if (!(target instanceof Node node)) {
            return false;
        }

        Node current = node;

        while (current != null) {
            if (current instanceof SelectionView) {
                return true;
            }

            current = current.getParent();
        }

        return false;
    }

    private void removeSelection(SelectionArea area) {
        SelectionView view = selections.remove(area);

        if (view == null) {
            return;
        }

        getChildren().remove(view);

        if (onSelectionRemoved != null) {
            onSelectionRemoved.accept(area);
        }
    }

    private void resetDrawingState() {
        currentSelection = null;
        startPoint = null;
    }
}