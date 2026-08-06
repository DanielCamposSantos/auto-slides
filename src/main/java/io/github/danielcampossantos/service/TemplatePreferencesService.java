package io.github.danielcampossantos.service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.prefs.Preferences;

public final class TemplatePreferencesService {

    private static final String TEMPLATE_PATH_KEY = "last-presentation-template";

    private static TemplatePreferencesService instance;

    private final Preferences preferences;

    private TemplatePreferencesService() {
        preferences = Preferences.userNodeForPackage(TemplatePreferencesService.class);
    }

    public static TemplatePreferencesService getInstance() {
        if (instance == null) {
            instance = new TemplatePreferencesService();
        }

        return instance;
    }

    public void saveTemplate(Path templatePath) {
        preferences.put(TEMPLATE_PATH_KEY, templatePath.toAbsolutePath().toString());
    }

    public Optional<Path> getTemplate() {
        String savedPath = preferences.get(TEMPLATE_PATH_KEY, null);

        if (savedPath == null || savedPath.isBlank()) {
            return Optional.empty();
        }

        Path path = Path.of(savedPath);

        if (!Files.isRegularFile(path)) {
            preferences.remove(TEMPLATE_PATH_KEY);
            return Optional.empty();
        }

        return Optional.of(path);
    }

    public void clearTemplate() {
        preferences.remove(TEMPLATE_PATH_KEY);
    }
}