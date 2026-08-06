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

    private final List<File> selectedPdfs;

    private final List<PdfPage> pages;

    Workspace() {

        this.selectedPdfs = new ArrayList<>();

        this.pages = new ArrayList<>();

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

    public void clear() {
        try {
            var path = temporaryDirectory.toAbsolutePath();

            FileUtils.deletarDiretorioRecursivo(path);
            temporaryDirectory = null;

            selectedPdfs.clear();

            pages.clear();
        } catch (IOException e) {
            log.error(e);
        }
    }

}