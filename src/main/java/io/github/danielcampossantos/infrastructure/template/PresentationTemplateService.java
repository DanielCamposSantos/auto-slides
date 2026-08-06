package io.github.danielcampossantos.infrastructure.template;

import io.github.danielcampossantos.domain.template.PresentationSlideItem;
import io.github.danielcampossantos.domain.template.PresentationTemplateInfo;
import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.apache.poi.xslf.usermodel.XSLFSlide;

import java.awt.*;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class PresentationTemplateService {

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

    public List<PresentationSlideItem> readSlides(Path templatePath) throws IOException {
        validatePath(templatePath);

        try (
                InputStream input = Files.newInputStream(templatePath);
                XMLSlideShow presentation = new XMLSlideShow(input)
        ) {
            List<PresentationSlideItem> slides = new ArrayList<>();

            for (int index = 0; index < presentation.getSlides().size(); index++) {
                XSLFSlide slide = presentation.getSlides().get(index);

                slides.add(new PresentationSlideItem(
                        index + 1,
                        resolveTitle(slide, index + 1),
                        resolveDescription(slide)
                ));
            }

            return List.copyOf(slides);
        } catch (RuntimeException exception) {
            throw new IOException("Não foi possível ler os slides do template selecionado.", exception);
        }
    }

    private void validatePath(Path templatePath) throws IOException {
        if (templatePath == null) {
            throw new IOException("Nenhum template foi informado.");
        }

        if (!Files.isRegularFile(templatePath)) {
            throw new IOException("O arquivo do template não existe.");
        }

        String fileName = templatePath.getFileName().toString().toLowerCase(Locale.ROOT);

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