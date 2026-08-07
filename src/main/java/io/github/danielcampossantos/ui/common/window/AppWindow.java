package io.github.danielcampossantos.ui.common.window;

import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import lombok.Getter;

public final class AppWindow extends StackPane {

    private final Stage stage;

    private final BorderPane frame;

    private final StackPane contentHost;

    @Getter
    private final StackPane overlayHost;

    private final Label titleLabel;

    private final Label maximizeIcon;

    private double dragOffsetX;

    private double dragOffsetY;

    public AppWindow(Stage stage) {
        this.stage = stage;
        frame = new BorderPane();
        contentHost = new StackPane();
        overlayHost = new StackPane();
        titleLabel = new Label("Auto Slides");
        maximizeIcon = new Label("□");

        initialize();
    }

    private void initialize() {
        getStyleClass().add("app-window");

        frame.getStyleClass().add("window-frame");
        frame.setTop(createTitleBar());
        frame.setCenter(contentHost);

        contentHost.getStyleClass().add("window-content-host");

        overlayHost.getStyleClass().add("window-overlay-host");
        overlayHost.setPickOnBounds(false);
        overlayHost.setMouseTransparent(true);

        getChildren().addAll(frame, overlayHost);

        stage.maximizedProperty().addListener((observable, oldValue, maximized) -> updateMaximizedState(maximized));

        updateMaximizedState(stage.isMaximized());
    }

    private HBox createTitleBar() {
        HBox titleBar = new HBox();
        titleBar.setAlignment(Pos.CENTER_LEFT);
        titleBar.getStyleClass().add("custom-title-bar");

        ImageView applicationIcon = new ImageView(AppIconFactory.create(64));
        applicationIcon.setFitWidth(22);
        applicationIcon.setFitHeight(22);
        applicationIcon.setPreserveRatio(true);
        applicationIcon.setSmooth(true);
        applicationIcon.setMouseTransparent(true);
        applicationIcon.getStyleClass().add("application-icon");

        titleLabel.getStyleClass().add("window-title");
        titleLabel.setMouseTransparent(true);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button minimizeButton = createWindowButton("—", "minimize-button");
        minimizeButton.setOnAction(event -> stage.setIconified(true));

        Button maximizeButton = new Button();
        maximizeButton.setGraphic(maximizeIcon);
        maximizeButton.getStyleClass().addAll("window-button", "maximize-button");
        maximizeButton.setFocusTraversable(false);
        maximizeButton.setOnAction(event -> toggleMaximized());

        Button closeButton = createWindowButton("×", "close-window-button");
        closeButton.setOnAction(event -> stage.close());

        titleBar.getChildren().addAll(applicationIcon, titleLabel, spacer, minimizeButton, maximizeButton, closeButton);

        titleBar.setOnMousePressed(this::handleTitleBarPressed);
        titleBar.setOnMouseDragged(this::handleTitleBarDragged);
        titleBar.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2 && event.getButton() == javafx.scene.input.MouseButton.PRIMARY) {
                toggleMaximized();
            }
        });

        return titleBar;
    }

    private Button createWindowButton(String text, String styleClass) {
        Button button = new Button(text);

        button.getStyleClass().addAll("window-button", styleClass);
        button.setFocusTraversable(false);

        return button;
    }

    private void handleTitleBarPressed(MouseEvent event) {
        if (!event.isPrimaryButtonDown()) {
            return;
        }

        dragOffsetX = event.getSceneX();
        dragOffsetY = event.getSceneY();
    }

    private void handleTitleBarDragged(MouseEvent event) {
        if (!event.isPrimaryButtonDown()) {
            return;
        }

        if (stage.isMaximized()) {
            double relativePosition = event.getScreenX() / stage.getWidth();

            stage.setMaximized(false);

            dragOffsetX = stage.getWidth() * relativePosition;
            dragOffsetY = 18;
        }

        stage.setX(event.getScreenX() - dragOffsetX);
        stage.setY(event.getScreenY() - dragOffsetY);
    }

    private void toggleMaximized() {
        stage.setMaximized(!stage.isMaximized());
    }

    private void updateMaximizedState(boolean maximized) {
        maximizeIcon.setText(maximized ? "❐" : "□");

        getStyleClass().remove("maximized");

        if (maximized) {
            getStyleClass().add("maximized");
        }
    }

    public void setContent(Parent content) {
        contentHost.getChildren().setAll(content);
    }

    public void setTitle(String title) {
        titleLabel.setText(title);
    }

    public void beginOverlay() {
        overlayHost.setPickOnBounds(true);
        overlayHost.setMouseTransparent(false);
        overlayHost.setCursor(Cursor.DEFAULT);
    }

    public void endOverlay() {
        overlayHost.getChildren().clear();
        overlayHost.setPickOnBounds(false);
        overlayHost.setMouseTransparent(true);
    }
}