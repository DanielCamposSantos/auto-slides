package io.github.danielcampossantos.ui.home;

import io.github.danielcampossantos.ui.common.fxml.FXMLLoaderFactory;
import javafx.fxml.FXMLLoader;
import javafx.scene.layout.HBox;
import lombok.Getter;

import java.io.IOException;

@Getter
public final class PdfItemView {

    private final HBox root;
    private final PdfItemController controller;

    public PdfItemView() {
        FXMLLoader loader = FXMLLoaderFactory.create("/ui/fxml/pdf-item.fxml");

        try {
            root = loader.load();
            controller = loader.getController();
        } catch (IOException exception) {
            throw new IllegalStateException(
                    "Não foi possível carregar pdf-item.fxml",
                    exception
            );
        }
    }
}
