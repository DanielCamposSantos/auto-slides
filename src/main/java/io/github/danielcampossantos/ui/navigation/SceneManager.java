package io.github.danielcampossantos.ui.navigation;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

public final class SceneManager {

    private static SceneManager instance;

    private final Map<SceneType, Parent> cache = new EnumMap<>(SceneType.class);

    private Stage stage;

    private Scene scene;

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

        scene = new Scene(new StackPane());

        stage.setScene(scene);

    }

    public void show(SceneType sceneType) {

        Parent root;

        if (sceneType == SceneType.AREA_SELECTION) {

            root = loadView(sceneType);

        } else {

            root = cache.computeIfAbsent(sceneType, this::loadView);

        }

        scene.setRoot(root);

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
                Objects.requireNonNull(SceneManager.class.getResource(sceneType.getFxml()))
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