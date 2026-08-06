package io.github.danielcampossantos.ui.window;

import javafx.scene.image.Image;

import java.util.Objects;

public final class AppIconFactory {

    private static final String ICON_PATH = "/ui/images/app-icon.png";

    private AppIconFactory() {
    }

    public static Image create(int size) {
        return new Image(
                Objects.requireNonNull(AppIconFactory.class.getResourceAsStream(ICON_PATH)),
                size,
                size,
                true,
                true
        );
    }
}