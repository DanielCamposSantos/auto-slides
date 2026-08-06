package io.github.danielcampossantos.ui.window;

import javafx.geometry.VPos;
import javafx.scene.SnapshotParameters;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;

public final class AppIconFactory {

    private static final Color BACKGROUND_COLOR = Color.web("#5865F2");
    private static final Color TEXT_COLOR = Color.WHITE;

    private AppIconFactory() {
    }

    public static Image create(int size) {
        Canvas canvas = new Canvas(size, size);
        GraphicsContext graphics = canvas.getGraphicsContext2D();

        double radius = size * 0.25;

        graphics.setFill(BACKGROUND_COLOR);
        graphics.fillRoundRect(0, 0, size, size, radius, radius);

        graphics.setFill(TEXT_COLOR);
        graphics.setFont(Font.font("System", FontWeight.BOLD, size * 0.58));
        graphics.setTextAlign(TextAlignment.CENTER);
        graphics.setTextBaseline(VPos.CENTER);
        graphics.fillText("A", size / 2.0, size / 2.0 + size * 0.025);

        SnapshotParameters parameters = new SnapshotParameters();
        parameters.setFill(Color.TRANSPARENT);

        WritableImage image = new WritableImage(size, size);

        return canvas.snapshot(parameters, image);
    }
}