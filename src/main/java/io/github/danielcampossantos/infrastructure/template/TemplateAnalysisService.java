package io.github.danielcampossantos.infrastructure.template;

import io.github.danielcampossantos.domain.template.SlotFitMode;
import io.github.danielcampossantos.domain.template.TemplateLayout;
import io.github.danielcampossantos.domain.template.TemplateSlide;
import io.github.danielcampossantos.domain.template.TemplateSlot;
import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.apache.poi.xslf.usermodel.XSLFShape;
import org.apache.poi.xslf.usermodel.XSLFSlide;

import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;

public final class TemplateAnalysisService {

    private static final String SLOT_PREFIX = "AUTO_SLOT::";

    public TemplateLayout analyze(Path templatePath) throws IOException {
        validateTemplate(templatePath);

        try (
                InputStream input = Files.newInputStream(templatePath);
                XMLSlideShow presentation = new XMLSlideShow(input)
        ) {
            Dimension pageSize = presentation.getPageSize();
            List<TemplateSlide> slides = new ArrayList<>();

            for (int index = 0; index < presentation.getSlides().size(); index++) {
                XSLFSlide slide = presentation.getSlides().get(index);
                int slideNumber = index + 1;

                slides.add(
                        new TemplateSlide(
                                "slide-" + slideNumber,
                                slideNumber,
                                resolveTitle(slide, slideNumber),
                                readSlots(slide)
                        )
                );
            }

            return new TemplateLayout(
                    createTemplateId(templatePath),
                    1,
                    templatePath.getFileName().toString(),
                    templatePath.toAbsolutePath().normalize().toString(),
                    pageSize.getWidth(),
                    pageSize.getHeight(),
                    slides
            );
        } catch (IOException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw new IOException(
                    "Não foi possível analisar o template PowerPoint.",
                    exception
            );
        }
    }

    private List<TemplateSlot> readSlots(XSLFSlide slide) {
        List<TemplateSlot> slots = new ArrayList<>();

        for (XSLFShape shape : slide.getShapes()) {
            String shapeName = shape.getShapeName();

            if (shapeName == null || !shapeName.startsWith(SLOT_PREFIX)) {
                continue;
            }

            Rectangle2D anchor = shape.getAnchor();
            SlotName slotName = parseSlotName(shapeName);

            slots.add(
                    new TemplateSlot(
                            slotName.id(),
                            slotName.label(),
                            shapeName,
                            anchor.getX(),
                            anchor.getY(),
                            anchor.getWidth(),
                            anchor.getHeight(),
                            false,
                            1,
                            SlotFitMode.CONTAIN
                    )
            );
        }

        return List.copyOf(slots);
    }

    private SlotName parseSlotName(String shapeName) {
        String content = shapeName.substring(SLOT_PREFIX.length());
        String[] parts = content.split("::", 2);

        String id = normalizeId(parts[0]);

        String label = parts.length == 2 && !parts[1].isBlank()
                ? parts[1].strip()
                : id;

        return new SlotName(id, label);
    }

    private String normalizeId(String value) {
        String normalized = value.strip()
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9-]+", "-")
                .replaceAll("^-|-$", "");

        if (normalized.isBlank()) {
            throw new IllegalArgumentException(
                    "Foi encontrada uma forma AUTO_SLOT sem identificador válido."
            );
        }

        return normalized;
    }

    private String resolveTitle(XSLFSlide slide, int slideNumber) {
        String title = slide.getTitle();

        if (title == null || title.isBlank()) {
            return "Slide " + slideNumber;
        }

        return title.strip();
    }

    private String createTemplateId(Path templatePath) throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");

            try (InputStream input = Files.newInputStream(templatePath)) {
                byte[] buffer = new byte[8192];
                int bytesRead;

                while ((bytesRead = input.read(buffer)) != -1) {
                    digest.update(buffer, 0, bytesRead);
                }
            }

            return HexFormat.of()
                    .formatHex(digest.digest())
                    .substring(0, 16);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(
                    "O algoritmo SHA-256 não está disponível.",
                    exception
            );
        }
    }

    private void validateTemplate(Path templatePath) throws IOException {
        if (templatePath == null) {
            throw new IOException("Nenhum template foi informado.");
        }

        if (!Files.isRegularFile(templatePath)) {
            throw new IOException("O template selecionado não existe.");
        }

        String fileName = templatePath.getFileName()
                .toString()
                .toLowerCase(Locale.ROOT);

        if (!fileName.endsWith(".pptx")) {
            throw new IOException(
                    "O template deve estar no formato PowerPoint .pptx."
            );
        }
    }

    private record SlotName(
            String id,
            String label
    ) {
    }
}