package io.github.danielcampossantos.service;

import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.Locale;
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
        if (templatePath == null) {
            throw new IllegalArgumentException("O caminho do template não pode ser nulo.");
        }

        preferences.put(
                TEMPLATE_PATH_KEY,
                templatePath.toAbsolutePath().normalize().toString()
        );
    }

    public Optional<Path> getTemplate() {
        String savedPath = preferences.get(TEMPLATE_PATH_KEY, null);

        if (savedPath == null || savedPath.isBlank()) {
            return Optional.empty();
        }

        try {
            Path path = Path.of(savedPath);

            if (!Files.isRegularFile(path) || !isPptx(path)) {
                clearTemplate();
                return Optional.empty();
            }

            return Optional.of(path);
        } catch (InvalidPathException exception) {
            clearTemplate();
            return Optional.empty();
        }
    }

    public boolean hasTemplate() {
        return getTemplate().isPresent();
    }

    public void clearTemplate() {
        preferences.remove(TEMPLATE_PATH_KEY);
    }

    private boolean isPptx(Path path) {
        return path.getFileName()
                .toString()
                .toLowerCase(Locale.ROOT)
                .endsWith(".pptx");
    }
}