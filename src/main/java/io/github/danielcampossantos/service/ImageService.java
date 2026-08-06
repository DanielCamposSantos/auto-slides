package io.github.danielcampossantos.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.danielcampossantos.config.CutConfig;
import io.github.danielcampossantos.model.PageModel;
import io.github.danielcampossantos.model.PdfConfigModel;
import lombok.extern.log4j.Log4j2;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;

@Log4j2
public class ImageService {
    private final ObjectMapper objectMapper = new ObjectMapper();

    public void recortarImagensTemporarias(Path pastaTemporaria, String prefixo) throws IOException {
        ClassLoader classLoader = getClass().getClassLoader();

        try (InputStream inputStream = classLoader.getResourceAsStream("config/cut-config.json")) {
            if (inputStream == null) {
                throw new IOException("Configuracao nao encontrada");
            }

            CutConfig cutConfig = objectMapper.readValue(inputStream, CutConfig.class);
            PdfConfigModel configModel = cutConfig.getConfig(prefixo);

            if (configModel == null || configModel.paginas() == null) {
                log.warn("Nenhuma configuracao encontrada para o prefixo: {}", prefixo);
                return;
            }

            for (PageModel page : configModel.paginas()) {
                String nomeImagemOriginal = prefixo + "-imagem-pagina-" + page.numeroPagina() + ".png";
                File arquivoOriginal = pastaTemporaria.resolve(nomeImagemOriginal).toFile();

                if (!arquivoOriginal.exists()) {
                    log.warn("Pagina {} nao existe no PDF, ignorando", page.numeroPagina());
                    continue;
                }

                recortarESalvar(arquivoOriginal, page, pastaTemporaria, prefixo);
            }
        }
    }

    private void recortarESalvar(File arquivoOriginal, PageModel page, Path pastaTemporaria, String prefixo) throws IOException {
        BufferedImage imagemOriginal = ImageIO.read(arquivoOriginal);

        double multiplicadorEscala = 300.0 / 72.0;

        int xReal = (int) Math.round(page.x() * multiplicadorEscala);
        int yReal = (int) Math.round(page.y() * multiplicadorEscala);
        int larguraReal = (int) Math.round(page.largura() * multiplicadorEscala);
        int alturaReal = (int) Math.round(page.altura() * multiplicadorEscala);

        int x = Math.min(xReal, imagemOriginal.getWidth() - 1);
        int y = Math.min(yReal, imagemOriginal.getHeight() - 1);
        int largura = Math.min(larguraReal, imagemOriginal.getWidth() - x);
        int altura = Math.min(alturaReal, imagemOriginal.getHeight() - y);

        if (largura <= 0 || altura <= 0) {
            return;
        }

        BufferedImage imagemRecortada = imagemOriginal.getSubimage(x, y, largura, altura);
        String nomeDestino = prefixo + "-imagem-pagina-" + page.numeroPagina() + "_recortada.png";
        File arquivoDestino = pastaTemporaria.resolve(nomeDestino).toFile();

        ImageIO.write(imagemRecortada, "PNG", arquivoDestino);
        log.info("Recorte criado: {}", arquivoDestino.getName());
    }
}
