package io.github.danielcampossantos.service;

import io.github.danielcampossantos.model.PdfPage;
import lombok.extern.log4j.Log4j2;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@Log4j2
public final class PdfService {

    private static PdfService instance;

    private PdfService() {
    }

    public static PdfService getInstance() {

        if (instance == null) {
            instance = new PdfService();
        }

        return instance;

    }

    public List<PdfPage> toImages(List<File> pdfFiles, Path temporaryDirectory) throws IOException {

        List<PdfPage> pages = new ArrayList<>();

        for (int pdfIndex = 0; pdfIndex < pdfFiles.size(); pdfIndex++) {

            File pdf = pdfFiles.get(pdfIndex);

            log.info("Processando {}", pdf.getName());

            try (PDDocument document = Loader.loadPDF(pdf)) {

                PDFRenderer renderer = new PDFRenderer(document);

                for (int pageIndex = 0; pageIndex < document.getNumberOfPages(); pageIndex++) {

                    BufferedImage image = renderer.renderImageWithDPI(
                            pageIndex,
                            300,
                            ImageType.RGB
                    );

                    Path imagePath = temporaryDirectory.resolve(
                            "pdf-%d-pagina-%d.png".formatted(
                                    pdfIndex + 1,
                                    pageIndex + 1
                            )
                    );

                    ImageIO.write(image, "PNG", imagePath.toFile());

                    pages.add(new PdfPage(
                            pdfIndex + 1,
                            pageIndex + 1,
                            imagePath
                    ));

                }

            }

            log.info("Renderização concluída para {}", pdf.getName());

        }

        return pages;

    }

}