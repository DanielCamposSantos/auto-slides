package io.github.danielcampossantos.infrastructure.template;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import io.github.danielcampossantos.domain.template.TemplateLayout;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class TemplateLayoutStorageService {

    private static final String APPLICATION_DIRECTORY = ".auto-slides";
    private static final String TEMPLATES_DIRECTORY = "templates";
    private static final String LAYOUT_FILE_NAME = "template-layout.json";

    private final ObjectMapper objectMapper = new ObjectMapper()
            .enable(SerializationFeature.INDENT_OUTPUT);

    public Path save(TemplateLayout layout) throws IOException {
        Path templateDirectory = getTemplateDirectory(layout.templateId());

        Files.createDirectories(templateDirectory);

        Path layoutPath = templateDirectory.resolve(LAYOUT_FILE_NAME);

        objectMapper.writeValue(layoutPath.toFile(), layout);

        return layoutPath;
    }

    public TemplateLayout read(Path layoutPath) throws IOException {
        if (layoutPath == null) {
            throw new IOException(
                    "Nenhum arquivo de configuração do template foi informado."
            );
        }

        if (!Files.isRegularFile(layoutPath)) {
            throw new IOException(
                    "O arquivo de configuração do template não foi encontrado: "
                            + layoutPath
            );
        }

        return objectMapper.readValue(
                layoutPath.toFile(),
                TemplateLayout.class
        );
    }

    public void delete(String templateId) throws IOException {
        if (templateId == null || templateId.isBlank()) {
            return;
        }

        Path templateDirectory = getTemplateDirectory(templateId);

        if (!Files.exists(templateDirectory)) {
            return;
        }

        try (var paths = Files.walk(templateDirectory)) {
            paths.sorted((first, second) -> second.compareTo(first))
                    .forEach(path -> {
                        try {
                            Files.deleteIfExists(path);
                        } catch (IOException exception) {
                            throw new TemplateStorageException(exception);
                        }
                    });
        } catch (TemplateStorageException exception) {
            throw exception.getIOException();
        }
    }

    private Path getTemplateDirectory(String templateId) {
        return Path.of(
                System.getProperty("user.home"),
                APPLICATION_DIRECTORY,
                TEMPLATES_DIRECTORY,
                templateId
        );
    }

    private static final class TemplateStorageException extends RuntimeException {

        private final IOException ioException;

        private TemplateStorageException(IOException ioException) {
            super(ioException);

            this.ioException = ioException;
        }

        private IOException getIOException() {
            return ioException;
        }
    }
}