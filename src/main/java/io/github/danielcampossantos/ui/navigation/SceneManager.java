package io.github.danielcampossantos.ui.navigation;

import io.github.danielcampossantos.ui.common.fxml.FXMLLoaderFactory;
import io.github.danielcampossantos.ui.common.window.AppWindow;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import lombok.Getter;

import java.io.IOException;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

public final class SceneManager {

    private static SceneManager instance;

    private final Map<SceneType, LoadedView> cache = new EnumMap<>(SceneType.class);

    private Stage stage;

    @Getter
    private AppWindow window;

    private SceneManager() {
    }

    public static SceneManager getInstance() {
        if (instance == null) {
            instance = new SceneManager();
        }

        return instance;
    }

    public void initialize(Stage stage) {
        this.stage = stage;
        window = new AppWindow(stage);

        Scene scene = new Scene(window);
        scene.setFill(Color.TRANSPARENT);
        scene.getStylesheets().addAll(
                stylesheet("/ui/css/window.css"),
                stylesheet("/ui/css/popup.css")
        );

        stage.setScene(scene);
    }

    public void show(SceneType sceneType) {
        LoadedView view = shouldReload(sceneType)
                ? loadView(sceneType)
                : cache.computeIfAbsent(sceneType, this::loadView);

        if (view.controller() instanceof Reloadable reloadable) {
            reloadable.reload();
        }

        window.setContent(view.root());
        window.setTitle(sceneType.title());
        stage.setTitle(sceneType.title());
        stage.show();
    }

    private boolean shouldReload(SceneType sceneType) {
        return switch (sceneType) {
            case AREA_SELECTION, PRESENTATION_PREVIEW -> true;
            default -> false;
        };
    }

    private LoadedView loadView(SceneType sceneType) {
        FXMLLoader loader = FXMLLoaderFactory.create(sceneType.fxml());

        try {
            return new LoadedView(loader.load(), loader.getController());
        } catch (IOException exception) {
            throw new IllegalStateException(
                    "Não foi possível carregar %s".formatted(sceneType.fxml()),
                    exception
            );
        }
    }

    private String stylesheet(String resource) {
        return Objects.requireNonNull(
                SceneManager.class.getResource(resource),
                "Stylesheet não encontrado: " + resource
        ).toExternalForm();
    }

    private record LoadedView(Parent root, Object controller) {
    }
}
