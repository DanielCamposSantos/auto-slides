package io.github.danielcampossantos.service;

import lombok.Getter;
import lombok.extern.log4j.Log4j2;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

@Log4j2
public final class ApplicationService {

    private static ApplicationService instance;

    private final PdfService pdfService;

    @Getter
    private Workspace workspace;

    private ApplicationService() {

        pdfService = PdfService.getInstance();

    }

    public static ApplicationService getInstance() {

        if (instance == null) {
            instance = new ApplicationService();
        }

        return instance;

    }

    public void startSelection(List<File> pdfFiles) throws IOException {

        clearWorkspace();

        workspace = new Workspace();

        Path temporaryDirectory = Files.createTempDirectory("auto-slides");

        workspace.setTemporaryDirectory(temporaryDirectory);

        workspace.setSelectedPdfs(pdfFiles);

        workspace.setPages(pdfService.toImages(pdfFiles, temporaryDirectory));

        log.info("Workspace criado.");

        log.info("Diretório temporário: {}", temporaryDirectory);

        log.info("{} páginas carregadas.", workspace.getPages().size());

    }

    public void clearWorkspace() {

        if (workspace == null) {
            return;
        }

        workspace.clear();

        workspace = null;

    }

}