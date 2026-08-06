package io.github.danielcampossantos.ui.selection.popup;

import io.github.danielcampossantos.domain.selection.SelectionDestination;
import io.github.danielcampossantos.domain.template.TemplateSlide;
import io.github.danielcampossantos.domain.template.TemplateSlot;
import io.github.danielcampossantos.infrastructure.template.TemplateLayoutService;
import io.github.danielcampossantos.ui.common.popup.PopupService;
import io.github.danielcampossantos.ui.common.window.AppWindow;
import io.github.danielcampossantos.ui.navigation.SceneManager;
import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.ParallelTransition;
import javafx.animation.ScaleTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import lombok.extern.log4j.Log4j2;

import java.io.IOException;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;

@Log4j2
public final class DestinationSelectionPopup {

    private static final Duration OPEN_DURATION = Duration.millis(180);
    private static final Duration CLOSE_DURATION = Duration.millis(140);

    private final TemplateLayoutService templateLayoutService;

    public DestinationSelectionPopup(TemplateLayoutService templateLayoutService) {
        this.templateLayoutService = templateLayoutService;
    }

    public void show(
            Predicate<SelectionDestination> availabilityValidator,
            Consumer<SelectionDestination> onConfirm,
            Runnable onCancel
    ) {
        try {
            List<TemplateSlide> slides = templateLayoutService.getSlides();

            showPopup(slides, availabilityValidator, onConfirm, onCancel);
        } catch (IOException exception) {
            log.error("Não foi possível carregar a configuração do template.", exception);

            PopupService.getInstance().error(
                    "Configuração indisponível",
                    "Não foi possível carregar os slides e espaços do template."
            );

            onCancel.run();
        }
    }

    private void showPopup(
            List<TemplateSlide> slides,
            Predicate<SelectionDestination> availabilityValidator,
            Consumer<SelectionDestination> onConfirm,
            Runnable onCancel
    ) {
        AppWindow window = SceneManager.getInstance().getWindow();

        window.beginOverlay();

        StackPane overlay = new StackPane();
        overlay.getStyleClass().add("popup-overlay");

        Region shield = new Region();
        shield.getStyleClass().add("popup-shield");

        VBox card = new VBox(16);
        card.setAlignment(Pos.CENTER_LEFT);
        card.setPadding(new Insets(26));
        card.getStyleClass().add("destination-popup-card");

        Label title = new Label("Destino da seleção");
        title.getStyleClass().add("popup-title");

        Label description = new Label(
                "Escolha em qual slide e espaço esta imagem deverá ser inserida."
        );
        description.setWrapText(true);
        description.getStyleClass().add("popup-message");

        Label slideLabel = new Label("Slide");
        slideLabel.getStyleClass().add("destination-field-label");

        ComboBox<TemplateSlide> slideComboBox = new ComboBox<>();
        slideComboBox.getItems().setAll(slides);
        slideComboBox.setMaxWidth(Double.MAX_VALUE);
        slideComboBox.getStyleClass().add("destination-combo-box");

        Label slotLabel = new Label("Espaço");
        slotLabel.getStyleClass().add("destination-field-label");

        ComboBox<TemplateSlot> slotComboBox = new ComboBox<>();
        slotComboBox.setMaxWidth(Double.MAX_VALUE);
        slotComboBox.setDisable(true);
        slotComboBox.getStyleClass().add("destination-combo-box");

        Label validationLabel = new Label();
        validationLabel.setWrapText(true);
        validationLabel.setVisible(false);
        validationLabel.setManaged(false);
        validationLabel.getStyleClass().add("destination-validation-label");

        Button cancelButton = new Button("Cancelar");
        cancelButton.getStyleClass().add("destination-cancel-button");

        Button confirmButton = new Button("Confirmar destino");
        confirmButton.setDisable(true);
        confirmButton.getStyleClass().add("popup-confirm-button");

        HBox actions = new HBox(10, cancelButton, confirmButton);
        actions.setAlignment(Pos.CENTER_RIGHT);

        slideComboBox.valueProperty().addListener((observable, oldSlide, selectedSlide) -> {
            slotComboBox.getItems().clear();
            slotComboBox.setValue(null);

            boolean hasSlide = selectedSlide != null;

            slotComboBox.setDisable(!hasSlide);

            if (hasSlide) {
                slotComboBox.getItems().setAll(selectedSlide.slots());
            }

            confirmButton.setDisable(true);
            hideValidation(validationLabel);
        });

        slotComboBox.valueProperty().addListener((observable, oldSlot, selectedSlot) -> {
            TemplateSlide selectedSlide = slideComboBox.getValue();

            if (selectedSlide == null || selectedSlot == null) {
                confirmButton.setDisable(true);
                return;
            }

            SelectionDestination destination = templateLayoutService.createDestination(
                    selectedSlide,
                    selectedSlot
            );

            boolean available = availabilityValidator.test(destination);

            confirmButton.setDisable(!available);

            if (available) {
                hideValidation(validationLabel);
            } else {
                showValidation(
                        validationLabel,
                        "Esse espaço já atingiu o limite de imagens permitido."
                );
            }
        });

        cancelButton.setOnAction(event -> close(
                window,
                overlay,
                card,
                onCancel
        ));

        confirmButton.setOnAction(event -> {
            TemplateSlide slide = slideComboBox.getValue();
            TemplateSlot slot = slotComboBox.getValue();

            if (slide == null || slot == null) {
                return;
            }

            SelectionDestination destination = templateLayoutService.createDestination(
                    slide,
                    slot
            );

            close(
                    window,
                    overlay,
                    card,
                    () -> onConfirm.accept(destination)
            );
        });

        card.getChildren().addAll(
                title,
                description,
                slideLabel,
                slideComboBox,
                slotLabel,
                slotComboBox,
                validationLabel,
                actions
        );

        overlay.getChildren().addAll(shield, card);

        StackPane.setAlignment(card, Pos.CENTER);

        window.getOverlayHost().getChildren().setAll(overlay);

        playOpenAnimation(overlay, card);
    }

    private void showValidation(Label label, String message) {
        label.setText(message);
        label.setVisible(true);
        label.setManaged(true);
    }

    private void hideValidation(Label label) {
        label.setVisible(false);
        label.setManaged(false);
    }

    private void playOpenAnimation(StackPane overlay, VBox card) {
        overlay.setOpacity(0);
        card.setOpacity(0);
        card.setScaleX(0.9);
        card.setScaleY(0.9);

        FadeTransition overlayFade = new FadeTransition(OPEN_DURATION, overlay);
        overlayFade.setFromValue(0);
        overlayFade.setToValue(1);

        FadeTransition cardFade = new FadeTransition(OPEN_DURATION, card);
        cardFade.setFromValue(0);
        cardFade.setToValue(1);

        ScaleTransition scale = new ScaleTransition(OPEN_DURATION, card);
        scale.setFromX(0.9);
        scale.setFromY(0.9);
        scale.setToX(1);
        scale.setToY(1);
        scale.setInterpolator(Interpolator.EASE_OUT);

        new ParallelTransition(overlayFade, cardFade, scale).play();
    }

    private void close(
            AppWindow window,
            StackPane overlay,
            VBox card,
            Runnable afterClose
    ) {
        FadeTransition fade = new FadeTransition(CLOSE_DURATION, overlay);
        fade.setFromValue(1);
        fade.setToValue(0);

        ScaleTransition scale = new ScaleTransition(CLOSE_DURATION, card);
        scale.setFromX(1);
        scale.setFromY(1);
        scale.setToX(0.94);
        scale.setToY(0.94);
        scale.setInterpolator(Interpolator.EASE_IN);

        ParallelTransition transition = new ParallelTransition(fade, scale);

        transition.setOnFinished(event -> {
            window.endOverlay();
            afterClose.run();
        });

        transition.play();
    }
}