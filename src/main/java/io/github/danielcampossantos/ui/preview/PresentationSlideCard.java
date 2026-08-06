package io.github.danielcampossantos.ui.preview;

import io.github.danielcampossantos.domain.template.PresentationSlideItem;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import lombok.Getter;
import org.kordamp.ikonli.javafx.FontIcon;
import org.kordamp.ikonli.materialdesign2.MaterialDesignC;
import org.kordamp.ikonli.materialdesign2.MaterialDesignD;

import java.util.function.Consumer;

public final class PresentationSlideCard extends HBox {

    @Getter
    private final PresentationSlideItem item;

    private final Consumer<PresentationSlideItem> removeAction;

    private final Consumer<PresentationSlideItem> duplicateAction;

    public PresentationSlideCard(
            PresentationSlideItem item,
            Consumer<PresentationSlideItem> removeAction,
            Consumer<PresentationSlideItem> duplicateAction
    ) {
        this.item = item;
        this.removeAction = removeAction;
        this.duplicateAction = duplicateAction;

        initialize();
    }

    private void initialize() {
        setAlignment(Pos.CENTER_LEFT);
        setSpacing(20);
        setPadding(new Insets(18));
        getStyleClass().add("presentation-slide-card");

        StackPane slidePreview = createSlidePreview();
        VBox information = createInformation();
        VBox actions = createActions();

        HBox.setHgrow(information, Priority.ALWAYS);

        getChildren().addAll(slidePreview, information, actions);
    }

    private StackPane createSlidePreview() {
        StackPane preview = new StackPane();
        preview.setPrefSize(420, 236);
        preview.setMinSize(420, 236);
        preview.setMaxSize(420, 236);
        preview.getStyleClass().add("slide-preview");

        if (item.thumbnailPath() != null) {
            Image image = new Image(
                    item.thumbnailPath().toUri().toString(),
                    420,
                    236,
                    true,
                    true
            );

            ImageView imageView = new ImageView(image);

            imageView.setFitWidth(420);
            imageView.setFitHeight(236);
            imageView.setPreserveRatio(true);
            imageView.setSmooth(true);

            preview.getChildren().add(imageView);
        }

        Label numberBadge = new Label(String.valueOf(item.slideNumber()));
        numberBadge.getStyleClass().add("slide-number-badge");

        StackPane.setAlignment(numberBadge, Pos.BOTTOM_RIGHT);
        StackPane.setMargin(numberBadge, new Insets(0, 10, 10, 0));

        preview.getChildren().add(numberBadge);

        return preview;
    }

    private VBox createInformation() {
        VBox information = new VBox(8);
        information.setAlignment(Pos.CENTER_LEFT);

        Label title = new Label(item.title());
        title.getStyleClass().add("slide-card-title");

        Label description = new Label(item.description());
        description.setWrapText(true);
        description.getStyleClass().add("slide-card-description");

        String statusText = item.copyNumber() == 1
                ? "Slide original do template"
                : "Cópia do slide " + item.sourceSlideNumber();

        Label status = new Label(statusText);
        status.getStyleClass().add("slide-card-status");

        information.getChildren().addAll(title, description, status);

        return information;
    }

    private VBox createActions() {
        VBox actions = new VBox(10);
        actions.setAlignment(Pos.CENTER);

        Button duplicateButton = createDuplicateButton();
        Button removeButton = createRemoveButton();

        actions.getChildren().addAll(duplicateButton, removeButton);

        return actions;
    }

    private Button createDuplicateButton() {
        FontIcon icon = new FontIcon(MaterialDesignC.CONTENT_COPY);
        icon.setIconSize(20);

        Button button = new Button();
        button.setGraphic(icon);
        button.setCursor(Cursor.HAND);
        button.setFocusTraversable(false);
        button.getStyleClass().add("slide-duplicate-button");
        button.setOnAction(event -> duplicateAction.accept(item));

        return button;
    }

    private Button createRemoveButton() {
        FontIcon icon = new FontIcon(MaterialDesignD.DELETE_OUTLINE);
        icon.setIconSize(21);

        Button button = new Button();
        button.setGraphic(icon);
        button.setCursor(Cursor.HAND);
        button.setFocusTraversable(false);
        button.getStyleClass().add("slide-remove-button");
        button.setOnAction(event -> removeAction.accept(item));

        return button;
    }
}