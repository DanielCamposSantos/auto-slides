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
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

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
            throw new IOException("O arquivo selecionado não é um template PowerPoint válido.", exception);
        }
    }

    public List<PresentationSlideItem> readSlides(
            Path templatePath,
            Path temporaryDirectory
    ) throws IOException {
        validatePath(templatePath);

        Path thumbnailDirectory = temporaryDirectory.resolve("template-thumbnails");

        Files.createDirectories(thumbnailDirectory);

        try (
                InputStream input = Files.newInputStream(templatePath);
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
                        slideNumber,
                        1,
                        resolveTitle(slide, slideNumber),
                        resolveDescription(slide),
                        thumbnailPath
                ));
            }

            return List.copyOf(slides);
        } catch (RuntimeException exception) {
            throw new IOException("Não foi possível ler os slides do template selecionado.", exception);
        }
    }

    public PresentationSlideItem duplicate(
            PresentationSlideItem source,
            int newSlideNumber
    ) {
        return new PresentationSlideItem(
                UUID.randomUUID(),
                newSlideNumber,
                source.sourceSlideNumber(),
                source.copyNumber() + 1,
                source.title() + " — cópia " + (source.copyNumber() + 1),
                source.description(),
                source.thumbnailPath()
        );
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

            graphics.setPaint(Color.WHITE);
            graphics.fill(new Rectangle2D.Double(0, 0, THUMBNAIL_WIDTH, THUMBNAIL_HEIGHT));

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

        ImageIO.write(image, "PNG", thumbnailPath.toFile());

        return thumbnailPath;
    }

    private void validatePath(Path templatePath) throws IOException {
        if (templatePath == null) {
            throw new IOException("Nenhum template foi informado.");
        }

        if (!Files.isRegularFile(templatePath)) {
            throw new IOException("O arquivo do template não existe.");
        }

        String fileName = templatePath.getFileName()
                .toString()
                .toLowerCase(Locale.ROOT);

        if (!fileName.endsWith(".pptx")) {
            throw new IOException("Selecione um arquivo PowerPoint no formato .pptx.");
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
                ? "1 elemento encontrado no template."
                : shapeCount + " elementos encontrados no template.";
    }
}