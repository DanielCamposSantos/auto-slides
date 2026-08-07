package io.github.danielcampossantos.infrastructure.template;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.danielcampossantos.domain.selection.*;
import io.github.danielcampossantos.domain.template.SlotFitMode;
import lombok.extern.log4j.Log4j2;
import org.apache.poi.sl.usermodel.PictureData;
import org.apache.poi.xslf.usermodel.*;

import javax.imageio.ImageIO;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

@Log4j2
public final class PresentationGenerationService {

    private static final String GENERATED_PRESENTATION_NAME = "generated-presentation.pptx";
    private static final String SLOT_PREFIX = "AUTO_SLOT::";

    private final ObjectMapper objectMapper = new ObjectMapper();

    public Path generate(
            Path templatePath,
            Path configPath,
            Path temporaryDirectory
    ) throws IOException {
        SelectionConfig config = objectMapper.readValue(
                configPath.toFile(),
                SelectionConfig.class
        );

        List<CropPlacement> placements = flatten(config);
        Path generatedPath = temporaryDirectory.resolve(GENERATED_PRESENTATION_NAME);

        try (
                InputStream input = Files.newInputStream(templatePath);
                XMLSlideShow presentation = new XMLSlideShow(input)
        ) {
            Map<SlideKey, XSLFSlide> slideInstances = createRequiredSlideInstances(
                    presentation,
                    placements
            );

            for (CropPlacement placement : placements) {
                insertCrop(
                        presentation,
                        slideInstances,
                        temporaryDirectory,
                        placement
                );
            }

            removeRemainingSlotShapes(presentation);

            try (OutputStream output = Files.newOutputStream(generatedPath)) {
                presentation.write(output);
            }
        } catch (RuntimeException exception) {
            throw new IOException(
                    "Não foi possível montar a apresentação a partir do template.",
                    exception
            );
        }

        log.info("Apresentação gerada em {}", generatedPath);

        return generatedPath;
    }

    public void exportWithoutSlides(
            Path generatedPresentationPath,
            Set<Integer> removedSlideIndexes,
            Path destinationPath
    ) throws IOException {
        try (
                InputStream input = Files.newInputStream(generatedPresentationPath);
                XMLSlideShow presentation = new XMLSlideShow(input)
        ) {
            TreeSet<Integer> descendingIndexes = new TreeSet<>(Comparator.reverseOrder());
            descendingIndexes.addAll(removedSlideIndexes);

            for (int slideIndex : descendingIndexes) {
                if (slideIndex >= 0 && slideIndex < presentation.getSlides().size()) {
                    presentation.removeSlide(slideIndex);
                }
            }

            Path parent = destinationPath.toAbsolutePath().normalize().getParent();

            if (parent != null) {
                Files.createDirectories(parent);
            }

            try (OutputStream output = Files.newOutputStream(destinationPath)) {
                presentation.write(output);
            }
        }

        log.info("Apresentação exportada em {}", destinationPath);
    }

    private Map<SlideKey, XSLFSlide> createRequiredSlideInstances(
            XMLSlideShow presentation,
            List<CropPlacement> placements
    ) throws IOException {
        List<XSLFSlide> originalSlides = new ArrayList<>(presentation.getSlides());
        Map<Integer, Integer> maximumInstances = resolveMaximumInstances(placements);
        Map<SlideKey, XSLFSlide> result = new HashMap<>();

        int targetIndex = 0;

        for (int sourceIndex = 0; sourceIndex < originalSlides.size(); sourceIndex++) {
            int sourceSlideNumber = sourceIndex + 1;
            XSLFSlide sourceSlide = originalSlides.get(sourceIndex);
            int instanceCount = maximumInstances.getOrDefault(sourceSlideNumber, 1);

            presentation.setSlideOrder(sourceSlide, targetIndex);
            result.put(new SlideKey(sourceSlideNumber, 1), sourceSlide);

            for (int instance = 2; instance <= instanceCount; instance++) {
                XSLFSlide copy = presentation.createSlide(sourceSlide.getSlideLayout());

                copy.importContent(sourceSlide);
                presentation.setSlideOrder(copy, targetIndex + instance - 1);

                result.put(new SlideKey(sourceSlideNumber, instance), copy);
            }

            targetIndex += instanceCount;
        }

        for (CropPlacement placement : placements) {
            SelectionDestination destination = placement.crop().destination();
            SlideKey key = new SlideKey(
                    destination.sourceSlideNumber(),
                    destination.slideInstance()
            );

            if (!result.containsKey(key)) {
                throw new IOException(
                        "Não foi possível criar a instância %d do slide %d."
                                .formatted(key.instance(), key.sourceSlideNumber())
                );
            }
        }

        return result;
    }

    private Map<Integer, Integer> resolveMaximumInstances(
            List<CropPlacement> placements
    ) {
        Map<Integer, Integer> result = new LinkedHashMap<>();

        for (CropPlacement placement : placements) {
            SelectionDestination destination = placement.crop().destination();

            result.merge(
                    destination.sourceSlideNumber(),
                    destination.slideInstance(),
                    Math::max
            );
        }

        return result;
    }

    private void insertCrop(
            XMLSlideShow presentation,
            Map<SlideKey, XSLFSlide> slideInstances,
            Path temporaryDirectory,
            CropPlacement placement
    ) throws IOException {
        CropAreaConfig crop = placement.crop();
        SelectionDestination destination = crop.destination();
        SlideKey slideKey = new SlideKey(
                destination.sourceSlideNumber(),
                destination.slideInstance()
        );

        XSLFSlide slide = slideInstances.get(slideKey);

        if (slide == null) {
            throw new IOException(
                    "Slide de destino não encontrado: slide %d, instância %d."
                            .formatted(slideKey.sourceSlideNumber(), slideKey.instance())
            );
        }

        Path imagePath = temporaryDirectory.resolve(crop.outputImage());

        if (!Files.isRegularFile(imagePath)) {
            throw new IOException("Imagem recortada não encontrada: " + imagePath);
        }

        removeSlotShape(slide, destination.slotShapeName());

        BufferedImage image = ImageIO.read(imagePath.toFile());

        if (image == null) {
            throw new IOException("Não foi possível ler o recorte: " + imagePath);
        }

        Rectangle2D slot = new Rectangle2D.Double(
                destination.x(),
                destination.y(),
                destination.width(),
                destination.height()
        );

        Rectangle2D anchor = resolveAnchor(
                image.getWidth(),
                image.getHeight(),
                slot,
                destination.fitMode()
        );

        XSLFPictureData pictureData = presentation.addPicture(
                imagePath.toFile(),
                PictureData.PictureType.PNG
        );

        XSLFPictureShape picture = slide.createPicture(pictureData);
        picture.setAnchor(anchor);

        log.info(
                "Recorte {} inserido no slide {} instância {} slot {}",
                crop.id(),
                destination.sourceSlideNumber(),
                destination.slideInstance(),
                destination.slotId()
        );
    }

    private Rectangle2D resolveAnchor(
            double imageWidth,
            double imageHeight,
            Rectangle2D slot,
            SlotFitMode fitMode
    ) {
        if (fitMode == SlotFitMode.STRETCH) {
            return slot;
        }

        double widthScale = slot.getWidth() / imageWidth;
        double heightScale = slot.getHeight() / imageHeight;
        double scale = fitMode == SlotFitMode.COVER
                ? Math.max(widthScale, heightScale)
                : Math.min(widthScale, heightScale);

        double width = imageWidth * scale;
        double height = imageHeight * scale;
        double x = slot.getX() + (slot.getWidth() - width) / 2.0;
        double y = slot.getY() + (slot.getHeight() - height) / 2.0;

        return new Rectangle2D.Double(x, y, width, height);
    }

    private void removeSlotShape(
            XSLFSlide slide,
            String shapeName
    ) {
        if (shapeName == null || shapeName.isBlank()) {
            return;
        }

        XSLFShape target = slide.getShapes()
                .stream()
                .filter(shape -> shapeName.equals(shape.getShapeName()))
                .findFirst()
                .orElse(null);

        if (target != null) {
            slide.removeShape(target);
        }
    }

    private void removeRemainingSlotShapes(XMLSlideShow presentation) {
        for (XSLFSlide slide : presentation.getSlides()) {
            List<XSLFShape> shapes = new ArrayList<>(slide.getShapes());

            for (XSLFShape shape : shapes) {
                String name = shape.getShapeName();

                if (name != null && name.startsWith(SLOT_PREFIX)) {
                    slide.removeShape(shape);
                }
            }
        }
    }

    private List<CropPlacement> flatten(SelectionConfig config) {
        List<CropPlacement> placements = new ArrayList<>();

        for (PdfCropConfig pdf : config.pdfs()) {
            for (PageCropConfig page : pdf.pages()) {
                for (CropAreaConfig crop : page.selections()) {
                    placements.add(new CropPlacement(
                            pdf.pdfNumber(),
                            page.pageNumber(),
                            crop
                    ));
                }
            }
        }

        return placements.stream()
                .sorted(
                        Comparator.comparingInt(CropPlacement::pdfNumber)
                                .thenComparingInt(CropPlacement::pageNumber)
                                .thenComparingInt(placement -> placement.crop().selectionOrder())
                )
                .toList();
    }

    private record SlideKey(
            int sourceSlideNumber,
            int instance
    ) {
    }

    private record CropPlacement(
            int pdfNumber,
            int pageNumber,
            CropAreaConfig crop
    ) {
    }
}
