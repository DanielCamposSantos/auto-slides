package io.github.danielcampossantos.ui.selection.popup;

import io.github.danielcampossantos.domain.selection.SelectionAssignment;
import io.github.danielcampossantos.domain.selection.SelectionDestination;
import io.github.danielcampossantos.domain.template.TemplateSlide;
import io.github.danielcampossantos.domain.template.TemplateSlot;
import io.github.danielcampossantos.infrastructure.template.TemplateLayoutService;
import io.github.danielcampossantos.ui.common.popup.PopupService;
import io.github.danielcampossantos.ui.navigation.SceneManager;
import io.github.danielcampossantos.ui.common.window.AppWindow;
import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.ParallelTransition;
import javafx.animation.ScaleTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import lombok.extern.log4j.Log4j2;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

@Log4j2
public final class DestinationSelectionPopup {

    private static final Duration OPEN_DURATION = Duration.millis(180);
    private static final Duration CLOSE_DURATION = Duration.millis(140);

    private final TemplateLayoutService templateLayoutService;

    public DestinationSelectionPopup(
            TemplateLayoutService templateLayoutService
    ) {
        this.templateLayoutService = templateLayoutService;
    }

    public void show(
            List<SelectionAssignment> assignments,
            Consumer<SelectionDestination> onConfirm,
            Runnable onCancel
    ) {
        try {
            List<DestinationOption> options = createOptions(assignments);

            if (options.isEmpty()) {
                PopupService.getInstance().warning(
                        "Nenhum destino disponível",
                        "O template selecionado não possui áreas configuradas para imagens."
                );

                onCancel.run();

                return;
            }

            showPopup(
                    options,
                    onConfirm,
                    onCancel
            );
        } catch (IOException exception) {
            log.error(
                    "Não foi possível carregar os destinos do template.",
                    exception
            );

            PopupService.getInstance().error(
                    "Erro ao carregar template",
                    exception.getMessage() == null
                            ? "Não foi possível carregar as áreas configuradas."
                            : exception.getMessage()
            );

            onCancel.run();
        }
    }

    private List<DestinationOption> createOptions(
            List<SelectionAssignment> assignments
    ) throws IOException {
        List<DestinationOption> options = new ArrayList<>();

        for (TemplateSlide slide : templateLayoutService.getSlides()) {
            for (TemplateSlot slot : slide.slots()) {
                int usageCount = countUsages(
                        slide,
                        slot,
                        assignments
                );

                options.add(
                        new DestinationOption(
                                slide,
                                slot,
                                usageCount
                        )
                );
            }
        }

        return List.copyOf(options);
    }

    private int countUsages(
            TemplateSlide slide,
            TemplateSlot slot,
            List<SelectionAssignment> assignments
    ) {
        return Math.toIntExact(
                assignments.stream()
                        .map(SelectionAssignment::destination)
                        .filter(destination ->
                                destination.slideId().equals(slide.slideId())
                        )
                        .filter(destination ->
                                destination.slotId().equals(slot.slotId())
                        )
                        .count()
        );
    }

    private void showPopup(
            List<DestinationOption> options,
            Consumer<SelectionDestination> onConfirm,
            Runnable onCancel
    ) {
        AppWindow window = SceneManager.getInstance().getWindow();

        window.beginOverlay();

        StackPane overlay = new StackPane();
        overlay.getStyleClass().add("popup-overlay");

        Region shield = new Region();
        shield.getStyleClass().add("popup-shield");

        VBox card = new VBox(12);
        card.setAlignment(Pos.CENTER_LEFT);
        card.setPadding(new Insets(20));
        card.setMaxHeight(Region.USE_PREF_SIZE);
        card.getStyleClass().add("destination-popup-card");

        Label title = new Label("Onde inserir esta imagem?");
        title.getStyleClass().add("popup-title");

        Label description = new Label(
                "Selecione o espaço da apresentação onde este recorte deverá ser inserido."
        );

        description.setWrapText(true);
        description.getStyleClass().add("popup-message");

        ListView<DestinationOption> destinationList = new ListView<>();

        destinationList.getItems().setAll(options);
        destinationList.setCellFactory(listView -> new DestinationOptionCell());
        destinationList.getStyleClass().add("destination-list");
        destinationList.setPrefHeight(
                Math.clamp(options.size() * 52.0 + 4, 180,
                        420)
        );

        Button cancelButton = new Button("Cancelar");
        cancelButton.getStyleClass().add("destination-cancel-button");

        Button confirmButton = new Button("Selecionar");
        confirmButton.getStyleClass().add("popup-confirm-button");
        confirmButton.setDisable(true);

        destinationList.getSelectionModel()
                .selectedItemProperty()
                .addListener((observable, oldValue, newValue) ->
                        confirmButton.setDisable(newValue == null)
                );

        HBox actions = new HBox(
                10,
                cancelButton,
                confirmButton
        );

        actions.setAlignment(Pos.CENTER_RIGHT);

        cancelButton.setOnAction(event ->
                close(
                        window,
                        overlay,
                        card,
                        onCancel
                )
        );

        confirmButton.setOnAction(event -> {
            DestinationOption option = destinationList
                    .getSelectionModel()
                    .getSelectedItem();

            if (option == null) {
                return;
            }

            confirmDestination(
                    option,
                    window,
                    overlay,
                    card,
                    onConfirm
            );
        });

        destinationList.setOnMouseClicked(event -> {
            if (event.getButton() != MouseButton.PRIMARY
                    || event.getClickCount() != 2) {
                return;
            }

            DestinationOption option = destinationList
                    .getSelectionModel()
                    .getSelectedItem();

            if (option == null) {
                return;
            }

            confirmDestination(
                    option,
                    window,
                    overlay,
                    card,
                    onConfirm
            );
        });

        card.getChildren().addAll(
                title,
                description,
                destinationList,
                actions
        );

        overlay.getChildren().addAll(
                shield,
                card
        );

        StackPane.setAlignment(
                card,
                Pos.CENTER
        );

        window.getOverlayHost()
                .getChildren()
                .setAll(overlay);

        playOpenAnimation(
                overlay,
                card
        );
    }

    private void confirmDestination(
            DestinationOption option,
            AppWindow window,
            StackPane overlay,
            VBox card,
            Consumer<SelectionDestination> onConfirm
    ) {
        SelectionDestination destination = templateLayoutService.createDestination(
                option.slide(),
                option.slot()
        );

        close(
                window,
                overlay,
                card,
                () -> onConfirm.accept(destination)
        );
    }

    private void playOpenAnimation(
            StackPane overlay,
            VBox card
    ) {
        overlay.setOpacity(0);
        card.setOpacity(0);
        card.setScaleX(0.92);
        card.setScaleY(0.92);

        FadeTransition overlayFade = new FadeTransition(
                OPEN_DURATION,
                overlay
        );

        overlayFade.setFromValue(0);
        overlayFade.setToValue(1);

        FadeTransition cardFade = new FadeTransition(
                OPEN_DURATION,
                card
        );

        cardFade.setFromValue(0);
        cardFade.setToValue(1);

        ScaleTransition scaleTransition = new ScaleTransition(
                OPEN_DURATION,
                card
        );

        scaleTransition.setFromX(0.92);
        scaleTransition.setFromY(0.92);
        scaleTransition.setToX(1);
        scaleTransition.setToY(1);
        scaleTransition.setInterpolator(
                Interpolator.EASE_OUT
        );

        new ParallelTransition(
                overlayFade,
                cardFade,
                scaleTransition
        ).play();
    }

    private void close(
            AppWindow window,
            StackPane overlay,
            VBox card,
            Runnable afterClose
    ) {
        FadeTransition fadeTransition = new FadeTransition(
                CLOSE_DURATION,
                overlay
        );

        fadeTransition.setFromValue(1);
        fadeTransition.setToValue(0);

        ScaleTransition scaleTransition = new ScaleTransition(
                CLOSE_DURATION,
                card
        );

        scaleTransition.setFromX(1);
        scaleTransition.setFromY(1);
        scaleTransition.setToX(0.96);
        scaleTransition.setToY(0.96);
        scaleTransition.setInterpolator(
                Interpolator.EASE_IN
        );

        ParallelTransition transition = new ParallelTransition(
                fadeTransition,
                scaleTransition
        );

        transition.setOnFinished(event -> {
            window.endOverlay();
            afterClose.run();
        });

        transition.play();
    }

    private record DestinationOption(
            TemplateSlide slide,
            TemplateSlot slot,
            int usageCount
    ) {
    }

    private static final class DestinationOptionCell
            extends ListCell<DestinationOption> {

        private final HBox container = new HBox();
        private final Label nameLabel = new Label();
        private final Label usageLabel = new Label();
        private final Region spacer = new Region();

        private DestinationOptionCell() {
            initialize();
        }

        private void initialize() {
            container.setAlignment(Pos.CENTER_LEFT);
            container.setSpacing(12);

            HBox.setHgrow(
                    spacer,
                    Priority.ALWAYS
            );

            nameLabel.getStyleClass().add(
                    "destination-option-name"
            );

            usageLabel.getStyleClass().add(
                    "destination-option-usage"
            );

            container.getChildren().addAll(
                    nameLabel,
                    spacer,
                    usageLabel
            );

            setCursor(Cursor.HAND);
        }

        @Override
        protected void updateItem(
                DestinationOption option,
                boolean empty
        ) {
            super.updateItem(
                    option,
                    empty
            );

            getStyleClass().remove(
                    "destination-option-used"
            );

            if (empty || option == null) {
                setGraphic(null);
                return;
            }

            nameLabel.setText(
                    option.slot().label()
            );

            if (option.usageCount() == 0) {
                usageLabel.setText("");
            } else {
                usageLabel.setText(
                        option.usageCount() == 1
                                ? "✓ 1 imagem"
                                : "✓ " + option.usageCount() + " imagens"
                );

                getStyleClass().add(
                        "destination-option-used"
                );
            }

            setGraphic(container);
        }
    }
}
