package io.github.danielcampossantos.ui.settings;

import io.github.danielcampossantos.domain.template.PresentationTemplateInfo;
import io.github.danielcampossantos.domain.template.TemplateLayout;
import io.github.danielcampossantos.infrastructure.template.PresentationTemplateService;
import io.github.danielcampossantos.infrastructure.template.TemplateAnalysisService;
import io.github.danielcampossantos.infrastructure.template.TemplateLayoutStorageService;
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

    private final TemplateAnalysisService templateAnalysisService = new TemplateAnalysisService();

    private final TemplateLayoutStorageService templateLayoutStorageService = new TemplateLayoutStorageService();

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
        FileChooser chooser = createTemplateFileChooser();

        configureInitialDirectory(chooser);

        File selectedFile = chooser.showOpenDialog(
                rootPane.getScene().getWindow()
        );

        if (selectedFile == null) {
            return;
        }

        configureTemplate(selectedFile.toPath());
    }

    private FileChooser createTemplateFileChooser() {
        FileChooser chooser = new FileChooser();

        chooser.setTitle(
                "Selecionar template do PowerPoint"
        );

        chooser.getExtensionFilters().setAll(
                new FileChooser.ExtensionFilter(
                        "Apresentações do PowerPoint",
                        "*.pptx"
                )
        );

        return chooser;
    }

    private void configureTemplate(Path templatePath) {
        try {
            PresentationTemplateInfo templateInfo = presentationTemplateService.inspect(
                    templatePath
            );

            TemplateLayout templateLayout = templateAnalysisService.analyze(
                    templateInfo.path()
            );

            validateTemplateSlots(templateLayout);

            Path layoutPath = templateLayoutStorageService.save(
                    templateLayout
            );

            templatePreferencesService.saveTemplate(
                    templateInfo.path(),
                    layoutPath,
                    templateLayout.templateId()
            );

            selectedTemplate = templateInfo.path();

            showTemplateInformation(
                    templateInfo,
                    templateLayout
            );

            PopupService.getInstance().success(
                    "Template configurado",
                    createSuccessMessage(
                            templateLayout,
                            layoutPath
                    )
            );

            log.info(
                    "Template configurado: {}",
                    selectedTemplate
            );

            log.info(
                    "Layout do template salvo em: {}",
                    layoutPath
            );
        } catch (IOException | IllegalArgumentException exception) {
            log.error(
                    "Não foi possível configurar o template.",
                    exception
            );

            PopupService.getInstance().error(
                    "Template inválido",
                    exception.getMessage() == null
                            ? "Não foi possível analisar o arquivo selecionado."
                            : exception.getMessage()
            );
        }
    }

    private void validateTemplateSlots(
            TemplateLayout templateLayout
    ) throws IOException {
        long slotCount = templateLayout.slides()
                .stream()
                .mapToLong(slide ->
                        slide.slots().size()
                )
                .sum();

        if (slotCount == 0) {
            throw new IOException(
                    """
                            O template não possui nenhum espaço configurado.
                            
                            No PowerPoint, renomeie as formas destinadas às imagens usando:
                            
                            AUTO_SLOT::identificador::Nome exibido
                            """
            );
        }
    }

    private String createSuccessMessage(
            TemplateLayout templateLayout,
            Path layoutPath
    ) {
        int slideCount = templateLayout.slides().size();

        long slotCount = templateLayout.slides()
                .stream()
                .mapToLong(slide ->
                        slide.slots().size()
                )
                .sum();

        String slidesText = slideCount == 1
                ? "1 slide analisado"
                : slideCount + " slides analisados";

        String slotsText = slotCount == 1
                ? "1 espaço de imagem encontrado"
                : slotCount + " espaços de imagem encontrados";

        return """
                O template foi analisado e configurado corretamente.
                
                %s
                %s
                
                Configuração:
                %s
                """.formatted(
                slidesText,
                slotsText,
                layoutPath
        );
    }

    @FXML
    private void onRemoveTemplate() {
        if (selectedTemplate == null) {
            return;
        }

        Optional<String> templateId = templatePreferencesService.getTemplateId();

        try {
            if (templateId.isPresent()) {
                templateLayoutStorageService.delete(
                        templateId.get()
                );
            }
        } catch (IOException exception) {
            log.error(
                    "Não foi possível excluir a configuração persistida do template.",
                    exception
            );
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
        SceneManager.getInstance().show(
                SceneType.HOME
        );
    }

    private void loadTemplateInformation(
            Path templatePath
    ) {
        try {
            PresentationTemplateInfo templateInfo = presentationTemplateService.inspect(
                    templatePath
            );

            Path layoutPath = templatePreferencesService.getLayoutPath()
                    .orElseThrow(() -> new IOException(
                            "A configuração do template não foi encontrada."
                    ));

            TemplateLayout templateLayout = templateLayoutStorageService.read(
                    layoutPath
            );

            showTemplateInformation(
                    templateInfo,
                    templateLayout
            );
        } catch (IOException exception) {
            log.error(
                    "O template salvo não pôde ser carregado.",
                    exception
            );

            templatePreferencesService.clearTemplate();

            selectedTemplate = null;

            showEmptyState();

            PopupService.getInstance().warning(
                    "Template indisponível",
                    """
                            O template ou sua configuração foram removidos, renomeados ou estão inválidos.
                            
                            Selecione novamente o arquivo PowerPoint.
                            """
            );
        }
    }

    private void showTemplateInformation(
            PresentationTemplateInfo templateInfo,
            TemplateLayout templateLayout
    ) {
        templateNameLabel.setText(
                templateInfo.fileName()
        );

        templatePathLabel.setText(
                templateInfo.path().toString()
        );

        long slotCount = templateLayout.slides()
                .stream()
                .mapToLong(slide ->
                        slide.slots().size()
                )
                .sum();

        slideCountLabel.setText(
                createSlideInformationText(
                        templateInfo.slideCount(),
                        slotCount
                )
        );

        slideSizeLabel.setText(
                "%.0f × %.0f pontos".formatted(
                        templateInfo.slideWidth(),
                        templateInfo.slideHeight()
                )
        );

        chooseTemplateButton.setText(
                "Trocar template"
        );

        removeTemplateButton.setDisable(
                false
        );
    }

    private String createSlideInformationText(
            int slideCount,
            long slotCount
    ) {
        String slidesText = slideCount == 1
                ? "1 slide"
                : slideCount + " slides";

        String slotsText = slotCount == 1
                ? "1 espaço"
                : slotCount + " espaços";

        return slidesText + " • " + slotsText;
    }

    private void showEmptyState() {
        templateNameLabel.setText(
                "Nenhum template configurado"
        );

        templatePathLabel.setText(
                "Selecione um arquivo .pptx para liberar o fluxo de criação."
        );

        slideCountLabel.setText(
                "Nenhum slide"
        );

        slideSizeLabel.setText(
                "Dimensões indisponíveis"
        );

        chooseTemplateButton.setText(
                "Selecionar template"
        );

        removeTemplateButton.setDisable(
                true
        );
    }

    private void configureInitialDirectory(
            FileChooser chooser
    ) {
        if (selectedTemplate == null
                || selectedTemplate.getParent() == null) {
            return;
        }

        File directory = selectedTemplate.getParent()
                .toFile();

        if (directory.isDirectory()) {
            chooser.setInitialDirectory(
                    directory
            );
        }
    }
}