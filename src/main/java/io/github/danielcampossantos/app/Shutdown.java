package io.github.danielcampossantos.app;

import io.github.danielcampossantos.service.ApplicationService;

public class Shutdown {
    private Shutdown() {
        /* This utility class should not be instantiated */
    }

    private static final ApplicationService applicationService = ApplicationService.getInstance();

    public static void stop() {
        applicationService.clearWorkspace();
    }
}
