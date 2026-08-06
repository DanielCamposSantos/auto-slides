package io.github.danielcampossantos.ui.view;

import io.github.danielcampossantos.ui.controller.PdfItemController;
import javafx.fxml.FXMLLoader;
import javafx.scene.layout.HBox;
import lombok.Getter;

import java.io.IOException;

@Getter
public final class PdfItemView {

    private final HBox root;

    private final PdfItemController controller;

    public PdfItemView() {

        try {

            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/ui/fxml/pdf-item.fxml")
            );

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