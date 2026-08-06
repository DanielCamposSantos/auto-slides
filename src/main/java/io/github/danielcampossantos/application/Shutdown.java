package io.github.danielcampossantos.application;

import io.github.danielcampossantos.application.workspace.ApplicationService;

public class Shutdown {
    private Shutdown() {
        /* This utility class should not be instantiated */
    }

    private static final ApplicationService applicationService = ApplicationService.getInstance();

    public static void stop() {
        applicationService.clearWorkspace();
    }
}
