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

    private final Map<SceneType, Parent> cache = new EnumMap<>(SceneType.class);

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
        Scene scene;
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
        Parent root = sceneType == SceneType.AREA_SELECTION
                ? loadView(sceneType)
                : cache.computeIfAbsent(sceneType, this::loadView);

        window.setContent(root);
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

    private Parent loadView(SceneType sceneType) {
        FXMLLoader loader = new FXMLLoader(
                Objects.requireNonNull(
                        SceneManager.class.getResource(sceneType.getFxml())
                )
        );

        try {
            return loader.load();
        } catch (IOException exception) {
            throw new IllegalStateException(
                    "Não foi possível carregar %s".formatted(sceneType.getFxml()),
                    exception
            );
        }
    }
}