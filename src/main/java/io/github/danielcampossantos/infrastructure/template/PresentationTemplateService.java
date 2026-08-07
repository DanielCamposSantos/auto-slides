package io.github.danielcampossantos.infrastructure.template;

import io.github.danielcampossantos.domain.template.PresentationSlideItem;
import io.github.danielcampossantos.domain.template.PresentationTemplateInfo;
import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.apache.poi.xslf.usermodel.XSLFSlide;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.List;

public final class PresentationTemplateService {

    private static final int THUMBNAIL_WIDTH = 840;
    private static final int THUMBNAIL_HEIGHT = 472;

    public PresentationTemplateInfo inspect(Path templatePath) throws IOException {
        validatePath(templatePath);

        try (
                InputStream input = Files.newInputStream(templatePath);
                XMLSlideShow presentation = new XMLSlideShow(input)
        ) {
            Dimension pageSize = presentation.getPageSize();

            return new PresentationTemplateInfo(
                    templatePath.toAbsolutePath().normalize(),
                    templatePath.getFileName().toString(),
                    presentation.getSlides().size(),
                    pageSize.getWidth(),
                    pageSize.getHeight()
            );
        } catch (RuntimeException exception) {
            throw new IOException(
                    "O arquivo selecionado não é uma apresentação PowerPoint válida.",
                    exception
            );
        }
    }

    public List<PresentationSlideItem> readSlides(
            Path presentationPath,
            Path temporaryDirectory
    ) throws IOException {
        validatePath(presentationPath);

        Path thumbnailDirectory = temporaryDirectory.resolve("presentation-thumbnails");

        clearDirectory(thumbnailDirectory);
        Files.createDirectories(thumbnailDirectory);

        try (
                InputStream input = Files.newInputStream(presentationPath);
                XMLSlideShow presentation = new XMLSlideShow(input)
        ) {
            List<PresentationSlideItem> slides = new ArrayList<>();

            for (int index = 0; index < presentation.getSlides().size(); index++) {
                XSLFSlide slide = presentation.getSlides().get(index);
                int slideNumber = index + 1;

                Path thumbnailPath = renderThumbnail(
                        presentation,
                        slide,
                        thumbnailDirectory,
                        slideNumber
                );

                slides.add(new PresentationSlideItem(
                        UUID.randomUUID(),
                        slideNumber,
                        index,
                        resolveTitle(slide, slideNumber),
                        resolveDescription(slide),
                        thumbnailPath
                ));
            }

            return List.copyOf(slides);
        } catch (RuntimeException exception) {
            throw new IOException(
                    "Não foi possível renderizar a apresentação gerada.",
                    exception
            );
        }
    }

    private Path renderThumbnail(
            XMLSlideShow presentation,
            XSLFSlide slide,
            Path thumbnailDirectory,
            int slideNumber
    ) throws IOException {
        Dimension pageSize = presentation.getPageSize();

        BufferedImage image = new BufferedImage(
                THUMBNAIL_WIDTH,
                THUMBNAIL_HEIGHT,
                BufferedImage.TYPE_INT_ARGB
        );

        Graphics2D graphics = image.createGraphics();

        try {
            graphics.setRenderingHint(
                    RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON
            );

            graphics.setRenderingHint(
                    RenderingHints.KEY_RENDERING,
                    RenderingHints.VALUE_RENDER_QUALITY
            );

            graphics.setRenderingHint(
                    RenderingHints.KEY_INTERPOLATION,
                    RenderingHints.VALUE_INTERPOLATION_BICUBIC
            );

            graphics.setPaint(Color.WHITE);
            graphics.fill(new Rectangle2D.Double(
                    0,
                    0,
                    THUMBNAIL_WIDTH,
                    THUMBNAIL_HEIGHT
            ));

            double scaleX = THUMBNAIL_WIDTH / pageSize.getWidth();
            double scaleY = THUMBNAIL_HEIGHT / pageSize.getHeight();

            graphics.scale(scaleX, scaleY);
            slide.draw(graphics);
        } finally {
            graphics.dispose();
        }

        Path thumbnailPath = thumbnailDirectory.resolve(
                "slide-%03d.png".formatted(slideNumber)
        );

        if (!ImageIO.write(image, "PNG", thumbnailPath.toFile())) {
            throw new IOException("Não foi possível gerar a miniatura " + thumbnailPath);
        }

        return thumbnailPath;
    }

    private void clearDirectory(Path directory) throws IOException {
        if (!Files.exists(directory)) {
            return;
        }

        try (var paths = Files.walk(directory)) {
            for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) {
                Files.deleteIfExists(path);
            }
        }
    }

    private void validatePath(Path presentationPath) throws IOException {
        if (presentationPath == null) {
            throw new IOException("Nenhuma apresentação foi informada.");
        }

        if (!Files.isRegularFile(presentationPath)) {
            throw new IOException("O arquivo da apresentação não existe: " + presentationPath);
        }

        String fileName = presentationPath.getFileName()
                .toString()
                .toLowerCase(Locale.ROOT);

        if (!fileName.endsWith(".pptx")) {
            throw new IOException("O arquivo deve estar no formato PowerPoint .pptx.");
        }
    }

    private String resolveTitle(XSLFSlide slide, int slideNumber) {
        String title = slide.getTitle();

        if (title == null || title.isBlank()) {
            return "Slide " + slideNumber;
        }

        return title.strip();
    }

    private String resolveDescription(XSLFSlide slide) {
        int shapeCount = slide.getShapes().size();

        return shapeCount == 1
                ? "1 elemento na apresentação gerada."
                : shapeCount + " elementos na apresentação gerada.";
    }
}
