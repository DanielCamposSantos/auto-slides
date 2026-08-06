package io.github.danielcampossantos.utils;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class FileUtils {

    public static List<File> getPdfs() throws IOException {
        ClassLoader classLoader = FileUtils.class.getClassLoader();
        URL resourceUrl = classLoader.getResource("pdfs");

        if (resourceUrl == null) {
            throw new IOException("Pasta pdfs nao encontrada");
        }

        try {
            URI uri = resourceUrl.toURI();
            Path pastaPdfs = Paths.get(uri);

            try (Stream<Path> arquivos = Files.list(pastaPdfs)) {
                return arquivos
                        .filter(Files::isRegularFile)
                        .filter(p -> p.toString().toLowerCase().endsWith(".pdf"))
                        .map(Path::toFile)
                        .sorted()
                        .collect(Collectors.toList());
            }
        } catch (URISyntaxException e) {
            throw new IOException(e);
        }
    }

    public static void deletarDiretorioRecursivo(Path caminho) throws IOException {
        if (caminho == null || !Files.exists(caminho)) {
            return;
        }
        try (Stream<Path> caminhos = Files.walk(caminho)) {
            caminhos.sorted((p1, p2) -> p2.compareTo(p1))
                    .map(Path::toFile)
                    .forEach(File::delete);
        }
    }
}
