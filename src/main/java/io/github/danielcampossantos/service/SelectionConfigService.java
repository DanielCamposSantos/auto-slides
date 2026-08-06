package io.github.danielcampossantos.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import io.github.danielcampossantos.model.*;
import lombok.extern.log4j.Log4j2;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Log4j2
public final class SelectionConfigService {

    private static final String CONFIG_FILE_NAME = "selection-config.json";

    private final ObjectMapper objectMapper = new ObjectMapper()
            .enable(SerializationFeature.INDENT_OUTPUT);

    public Path write(Workspace workspace, List<SelectionArea> selections) throws IOException {
        SelectionConfig config = createConfig(workspace, selections);
        Path configPath = workspace.getTemporaryDirectory().resolve(CONFIG_FILE_NAME);

        objectMapper.writeValue(configPath.toFile(), config);

        log.info("Configuração das seleções criada em {}", configPath);

        return configPath;
    }

    private SelectionConfig createConfig(Workspace workspace, List<SelectionArea> selections) throws IOException {
        Map<Integer, List<SelectionArea>> selectionsByPdf = selections.stream()
                .collect(Collectors.groupingBy(
                        area -> area.page().pdfNumber(),
                        LinkedHashMap::new,
                        Collectors.toList()
                ));

        List<PdfCropConfig> pdfConfigs = new ArrayList<>();

        for (Map.Entry<Integer, List<SelectionArea>> pdfEntry : selectionsByPdf.entrySet()) {
            int pdfNumber = pdfEntry.getKey();
            String fileName = getPdfFileName(workspace, pdfNumber);
            List<PageCropConfig> pageConfigs = createPageConfigs(pdfEntry.getValue());

            pdfConfigs.add(new PdfCropConfig(pdfNumber, fileName, pageConfigs));
        }

        return new SelectionConfig(pdfConfigs);
    }

    private List<PageCropConfig> createPageConfigs(List<SelectionArea> selections) throws IOException {
        Map<PdfPage, List<SelectionArea>> selectionsByPage = selections.stream()
                .collect(Collectors.groupingBy(
                        SelectionArea::page,
                        LinkedHashMap::new,
                        Collectors.toList()
                ));

        List<PageCropConfig> pageConfigs = new ArrayList<>();

        for (Map.Entry<PdfPage, List<SelectionArea>> pageEntry : selectionsByPage.entrySet()) {
            PdfPage page = pageEntry.getKey();
            BufferedImage sourceImage = readImage(page.imagePath());

            List<CropAreaConfig> cropAreas = pageEntry.getValue()
                    .stream()
                    .map(area -> convertToSourcePixels(area, sourceImage))
                    .toList();

            pageConfigs.add(new PageCropConfig(
                    page.pageNumber(),
                    page.imagePath().getFileName().toString(),
                    cropAreas
            ));
        }

        return pageConfigs;
    }

    private CropAreaConfig convertToSourcePixels(SelectionArea area, BufferedImage sourceImage) {
        if (area.viewportWidth() <= 0 || area.viewportHeight() <= 0) {
            throw new IllegalStateException("Dimensões da visualização inválidas para a seleção " + area.id());
        }

        double scaleX = sourceImage.getWidth() / area.viewportWidth();
        double scaleY = sourceImage.getHeight() / area.viewportHeight();

        int x = (int) Math.round(area.x() * scaleX);
        int y = (int) Math.round(area.y() * scaleY);
        int width = (int) Math.round(area.width() * scaleX);
        int height = (int) Math.round(area.height() * scaleY);

        x = Math.clamp(x, 0, sourceImage.getWidth() - 1);
        y = Math.clamp(y, 0, sourceImage.getHeight() - 1);
        width = Math.clamp(width, 1, sourceImage.getWidth() - x);
        height = Math.clamp(height, 1, sourceImage.getHeight() - y);

        return new CropAreaConfig(area.id(), x, y, width, height);
    }

    private BufferedImage readImage(Path imagePath) throws IOException {
        BufferedImage image = ImageIO.read(imagePath.toFile());

        if (image == null) {
            throw new IOException("Não foi possível ler a imagem " + imagePath);
        }

        return image;
    }

    private String getPdfFileName(Workspace workspace, int pdfNumber) {
        int index = pdfNumber - 1;
        List<File> pdfFiles = workspace.getSelectedPdfs();

        if (index < 0 || index >= pdfFiles.size()) {
            return "PDF " + pdfNumber;
        }

        return pdfFiles.get(index).getName();
    }
}