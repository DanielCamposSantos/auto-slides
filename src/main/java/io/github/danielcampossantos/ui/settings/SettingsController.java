package io.github.danielcampossantos.ui.settings;

import io.github.danielcampossantos.domain.template.PresentationTemplateInfo;
import io.github.danielcampossantos.infrastructure.template.PresentationTemplateService;
import io.github.danielcampossantos.infrastructure.template.TemplatePreferencesService;
import io.github.danielcampossantos.ui.common.popup.PopupService;
import io.github.danielcampossantos.ui.navigation.Reloadable;
import io.github.danielcampossantos.ui.navigation.SceneManager;
import io.github.danielcampossantos.ui.navigation.SceneType;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.stage.FileChooser;
import lombok.extern.log4j.Log4j2;
import org.kordamp.ikonli.javafx.FontIcon;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;

@Log4j2
public final class SettingsController implements Reloadable {

    private final TemplatePreferencesService templatePreferencesService = TemplatePreferencesService.getInstance();

    private final PresentationTemplateService presentationTemplateService = new PresentationTemplateService();

    @FXML
    private StackPane rootPane;

    @FXML
    private FontIcon templateIcon;

    @FXML
    private Label templateNameLabel;

    @FXML
    private Label templatePathLabel;

    @FXML
    private Label slideCountLabel;

    @FXML
    private Label slideSizeLabel;

    @FXML
    private Button chooseTemplateButton;

    @FXML
    private Button removeTemplateButton;

    private Path selectedTemplate;

    @FXML
    private void initialize() {
        templateIcon.setIconLiteral("mdi2f-file-powerpoint");

        reload();
    }

    @Override
    public void reload() {
        Optional<Path> template = templatePreferencesService.getTemplate();

        if (template.isEmpty()) {
            selectedTemplate = null;

            showEmptyState();

            return;
        }

        selectedTemplate = template.get();

        loadTemplateInformation(selectedTemplate);
    }

    @FXML
    private void onChooseTemplate() {
        FileChooser chooser = new FileChooser();

        chooser.setTitle("Selecionar template do PowerPoint");

        chooser.getExtensionFilters().setAll(
                new FileChooser.ExtensionFilter(
                        "Apresentações do PowerPoint",
                        "*.pptx"
                )
        );

        configureInitialDirectory(chooser);

        File selectedFile = chooser.showOpenDialog(
                rootPane.getScene().getWindow()
        );

        if (selectedFile == null) {
            return;
        }

        Path templatePath = selectedFile.toPath();

        try {
            PresentationTemplateInfo templateInfo = presentationTemplateService.inspect(templatePath);

            selectedTemplate = templateInfo.path();

            templatePreferencesService.saveTemplate(selectedTemplate);

            showTemplateInformation(templateInfo);

            PopupService.getInstance().success(
                    "Template configurado",
                    "O template foi validado e será utilizado nas próximas apresentações."
            );

            log.info("Template configurado: {}", selectedTemplate);
        } catch (IOException exception) {
            log.error("Não foi possível configurar o template.", exception);

            PopupService.getInstance().error(
                    "Template inválido",
                    exception.getMessage() == null
                            ? "Não foi possível ler o arquivo selecionado."
                            : exception.getMessage()
            );
        }
    }

    @FXML
    private void onRemoveTemplate() {
        if (selectedTemplate == null) {
            return;
        }

        templatePreferencesService.clearTemplate();

        selectedTemplate = null;

        showEmptyState();

        PopupService.getInstance().information(
                "Template removido",
                "Selecione outro template antes de iniciar uma nova apresentação."
        );
    }

    @FXML
    private void onBack() {
        SceneManager.getInstance().show(SceneType.HOME);
    }

    private void loadTemplateInformation(Path templatePath) {
        try {
            PresentationTemplateInfo templateInfo = presentationTemplateService.inspect(templatePath);

            showTemplateInformation(templateInfo);
        } catch (IOException exception) {
            log.error("O template salvo não pôde ser carregado.", exception);

            templatePreferencesService.clearTemplate();

            selectedTemplate = null;

            showEmptyState();

            PopupService.getInstance().warning(
                    "Template indisponível",
                    "O template salvo foi removido, renomeado ou está inválido. Selecione outro arquivo."
            );
        }
    }

    private void showTemplateInformation(PresentationTemplateInfo templateInfo) {
        templateNameLabel.setText(templateInfo.fileName());
        templatePathLabel.setText(templateInfo.path().toString());

        slideCountLabel.setText(
                templateInfo.slideCount() == 1
                        ? "1 slide"
                        : templateInfo.slideCount() + " slides"
        );

        slideSizeLabel.setText(
                "%.0f × %.0f pontos".formatted(
                        templateInfo.slideWidth(),
                        templateInfo.slideHeight()
                )
        );

        chooseTemplateButton.setText("Trocar template");
        removeTemplateButton.setDisable(false);
    }

    private void showEmptyState() {
        templateNameLabel.setText("Nenhum template configurado");

        templatePathLabel.setText(
                "Selecione um arquivo .pptx para liberar o fluxo de criação."
        );

        slideCountLabel.setText("Nenhum slide");
        slideSizeLabel.setText("Dimensões indisponíveis");

        chooseTemplateButton.setText("Selecionar template");
        removeTemplateButton.setDisable(true);
    }

    private void configureInitialDirectory(FileChooser chooser) {
        if (selectedTemplate == null || selectedTemplate.getParent() == null) {
            return;
        }

        File directory = selectedTemplate.getParent().toFile();

        if (directory.isDirectory()) {
            chooser.setInitialDirectory(directory);
        }
    }
}