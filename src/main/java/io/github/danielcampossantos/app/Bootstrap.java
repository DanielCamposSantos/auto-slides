package io.github.danielcampossantos.app;

import io.github.danielcampossantos.ui.navigation.SceneManager;
import io.github.danielcampossantos.ui.navigation.SceneType;
import javafx.stage.Stage;

public final class Bootstrap {

    private Bootstrap() {
    }

    public static void start(Stage stage) {

        stage.setTitle("Auto Slides");

        stage.setMinWidth(1280);

        stage.setMinHeight(720);

        stage.setResizable(true);

        stage.setMaximized(true);

        SceneManager sceneManager = SceneManager.getInstance();

        sceneManager.initialize(stage);

        sceneManager.show(SceneType.HOME);

    }


}