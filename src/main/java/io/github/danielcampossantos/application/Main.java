package io.github.danielcampossantos.application;

import javafx.application.Application;
import javafx.stage.Stage;

public final class Main extends Application {

    @Override
    public void start(Stage primaryStage) {

        Bootstrap.start(primaryStage);

    }

    @Override
    public void stop() throws Exception {

        Shutdown.stop();

    }

    static void main(String[] args) {

        launch(args);

    }

}