package io.github.danielcampossantos.ui.util;

import javafx.fxml.FXMLLoader;

import java.net.URL;

public final class FXMLLoaderFactory {

    private FXMLLoaderFactory() {
    }

    public static FXMLLoader create(String resource) {

        URL url = FXMLLoaderFactory.class.getResource(resource);

        if (url == null) {

            throw new IllegalArgumentException("FXML não encontrado: " + resource);

        }

        return new FXMLLoader(url);

    }

}