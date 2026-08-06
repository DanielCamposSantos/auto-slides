package io.github.danielcampossantos.ui.home;

import io.github.danielcampossantos.domain.pdf.PdfFileItem;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.ListCell;

import java.util.function.Consumer;

public final class PdfFileCell extends ListCell<PdfFileItem> {

    private final Consumer<PdfFileItem> removeAction;

    private final PdfItemView view = new PdfItemView();

    public PdfFileCell(
            Consumer<PdfFileItem> removeAction
    ) {

        this.removeAction = removeAction;

        setContentDisplay(ContentDisplay.GRAPHIC_ONLY);

    }

    @Override
    protected void updateItem(
            PdfFileItem item,
            boolean empty
    ) {

        super.updateItem(item, empty);

        if (empty || item == null) {

            setGraphic(null);

            return;

        }

        view.getController().bind(
                item,
                removeAction
        );

        setGraphic(view.getRoot());

    }

}