package io.github.danielcampossantos.ui.home;

import io.github.danielcampossantos.domain.pdf.PdfFileItem;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

import java.util.Objects;
import java.util.function.Consumer;

public final class PdfItemController {

    private static final Image PDF_IMAGE =
            new Image(
                    Objects.requireNonNull(
                            PdfItemController.class.getResourceAsStream(
                                    "/ui/images/pdf.png"
                            )
                    )
            );

    @FXML
    private ImageView pdfIcon;

    @FXML
    private Label fileNameLabel;

    private PdfFileItem currentItem;

    private Consumer<PdfFileItem> removeAction;

    @FXML
    private void initialize() {

        pdfIcon.setImage(PDF_IMAGE);

    }

    public void bind(
            PdfFileItem item,
            Consumer<PdfFileItem> removeAction
    ) {

        this.currentItem = item;

        this.removeAction = removeAction;

        fileNameLabel.setText(item.fileName());

    }

    @FXML
    private void onDelete() {

        if (currentItem != null && removeAction != null) {

            removeAction.accept(currentItem);

        }

    }

}