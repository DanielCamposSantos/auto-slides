package io.github.danielcampossantos.application;

import io.github.danielcampossantos.application.workspace.ApplicationService;
import javafx.application.Application;
import javafx.stage.Stage;

public final class Main extends Application {

    @Override
    public void start(Stage primaryStage) {
        Bootstrap.start(primaryStage);
    }

    @Override
    public void stop() {
        ApplicationService.getInstance().clearWorkspace();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
