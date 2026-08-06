package io.github.danielcampossantos.ui.navigation;

import lombok.Getter;

@Getter
public enum SceneType {

    HOME(
            "/ui/fxml/home.fxml",
            "Auto Slides"
    ),

    AREA_SELECTION(
            "/ui/fxml/area-selection.fxml",
            "Seleção"
    );

    private final String fxml;

    private final String title;

    SceneType(String fxml, String title) {

        this.fxml = fxml;

        this.title = title;

    }

}