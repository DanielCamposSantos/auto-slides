package io.github.danielcampossantos.ui.view;

import io.github.danielcampossantos.model.PdfPage;
import io.github.danielcampossantos.model.SelectionArea;
import io.github.danielcampossantos.ui.selection.SelectionPane;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import lombok.Setter;

import java.util.function.Consumer;

public final class PdfPageView extends StackPane {

    private static final double IMAGE_WIDTH = 900;

    private final PdfPage page;

    private final ImageView imageView;

    private final SelectionPane selectionPane;

    @Setter
    private Consumer<SelectionArea> onSelectionCreated;

    @Setter
    private Consumer<SelectionArea> onSelectionRemoved;

    public PdfPageView(PdfPage page) {
        this.page = page;
        imageView = new ImageView();
        selectionPane = new SelectionPane(page);

        initialize();
    }

    private void initialize() {
        setAlignment(Pos.CENTER);
        getStyleClass().add("pdf-page");

        Image image = new Image(page.imagePath().toUri().toString());

        imageView.setImage(image);
        imageView.setFitWidth(IMAGE_WIDTH);
        imageView.setPreserveRatio(true);
        imageView.setSmooth(true);
        imageView.setCache(true);

        selectionPane.setOnSelectionCreated(area -> {
            if (onSelectionCreated != null) {
                onSelectionCreated.accept(area);
            }
        });

        selectionPane.setOnSelectionRemoved(area -> {
            if (onSelectionRemoved != null) {
                onSelectionRemoved.accept(area);
            }
        });

        getChildren().addAll(imageView, selectionPane);

        imageView.layoutBoundsProperty().addListener(
                (observable, oldBounds, newBounds) -> resizeSelectionPane()
        );

        Platform.runLater(this::resizeSelectionPane);
    }

    private void resizeSelectionPane() {
        double width = imageView.getBoundsInLocal().getWidth();
        double height = imageView.getBoundsInLocal().getHeight();

        selectionPane.setPrefSize(width, height);
        selectionPane.setMinSize(width, height);
        selectionPane.setMaxSize(width, height);
    }

    public void removeSelection(SelectionArea area) {
        selectionPane.removeSelection(area);
    }
}