package io.github.danielcampossantos.ui.navigation;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum SceneType {

    HOME(
            "/ui/fxml/home.fxml",
            "Auto Slides"
    ),

    AREA_SELECTION(
            "/ui/fxml/area-selection.fxml",
            "Seleção de Áreas"
    ),

    PRESENTATION_PREVIEW(
            "/ui/fxml/presentation-preview.fxml",
            "Montagem da Apresentação"
    );

    private final String fxml;

    private final String title;
}