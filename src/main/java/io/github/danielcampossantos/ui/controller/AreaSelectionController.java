package io.github.danielcampossantos.ui.controller;

import io.github.danielcampossantos.model.PdfPage;
import io.github.danielcampossantos.model.SelectionArea;
import io.github.danielcampossantos.service.ApplicationService;
import io.github.danielcampossantos.service.Workspace;
import io.github.danielcampossantos.ui.navigation.SceneManager;
import io.github.danielcampossantos.ui.navigation.SceneType;
import io.github.danielcampossantos.ui.tree.NodeType;
import io.github.danielcampossantos.ui.tree.SelectionTreeNode;
import io.github.danielcampossantos.ui.view.PdfPageView;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.layout.VBox;
import lombok.extern.log4j.Log4j2;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Log4j2
public final class AreaSelectionController {

    @FXML
    private ScrollPane pagesScrollPane;

    @FXML
    private VBox pagesContainer;

    @FXML
    private TreeView<SelectionTreeNode> selectionTreeView;

    @FXML
    private Label selectionCounter;

    private Workspace workspace;

    private final TreeItem<SelectionTreeNode> root = new TreeItem<>();

    private final Map<Integer, TreeItem<SelectionTreeNode>> pdfNodes = new LinkedHashMap<>();

    private final Map<PdfPage, TreeItem<SelectionTreeNode>> pageNodes = new LinkedHashMap<>();

    private final Map<UUID, TreeItem<SelectionTreeNode>> selectionNodes = new LinkedHashMap<>();

    private int selectionCount;

    @FXML
    private void initialize() {

        workspace = ApplicationService.getInstance().getWorkspace();

        selectionTreeView.setRoot(root);

        loadPages();

        updateSelectionCounter();

    }

    private void loadPages() {

        pagesContainer.getChildren().clear();

        for (PdfPage page : workspace.getPages()) {

            PdfPageView pageView = new PdfPageView(page);

            pageView.setOnSelectionCreated(this::onSelectionCreated);

            pageView.setOnSelectionRemoved(this::onSelectionRemoved);

            pagesContainer.getChildren().add(pageView);

        }

    }

    private void onSelectionCreated(SelectionArea area) {

        PdfPage page = area.page();

        TreeItem<SelectionTreeNode> pdfNode = pdfNodes.computeIfAbsent(page.pdfNumber(), number -> {

            TreeItem<SelectionTreeNode> item = new TreeItem<>(
                    new SelectionTreeNode(
                            NodeType.PDF,
                            number,
                            "PDF " + number
                    )
            );

            root.getChildren().add(item);

            return item;

        });

        TreeItem<SelectionTreeNode> pageNode = pageNodes.computeIfAbsent(page, currentPage -> {

            TreeItem<SelectionTreeNode> item = new TreeItem<>(
                    new SelectionTreeNode(
                            NodeType.PAGE,
                            currentPage,
                            "Página " + currentPage.pageNumber()
                    )
            );

            pdfNode.getChildren().add(item);

            return item;

        });

        selectionCount++;

        TreeItem<SelectionTreeNode> selectionNode = new TreeItem<>(
                new SelectionTreeNode(
                        NodeType.SELECTION,
                        area,
                        "Seleção " + selectionCount
                )
        );

        selectionNodes.put(area.id(), selectionNode);

        pageNode.getChildren().add(selectionNode);

        pdfNode.setExpanded(true);
        pageNode.setExpanded(true);

        updateSelectionCounter();

    }

    private void onSelectionRemoved(SelectionArea area) {

        TreeItem<SelectionTreeNode> selectionNode = selectionNodes.remove(area.id());

        if (selectionNode == null) {
            return;
        }

        TreeItem<SelectionTreeNode> pageNode = selectionNode.getParent();

        if (pageNode != null) {

            pageNode.getChildren().remove(selectionNode);

            if (pageNode.getChildren().isEmpty()) {

                TreeItem<SelectionTreeNode> pdfNode = pageNode.getParent();

                pageNodes.remove(area.page());

                if (pdfNode != null) {

                    pdfNode.getChildren().remove(pageNode);

                    if (pdfNode.getChildren().isEmpty()) {

                        pdfNodes.remove(area.page().pdfNumber());

                        root.getChildren().remove(pdfNode);

                    }

                }

            }

        }

        selectionCount--;

        updateSelectionCounter();

    }

    private void updateSelectionCounter() {

        selectionCounter.setText(selectionCount == 1
                ? "1 área selecionada"
                : selectionCount + " áreas selecionadas");

    }

    @FXML
    private void onBack() {

        SceneManager.getInstance().show(SceneType.HOME);

    }

    @FXML
    private void onFinish() {

        ApplicationService.getInstance().clearWorkspace();

        Platform.exit();

    }

}