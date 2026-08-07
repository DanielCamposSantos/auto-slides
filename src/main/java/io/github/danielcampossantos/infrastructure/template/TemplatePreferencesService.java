package io.github.danielcampossantos.infrastructure.template;

import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Optional;
import java.util.prefs.Preferences;

public final class TemplatePreferencesService {

    private static final String TEMPLATE_PATH_KEY = "presentation-template-path";
    private static final String LAYOUT_PATH_KEY = "presentation-layout-path";
    private static final String TEMPLATE_ID_KEY = "presentation-template-id";

    private static TemplatePreferencesService instance;

    private final Preferences preferences;

    private TemplatePreferencesService() {
        preferences = Preferences.userNodeForPackage(
                TemplatePreferencesService.class
        );
    }

    public static TemplatePreferencesService getInstance() {
        if (instance == null) {
            instance = new TemplatePreferencesService();
        }

        return instance;
    }

    public void saveTemplate(
            Path templatePath,
            Path layoutPath,
            String templateId
    ) {
        if (templatePath == null) {
            throw new IllegalArgumentException(
                    "O caminho do template não pode ser nulo."
            );
        }

        if (layoutPath == null) {
            throw new IllegalArgumentException(
                    "O caminho da configuração não pode ser nulo."
            );
        }

        if (templateId == null || templateId.isBlank()) {
            throw new IllegalArgumentException(
                    "O identificador do template não pode ser vazio."
            );
        }

        preferences.put(
                TEMPLATE_PATH_KEY,
                templatePath.toAbsolutePath().normalize().toString()
        );

        preferences.put(
                LAYOUT_PATH_KEY,
                layoutPath.toAbsolutePath().normalize().toString()
        );

        preferences.put(
                TEMPLATE_ID_KEY,
                templateId
        );
    }

    public Optional<Path> getTemplate() {
        return readValidPath(
                TEMPLATE_PATH_KEY,
                true
        );
    }

    public Optional<Path> getLayoutPath() {
        return readValidPath(
                LAYOUT_PATH_KEY,
                false
        );
    }

    public Optional<String> getTemplateId() {
        String templateId = preferences.get(
                TEMPLATE_ID_KEY,
                null
        );

        if (templateId == null || templateId.isBlank()) {
            return Optional.empty();
        }

        return Optional.of(templateId);
    }

    public boolean hasTemplate() {
        return getTemplate().isPresent()
                && getLayoutPath().isPresent()
                && getTemplateId().isPresent();
    }

    public void clearTemplate() {
        preferences.remove(TEMPLATE_PATH_KEY);
        preferences.remove(LAYOUT_PATH_KEY);
        preferences.remove(TEMPLATE_ID_KEY);
    }

    private Optional<Path> readValidPath(
            String key,
            boolean requirePptx
    ) {
        String savedPath = preferences.get(
                key,
                null
        );

        if (savedPath == null || savedPath.isBlank()) {
            return Optional.empty();
        }

        try {
            Path path = Path.of(savedPath);

            if (!Files.isRegularFile(path)) {
                clearTemplate();

                return Optional.empty();
            }

            if (requirePptx && !isPptx(path)) {
                clearTemplate();

                return Optional.empty();
            }

            return Optional.of(path);
        } catch (InvalidPathException exception) {
            clearTemplate();

            return Optional.empty();
        }
    }

    private boolean isPptx(Path path) {
        return path.getFileName()
                .toString()
                .toLowerCase(Locale.ROOT)
                .endsWith(".pptx");
    }
}