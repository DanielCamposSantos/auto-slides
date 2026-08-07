package io.github.danielcampossantos.ui.common.popup;

import io.github.danielcampossantos.ui.common.window.AppWindow;
import io.github.danielcampossantos.ui.navigation.SceneManager;
import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.ParallelTransition;
import javafx.animation.ScaleTransition;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

public final class PopupService {

    private static final Duration OPEN_DURATION = Duration.millis(180);
    private static final Duration CLOSE_DURATION = Duration.millis(140);

    private static PopupService instance;

    private boolean showing;

    private PopupService() {
    }

    public static PopupService getInstance() {
        if (instance == null) {
            instance = new PopupService();
        }

        return instance;
    }

    public void information(String title, String message) {
        show(PopupType.INFORMATION, title, message, null);
    }

    public void success(String title, String message) {
        show(PopupType.SUCCESS, title, message, null);
    }

    public void warning(String title, String message) {
        show(PopupType.WARNING, title, message, null);
    }

    public void error(String title, String message) {
        show(PopupType.ERROR, title, message, null);
    }

    public void show(PopupType type, String title, String message, Runnable onClose) {
        if (!Platform.isFxApplicationThread()) {
            Platform.runLater(() -> show(type, title, message, onClose));
            return;
        }

        AppWindow window = SceneManager.getInstance().getWindow();

        if (window == null || showing) {
            return;
        }

        showing = true;
        window.beginOverlay();

        StackPane overlay = new StackPane();
        overlay.getStyleClass().add("popup-overlay");

        Region shield = new Region();
        shield.getStyleClass().add("popup-shield");

        VBox card = createCard(type, title, message);
        Button confirmButton = (Button) card.getProperties().get("confirmButton");

        confirmButton.setOnAction(event -> close(window, overlay, card, onClose));

        overlay.getChildren().addAll(shield, card);
        StackPane.setAlignment(card, Pos.CENTER);

        window.getOverlayHost().getChildren().setAll(overlay);

        playOpenAnimation(overlay, card);
    }

    private VBox createCard(PopupType type, String title, String message) {
        VBox card = new VBox(12);
        card.setAlignment(Pos.TOP_LEFT);
        card.setMaxHeight(Region.USE_PREF_SIZE);
        card.getStyleClass().addAll("popup-card", type.styleClass());

        Label icon = new Label(type.symbol());
        icon.getStyleClass().add("popup-icon");

        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("popup-title");
        titleLabel.setWrapText(true);
        titleLabel.setMaxWidth(420);

        Label messageLabel = new Label(message == null ? "" : message);
        messageLabel.getStyleClass().add("popup-message");
        messageLabel.setWrapText(true);
        messageLabel.setMaxWidth(420);

        VBox copy = new VBox(4, titleLabel, messageLabel);
        copy.setAlignment(Pos.TOP_LEFT);
        copy.setMinWidth(0);
        copy.setMaxWidth(Double.MAX_VALUE);

        HBox content = new HBox(14, icon, copy);
        content.setAlignment(Pos.TOP_LEFT);
        HBox.setHgrow(copy, Priority.ALWAYS);

        Button confirmButton = new Button("Entendido");
        confirmButton.getStyleClass().add("popup-confirm-button");
        confirmButton.setDefaultButton(true);

        HBox actions = new HBox(confirmButton);
        actions.setAlignment(Pos.CENTER_RIGHT);

        card.getChildren().addAll(content, actions);
        card.getProperties().put("confirmButton", confirmButton);

        return card;
    }

    private void playOpenAnimation(StackPane overlay, VBox card) {
        overlay.setOpacity(0);
        card.setScaleX(0.88);
        card.setScaleY(0.88);
        card.setOpacity(0);

        FadeTransition overlayFade = new FadeTransition(OPEN_DURATION, overlay);
        overlayFade.setFromValue(0);
        overlayFade.setToValue(1);

        FadeTransition cardFade = new FadeTransition(OPEN_DURATION, card);
        cardFade.setFromValue(0);
        cardFade.setToValue(1);

        ScaleTransition cardScale = new ScaleTransition(OPEN_DURATION, card);
        cardScale.setFromX(0.88);
        cardScale.setFromY(0.88);
        cardScale.setToX(1);
        cardScale.setToY(1);
        cardScale.setInterpolator(Interpolator.EASE_OUT);

        new ParallelTransition(overlayFade, cardFade, cardScale).play();
    }

    private void close(AppWindow window, StackPane overlay, VBox card, Runnable onClose) {
        FadeTransition overlayFade = new FadeTransition(CLOSE_DURATION, overlay);
        overlayFade.setFromValue(1);
        overlayFade.setToValue(0);

        ScaleTransition cardScale = new ScaleTransition(CLOSE_DURATION, card);
        cardScale.setFromX(1);
        cardScale.setFromY(1);
        cardScale.setToX(0.92);
        cardScale.setToY(0.92);
        cardScale.setInterpolator(Interpolator.EASE_IN);

        ParallelTransition transition = new ParallelTransition(overlayFade, cardScale);

        transition.setOnFinished(event -> {
            showing = false;
            window.endOverlay();

            if (onClose != null) {
                onClose.run();
            }
        });

        transition.play();
    }
}
