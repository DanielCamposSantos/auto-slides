package io.github.danielcampossantos.ui.view;

import io.github.danielcampossantos.model.PresentationSlideItem;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import lombok.Getter;
import org.kordamp.ikonli.javafx.FontIcon;
import org.kordamp.ikonli.materialdesign2.MaterialDesignD;

import java.util.function.Consumer;

public final class PresentationSlideCard extends HBox {

    @Getter
    private final PresentationSlideItem item;

    private final Consumer<PresentationSlideItem> removeAction;

    public PresentationSlideCard(PresentationSlideItem item, Consumer<PresentationSlideItem> removeAction) {
        this.item = item;
        this.removeAction = removeAction;

        initialize();
    }

    private void initialize() {
        setAlignment(Pos.CENTER_LEFT);
        setSpacing(20);
        setPadding(new Insets(18));
        getStyleClass().add("presentation-slide-card");

        StackPane slidePreview = createSlidePreview();
        VBox information = createInformation();
        Button removeButton = createRemoveButton();

        HBox.setHgrow(information, Priority.ALWAYS);

        getChildren().addAll(slidePreview, information, removeButton);
    }

    private StackPane createSlidePreview() {
        StackPane preview = new StackPane();
        preview.setPrefSize(420, 236);
        preview.setMinSize(420, 236);
        preview.setMaxSize(420, 236);
        preview.getStyleClass().add("slide-preview");

        VBox content = new VBox(14);
        content.setAlignment(Pos.CENTER);
        content.setPadding(new Insets(26));
        content.getStyleClass().add("slide-preview-content");

        Label slideTitle = new Label(item.title());
        slideTitle.setWrapText(true);
        slideTitle.getStyleClass().add("slide-preview-title");

        Label slideDescription = new Label(item.description());
        slideDescription.setWrapText(true);
        slideDescription.getStyleClass().add("slide-preview-description");

        HBox imagePlaceholders = new HBox(12);
        imagePlaceholders.setAlignment(Pos.CENTER);

        Region firstImage = new Region();
        firstImage.getStyleClass().add("slide-image-placeholder");

        Region secondImage = new Region();
        secondImage.getStyleClass().add("slide-image-placeholder");

        HBox.setHgrow(firstImage, Priority.ALWAYS);
        HBox.setHgrow(secondImage, Priority.ALWAYS);

        imagePlaceholders.getChildren().addAll(firstImage, secondImage);

        Label numberBadge = new Label(String.valueOf(item.slideNumber()));
        numberBadge.getStyleClass().add("slide-number-badge");
        StackPane.setAlignment(numberBadge, Pos.BOTTOM_RIGHT);
        StackPane.setMargin(numberBadge, new Insets(0, 10, 10, 0));

        content.getChildren().addAll(slideTitle, slideDescription, imagePlaceholders);
        preview.getChildren().addAll(content, numberBadge);

        return preview;
    }

    private VBox createInformation() {
        VBox information = new VBox(8);
        information.setAlignment(Pos.CENTER_LEFT);

        Label title = new Label("Slide " + item.slideNumber());
        title.getStyleClass().add("slide-card-title");

        Label description = new Label(item.description());
        description.setWrapText(true);
        description.getStyleClass().add("slide-card-description");

        Label status = new Label("Incluído na apresentação final");
        status.getStyleClass().add("slide-card-status");

        information.getChildren().addAll(title, description, status);

        return information;
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