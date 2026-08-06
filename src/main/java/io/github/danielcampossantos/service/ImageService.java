package io.github.danielcampossantos.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.danielcampossantos.model.CropAreaConfig;
import io.github.danielcampossantos.model.PageCropConfig;
import io.github.danielcampossantos.model.PdfCropConfig;
import io.github.danielcampossantos.model.SelectionConfig;
import lombok.extern.log4j.Log4j2;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@Log4j2
public final class ImageService {

    private static final String OUTPUT_DIRECTORY = "crops";

    private final ObjectMapper objectMapper = new ObjectMapper();

    public List<Path> crop(Path temporaryDirectory, Path configPath) throws IOException {
        SelectionConfig config = objectMapper.readValue(configPath.toFile(), SelectionConfig.class);
        Path outputDirectory = temporaryDirectory.resolve(OUTPUT_DIRECTORY);

        Files.createDirectories(outputDirectory);

        List<Path> generatedFiles = new ArrayList<>();

        for (PdfCropConfig pdf : config.pdfs()) {
            generatedFiles.addAll(cropPdf(temporaryDirectory, outputDirectory, pdf));
        }

        log.info("{} recortes criados em {}", generatedFiles.size(), outputDirectory);

        return List.copyOf(generatedFiles);
    }

    private List<Path> cropPdf(Path temporaryDirectory, Path outputDirectory, PdfCropConfig pdf) throws IOException {
        Path pdfDirectory = outputDirectory.resolve("pdf-" + pdf.pdfNumber());

        Files.createDirectories(pdfDirectory);

        List<Path> generatedFiles = new ArrayList<>();

        for (PageCropConfig page : pdf.pages()) {
            generatedFiles.addAll(cropPage(temporaryDirectory, pdfDirectory, page));
        }

        return generatedFiles;
    }

    private List<Path> cropPage(Path temporaryDirectory, Path pdfDirectory, PageCropConfig page) throws IOException {
        Path sourcePath = temporaryDirectory.resolve(page.sourceImage());

        if (!Files.exists(sourcePath)) {
            throw new IOException("Imagem temporária não encontrada: " + sourcePath);
        }

        BufferedImage sourceImage = ImageIO.read(sourcePath.toFile());

        if (sourceImage == null) {
            throw new IOException("Não foi possível ler a imagem: " + sourcePath);
        }

        List<Path> generatedFiles = new ArrayList<>();

        for (int index = 0; index < page.selections().size(); index++) {
            CropAreaConfig selection = page.selections().get(index);
            Path generatedPath = cropSelection(sourceImage, pdfDirectory, page.pageNumber(), index + 1, selection);

            generatedFiles.add(generatedPath);
        }

        return generatedFiles;
    }

    private Path cropSelection(
            BufferedImage sourceImage,
            Path pdfDirectory,
            int pageNumber,
            int selectionNumber,
            CropAreaConfig selection
    ) throws IOException {
        validateBounds(sourceImage, selection);

        BufferedImage croppedImage = sourceImage.getSubimage(
                selection.x(),
                selection.y(),
                selection.width(),
                selection.height()
        );

        String shortId = selection.id().toString().substring(0, 8);
        String fileName = "pagina-%d-selecao-%03d-%s.png".formatted(pageNumber, selectionNumber, shortId);
        Path destinationPath = pdfDirectory.resolve(fileName);

        ImageIO.write(croppedImage, "PNG", destinationPath.toFile());

        log.info("Recorte criado: {}", destinationPath);

        return destinationPath;
    }

    private void validateBounds(BufferedImage image, CropAreaConfig selection) {
        boolean invalidPosition = selection.x() < 0 || selection.y() < 0;
        boolean invalidSize = selection.width() <= 0 || selection.height() <= 0;
        boolean exceedsWidth = selection.x() + selection.width() > image.getWidth();
        boolean exceedsHeight = selection.y() + selection.height() > image.getHeight();

        if (invalidPosition || invalidSize || exceedsWidth || exceedsHeight) {
            throw new IllegalArgumentException("Área de corte inválida: " + selection);
        }
    }
}