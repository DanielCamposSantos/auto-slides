package io.github.danielcampossantos.ui.selection;

import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Group;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import lombok.Getter;
import lombok.Setter;
import org.kordamp.ikonli.javafx.FontIcon;
import org.kordamp.ikonli.materialdesign2.MaterialDesignC;

public final class SelectionView extends Group {

    @Getter
    private final Rectangle rectangle;

    private final StackPane removeButton;

    @Setter
    private Runnable onRemove;

    public SelectionView() {

        rectangle = new Rectangle();
        removeButton = new StackPane();

        initialize();

    }

    private void initialize() {

        rectangle.setFill(Color.rgb(88, 101, 242, 0.18));
        rectangle.setStroke(Color.web("#5865F2"));
        rectangle.setStrokeWidth(2);

        FontIcon icon = new FontIcon(MaterialDesignC.CLOSE);

        icon.setIconColor(Color.WHITE);
        icon.setIconSize(12);

        removeButton.setAlignment(Pos.CENTER);

        removeButton.setPrefSize(20, 20);
        removeButton.setMinSize(20, 20);
        removeButton.setMaxSize(20, 20);

        removeButton.setCursor(Cursor.HAND);

        removeButton.setStyle("""
                -fx-background-color: #ED4245;
                -fx-background-radius: 0 0 0 7;
                -fx-border-color: #5865F2;
                -fx-border-width: 0 0 2 2;
                -fx-border-radius: 0 0 0 7;
                """);

        removeButton.getChildren().add(icon);

        removeButton.setVisible(false);

        rectangle.setOnMouseEntered(event -> removeButton.setVisible(true));

        rectangle.setOnMouseExited(event -> {

            if (!removeButton.isHover()) {
                removeButton.setVisible(false);
            }

        });

        removeButton.setOnMouseEntered(event -> removeButton.setVisible(true));

        removeButton.setOnMouseExited(event -> removeButton.setVisible(false));

        removeButton.setOnMouseClicked(event -> {

            event.consume();

            if (onRemove != null) {
                onRemove.run();
            }

        });

        getChildren().addAll(rectangle, removeButton);

    }

    public void setBounds(double x, double y, double width, double height) {

        rectangle.setX(x);
        rectangle.setY(y);

        rectangle.setWidth(width);
        rectangle.setHeight(height);

        removeButton.relocate(x + width - 20, y);

    }

    public double getWidth() {

        return rectangle.getWidth();

    }

    public double getHeight() {

        return rectangle.getHeight();

    }

}