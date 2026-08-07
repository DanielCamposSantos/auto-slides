package io.github.danielcampossantos.infrastructure.selection;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.danielcampossantos.domain.selection.CropAreaConfig;
import io.github.danielcampossantos.domain.selection.PageCropConfig;
import io.github.danielcampossantos.domain.selection.PdfCropConfig;
import io.github.danielcampossantos.domain.selection.SelectionConfig;
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

    private final ObjectMapper objectMapper = new ObjectMapper();

    public List<Path> crop(Path temporaryDirectory, Path configPath) throws IOException {
        SelectionConfig config = objectMapper.readValue(
                configPath.toFile(),
                SelectionConfig.class
        );

        List<Path> generatedFiles = new ArrayList<>();

        for (PdfCropConfig pdf : config.pdfs()) {
            generatedFiles.addAll(cropPdf(temporaryDirectory, pdf));
        }

        log.info("{} recortes criados em {}", generatedFiles.size(), temporaryDirectory.resolve("crops"));

        return List.copyOf(generatedFiles);
    }

    private List<Path> cropPdf(
            Path temporaryDirectory,
            PdfCropConfig pdf
    ) throws IOException {
        List<Path> generatedFiles = new ArrayList<>();

        for (PageCropConfig page : pdf.pages()) {
            generatedFiles.addAll(cropPage(temporaryDirectory, page));
        }

        return generatedFiles;
    }

    private List<Path> cropPage(
            Path temporaryDirectory,
            PageCropConfig page
    ) throws IOException {
        Path sourcePath = temporaryDirectory.resolve(page.sourceImage());

        if (!Files.exists(sourcePath)) {
            throw new IOException("Imagem temporária não encontrada: " + sourcePath);
        }

        BufferedImage sourceImage = ImageIO.read(sourcePath.toFile());

        if (sourceImage == null) {
            throw new IOException("Não foi possível ler a imagem: " + sourcePath);
        }

        List<Path> generatedFiles = new ArrayList<>();

        for (CropAreaConfig selection : page.selections()) {
            generatedFiles.add(cropSelection(
                    temporaryDirectory,
                    sourceImage,
                    selection
            ));
        }

        return generatedFiles;
    }

    private Path cropSelection(
            Path temporaryDirectory,
            BufferedImage sourceImage,
            CropAreaConfig selection
    ) throws IOException {
        validateBounds(sourceImage, selection);

        BufferedImage croppedImage = sourceImage.getSubimage(
                selection.x(),
                selection.y(),
                selection.width(),
                selection.height()
        );

        Path destinationPath = temporaryDirectory.resolve(selection.outputImage());

        Files.createDirectories(destinationPath.getParent());

        if (!ImageIO.write(croppedImage, "PNG", destinationPath.toFile())) {
            throw new IOException("Não foi possível gravar o recorte: " + destinationPath);
        }

        log.info("Recorte criado: {}", destinationPath);

        return destinationPath;
    }

    private void validateBounds(
            BufferedImage image,
            CropAreaConfig selection
    ) {
        boolean invalidPosition = selection.x() < 0 || selection.y() < 0;
        boolean invalidSize = selection.width() <= 0 || selection.height() <= 0;
        boolean exceedsWidth = selection.x() + selection.width() > image.getWidth();
        boolean exceedsHeight = selection.y() + selection.height() > image.getHeight();

        if (invalidPosition || invalidSize || exceedsWidth || exceedsHeight) {
            throw new IllegalArgumentException("Área de corte inválida: " + selection);
        }
    }
}
