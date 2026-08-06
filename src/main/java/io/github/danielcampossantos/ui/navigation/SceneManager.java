package io.github.danielcampossantos.ui.navigation;

import io.github.danielcampossantos.ui.window.AppWindow;
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

    private Scene scene;

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
        scene = new Scene(window);

        scene.setFill(Color.TRANSPARENT);

        scene.getStylesheets().add(
                Objects.requireNonNull(
                        SceneManager.class.getResource("/ui/css/window.css")
                ).toExternalForm()
        );

        scene.getStylesheets().add(
                Objects.requireNonNull(
                        SceneManager.class.getResource("/ui/css/popup.css")
                ).toExternalForm()
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
        window.setTitle(sceneType.getTitle());

        stage.setTitle(sceneType.getTitle());
        stage.show();
    }

    public void clear(SceneType sceneType) {
        cache.remove(sceneType);
    }

    public void clearAll() {
        cache.clear();
    }

    private boolean shouldReload(SceneType sceneType) {
        return sceneType == SceneType.AREA_SELECTION
                || sceneType == SceneType.PRESENTATION_PREVIEW;
    }

    private LoadedView loadView(SceneType sceneType) {
        FXMLLoader loader = new FXMLLoader(
                Objects.requireNonNull(
                        SceneManager.class.getResource(sceneType.getFxml())
                )
        );

        try {
            Parent root = loader.load();

            return new LoadedView(root, loader.getController());
        } catch (IOException exception) {
            throw new IllegalStateException(
                    "Não foi possível carregar %s".formatted(sceneType.getFxml()),
                    exception
            );
        }
    }

    private record LoadedView(
            Parent root,
            Object controller
    ) {
    }
}