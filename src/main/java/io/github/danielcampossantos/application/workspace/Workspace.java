package io.github.danielcampossantos.application.workspace;

import io.github.danielcampossantos.domain.pdf.PdfPage;
import io.github.danielcampossantos.shared.io.FileUtils;
import lombok.Getter;
import lombok.extern.log4j.Log4j2;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@Log4j2
public final class Workspace {

    @Getter
    private Path temporaryDirectory;

    @Getter
    private Path generatedPresentationPath;

    private final List<File> selectedPdfs = new ArrayList<>();
    private final List<PdfPage> pages = new ArrayList<>();

    Workspace() {
    }

    void setTemporaryDirectory(Path temporaryDirectory) {
        this.temporaryDirectory = temporaryDirectory;
    }

    public List<File> getSelectedPdfs() {
        return List.copyOf(selectedPdfs);
    }

    void setSelectedPdfs(List<File> pdfs) {
        selectedPdfs.clear();
        selectedPdfs.addAll(pdfs);
    }

    public List<PdfPage> getPages() {
        return List.copyOf(pages);
    }

    void setPages(List<PdfPage> pages) {
        this.pages.clear();
        this.pages.addAll(pages);
    }

    public void setGeneratedPresentationPath(Path generatedPresentationPath) {
        this.generatedPresentationPath = generatedPresentationPath;
    }

    void clear() {
        try {
            FileUtils.deleteRecursively(temporaryDirectory);
        } catch (IOException exception) {
            log.error("Não foi possível limpar o diretório temporário.", exception);
        } finally {
            temporaryDirectory = null;
            generatedPresentationPath = null;
            selectedPdfs.clear();
            pages.clear();
        }
    }
}
