package io.github.danielcampossantos.ui.navigation;

public enum SceneType {

    HOME(
            "/ui/fxml/home.fxml",
            "Auto Slides"
    ),

    SETTINGS(
            "/ui/fxml/settings.fxml",
            "Configurações"
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

    SceneType(String fxml, String title) {
        this.fxml = fxml;
        this.title = title;
    }

    public String fxml() {
        return fxml;
    }

    public String title() {
        return title;
    }
}
