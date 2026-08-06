package io.github.danielcampossantos.application;

import io.github.danielcampossantos.ui.common.window.AppIconFactory;
import io.github.danielcampossantos.ui.navigation.SceneManager;
import io.github.danielcampossantos.ui.navigation.SceneType;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

public final class Bootstrap {

    private Bootstrap() {
    }

    public static void start(Stage stage) {
        stage.initStyle(StageStyle.TRANSPARENT);

        stage.setTitle("Auto Slides");
        stage.setMinWidth(1280);
        stage.setMinHeight(720);
        stage.setResizable(true);

        stage.getIcons().setAll(
                AppIconFactory.create(16),
                AppIconFactory.create(24),
                AppIconFactory.create(32),
                AppIconFactory.create(48),
                AppIconFactory.create(64),
                AppIconFactory.create(128),
                AppIconFactory.create(256)
        );

        SceneManager sceneManager = SceneManager.getInstance();

        sceneManager.initialize(stage);
        sceneManager.show(SceneType.HOME);

        stage.setMaximized(true);
    }
}