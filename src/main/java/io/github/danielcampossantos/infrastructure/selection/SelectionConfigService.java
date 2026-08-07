package io.github.danielcampossantos.infrastructure.selection;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import io.github.danielcampossantos.application.workspace.Workspace;
import io.github.danielcampossantos.domain.pdf.PdfPage;
import io.github.danielcampossantos.domain.selection.*;
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

    public Path write(
            Workspace workspace,
            List<SelectionAssignment> assignments
    ) throws IOException {
        List<SelectionAssignment> orderedAssignments = assignments.stream()
                .sorted(SelectionAssignment.order())
                .toList();

        SelectionConfig config = createConfig(workspace, orderedAssignments);
        Path configPath = workspace.getTemporaryDirectory().resolve(CONFIG_FILE_NAME);

        objectMapper.writeValue(configPath.toFile(), config);

        log.info("Configuração das seleções criada em {}", configPath);

        return configPath;
    }

    private SelectionConfig createConfig(
            Workspace workspace,
            List<SelectionAssignment> assignments
    ) throws IOException {
        Map<Integer, List<SelectionAssignment>> assignmentsByPdf = assignments.stream()
                .collect(Collectors.groupingBy(
                        assignment -> assignment.page().pdfNumber(),
                        LinkedHashMap::new,
                        Collectors.toList()
                ));

        List<PdfCropConfig> pdfConfigs = new ArrayList<>();

        for (Map.Entry<Integer, List<SelectionAssignment>> pdfEntry : assignmentsByPdf.entrySet()) {
            int pdfNumber = pdfEntry.getKey();
            String fileName = getPdfFileName(workspace, pdfNumber);
            List<PageCropConfig> pageConfigs = createPageConfigs(pdfNumber, pdfEntry.getValue());

            pdfConfigs.add(new PdfCropConfig(pdfNumber, fileName, pageConfigs));
        }

        return new SelectionConfig(List.copyOf(pdfConfigs));
    }

    private List<PageCropConfig> createPageConfigs(
            int pdfNumber,
            List<SelectionAssignment> assignments
    ) throws IOException {
        Map<PdfPage, List<SelectionAssignment>> assignmentsByPage = assignments.stream()
                .collect(Collectors.groupingBy(
                        SelectionAssignment::page,
                        LinkedHashMap::new,
                        Collectors.toList()
                ));

        List<PageCropConfig> pageConfigs = new ArrayList<>();

        for (Map.Entry<PdfPage, List<SelectionAssignment>> pageEntry : assignmentsByPage.entrySet()) {
            PdfPage page = pageEntry.getKey();
            BufferedImage sourceImage = readImage(page.imagePath());

            List<CropAreaConfig> cropAreas = pageEntry.getValue()
                    .stream()
                    .map(assignment -> convertToSourcePixels(pdfNumber, page, assignment, sourceImage))
                    .toList();

            pageConfigs.add(new PageCropConfig(
                    page.pageNumber(),
                    page.imagePath().getFileName().toString(),
                    cropAreas
            ));
        }

        return List.copyOf(pageConfigs);
    }

    private CropAreaConfig convertToSourcePixels(
            int pdfNumber,
            PdfPage page,
            SelectionAssignment assignment,
            BufferedImage sourceImage
    ) {
        SelectionArea area = assignment.area();

        if (area.viewportWidth() <= 0 || area.viewportHeight() <= 0) {
            throw new IllegalStateException(
                    "Dimensões da visualização inválidas para a seleção " + area.id()
            );
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

        String outputImage = "crops/pdf-%d/pagina-%d-selecao-%04d-%s.png".formatted(
                pdfNumber,
                page.pageNumber(),
                assignment.selectionOrder(),
                area.id().toString().substring(0, 8)
        );

        return new CropAreaConfig(
                area.id(),
                assignment.selectionOrder(),
                x,
                y,
                width,
                height,
                outputImage,
                assignment.destination()
        );
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
