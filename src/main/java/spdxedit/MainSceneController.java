package spdxedit;

import com.google.common.base.Joiner;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.Window;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spdx.rdfparser.InvalidSPDXAnalysisException;
import org.spdx.rdfparser.SPDXDocumentFactory;
import org.spdx.rdfparser.SpdxDocumentContainer;
import org.spdx.rdfparser.model.SpdxDocument;
import org.spdx.rdfparser.model.SpdxPackage;
import org.spdx.tag.CommonCode;
import org.spdx.tools.TagToRDF;
import spdxedit.io.FileDataType;
import spdxedit.util.UiUtils;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

public class MainSceneController {

    @FXML
    private TreeView<Path> dirTree;

    @FXML
    private TextField txtDocumentName;

    @FXML
    private Button chooseDir;

    @FXML
    private Button btnAddPackage;

    @FXML
    private Button saveSpdx;

    @FXML
    private Button validateSpdx;

    @FXML
    private Button btnNewDocument;


    @FXML
    private ListView<SpdxPackage> addedPackagesUiList;

    private SpdxDocument documentToEdit = null;

    private static final Logger logger = LoggerFactory.getLogger(MainSceneController.class);


    //A representation of a package in the pacage list
    static class SpdxPackageListCell extends ListCell<SpdxPackage> {
        public SpdxPackageListCell() {
            super();
            //A necessary hack to work around list view not selecting rows on click.
            this.setOnMouseClicked(event -> getListView().getSelectionModel().select(this.getIndex()));
        }

        @Override
        protected void updateItem(SpdxPackage item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null) {
                setText(null);
                setGraphic(null);
            } else {
                //TODO: Add graphic
                setText(item.getName());
            }
        }
    }


    private FileChooser getSpdxFileChooser(Iterable<String> extenions) {
        FileChooser chooser = new FileChooser();

        boolean first = true;
        for (String extension : extenions) {
            FileChooser.ExtensionFilter spdxExtensionFilter = new FileChooser.ExtensionFilter(extension, "*." + extension);
            chooser.getExtensionFilters().add(spdxExtensionFilter);
            if (first){
                chooser.setSelectedExtensionFilter(spdxExtensionFilter);
                first = false;
            }

        }

        return chooser;
    }

    @FXML
    void initialize() {
        assert dirTree != null : "fx:id=\"dirTree\" was not injected: check your FXML file 'MainScene.fxml'.";
        assert chooseDir != null : "fx:id=\"chooseDir\" was not injected: check your FXML file 'MainScene.fxml'.";
        assert btnAddPackage != null : "fx:id=\"btnAddPackage\" was not injected: check your FXML file 'MainScene.fxml'.";
        assert btnNewDocument != null : "fx:id=\"btnNewDocument\" was not injected: check your FXML file 'MainScene.fxml'.";
        assert saveSpdx != null : "fx:id=\"saveSpdx\" was not injected: check your FXML file 'MainScene.fxml'.";
        assert validateSpdx != null : "fx:id=\"validateSpdx\" was not injected: check your FXML file 'MainScene.fxml'.";
        assert txtDocumentName != null : "fx:id=\"txtDocumentName\" was not injected: check your FXML file 'MainScene.fxml'.";
        assert addedPackagesUiList != null : "fx:id=\"addedPackagesUiList\" was not injected: check your FXML file 'MainScene.fxml'.";

        txtDocumentName.textProperty().addListener((observable, oldValue, newValue) -> handleTxtDocumentNameChanged(newValue));
        addedPackagesUiList.setCellFactory(param -> new SpdxPackageListCell());

    }

    private void enableAllButtons(){
        this.btnAddPackage.setDisable(false);
        this.validateSpdx.setDisable(false);
        this.chooseDir.setDisable(false);
        this.txtDocumentName.setDisable(false);
        this.saveSpdx.setDisable(false);
    }

    private static Optional<Path> selectDirectory(Window parentWindow) {
        Objects.requireNonNull(parentWindow);
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle(Main.APP_TITLE);
        File result = chooser.showDialog(parentWindow);
        if (result == null) {
            return Optional.empty();
        } else {
            return Optional.of(Paths.get(result.getAbsolutePath()));
        }
    }

    private static TreeItem<Path> getTreeForPath(final Path base) throws IOException {
        final HashMap<Path, TreeItem<Path>> addedNodes = new HashMap<>();
        TreeItem<Path> rootNode = new TreeItem<>(base);
        rootNode.setExpanded(true);
        addedNodes.put(base, rootNode);
        Files.walk(base).forEachOrdered(path -> {
            if (Objects.equals(path, base)) return;
            TreeItem<Path> current = new TreeItem<>(path);
            TreeItem<Path> parent = addedNodes.get(path.getParent());
            parent.getChildren().add(current);
            addedNodes.put(path, current);
        });
        return rootNode;

    }

    public void handleNewDocumentClicked(MouseEvent event){
        TextInputDialog dialog = new TextInputDialog("http://url.example.com/spdx/builder");
        ((Stage)dialog.getDialogPane().getScene().getWindow()).getIcons().addAll(UiUtils.ICON_IMAGE_VIEW.getImage());
        dialog.setTitle("New SPDX Document");
        dialog.setHeaderText("Enter document namespace");

        Optional<String> result = dialog.showAndWait();
        while (result.isPresent() && !SpdxLogic.validateDocumentNamespace(result.orElse(""))){

            dialog.setHeaderText("Invalid document namespace. Enter new document namespace.");
            result = dialog.showAndWait();
        }
        if (result.isPresent()){
            SpdxDocument newDocument = SpdxLogic.createEmptyDocument(result.get());
            loadSpdxDocument(newDocument);
        }

    }

    public void handleChooseDirectoryClicked(MouseEvent event) {
        Optional<Path> chosenPath = selectDirectory(chooseDir.getParent().getScene().getWindow());
        if (!chosenPath.isPresent()) return;
        try {
            dirTree.setRoot(getTreeForPath(chosenPath.get()));
        } catch (IOException ioe) {
            logger.error("Unable to access directory " + chosenPath.toString(), ioe);
        }
    }



    public void handleSaveSpdxClicked(MouseEvent event) {
        Optional<FileDataType> outputType = IoFileTypeSelectionDialog.getDataType("Save SPDX");
        if (!outputType.isPresent()) return;

        File targetFile = getSpdxFileChooser( outputType.get().getExtensions()).showSaveDialog(saveSpdx.getScene().getWindow());
        if (targetFile == null) //Dialog cancelled
            return;
        try {
            outputType.get().writeToFile(targetFile, documentToEdit);
        } catch (IOException e) {
            logger.error("Unable to write SPDX file", e);
        }
    }

    public void handleAddPackageClicked(MouseEvent event) {
        List<TreeItem<Path>> selectedNodes = dirTree.getSelectionModel().getSelectedItems();
        assert (selectedNodes.size() <= 1);
        Optional<Path> path = selectedNodes.size() > 0 ? Optional.of(selectedNodes.get(0).getValue()) : Optional.empty();
        SpdxPackage newPackage = NewPackageDialog.createPackageWithPrompt(btnAddPackage.getScene().getWindow(), path, documentToEdit.getDocumentContainer());
        addedPackagesUiList.getItems().add(newPackage);
        if (addedPackagesUiList.getSelectionModel().getSelectedItem() == null) {
            addedPackagesUiList.getSelectionModel().selectFirst();
        }
    }


    private void loadSpdxDocument(SpdxDocument loadedDocument) {
        this.documentToEdit = loadedDocument;
        this.addedPackagesUiList.getItems().setAll(SpdxLogic.getSpdxPackagesInDocument(loadedDocument).collect(Collectors.toList()));
        this.txtDocumentName.setText(loadedDocument.getName());
        enableAllButtons();
    }

    public void handleLoadSpdxClicked(MouseEvent event) {
        Optional<FileDataType> inputType = IoFileTypeSelectionDialog.getDataType("Load SPDX");
        if (!inputType.isPresent())return;

        File targetFile = getSpdxFileChooser(inputType.get().getExtensions()).showOpenDialog(saveSpdx.getScene().getWindow());
        if (targetFile == null) return; //Cancelled
        try {
            SpdxDocument loadedDocument = inputType.get().readFromFile(targetFile);
            loadSpdxDocument(loadedDocument);
        } catch (InvalidSPDXAnalysisException isae) {
            logger.error("Invalid SPDX load attempt", isae);
            new Alert(Alert.AlertType.ERROR, "Invalid SPDX file " + targetFile.getAbsolutePath());
        } catch (FileNotFoundException fnfe) {
            logger.error("File not found: " + targetFile.getAbsolutePath());
            new Alert(Alert.AlertType.ERROR, "File " + targetFile.getAbsolutePath() + " not found").showAndWait();
        } catch (IOException ioe) {
            logger.error("Error loading SPDX", ioe);
            new Alert(Alert.AlertType.ERROR, "Error loading SPDX file " + targetFile.getAbsolutePath());
        }
    }



    public void handlePackageListClicked(MouseEvent event) {
        if (event.getClickCount() == 2) {
            SpdxPackage toEdit = addedPackagesUiList.getSelectionModel().getSelectedItem();
            List<SpdxPackage> otherPackages = SpdxLogic.getSpdxPackagesInDocument(documentToEdit)
                    .filter(spdxPackage -> !Objects.equals(spdxPackage, toEdit))
                    .collect(Collectors.toList());
            PackageEditor.editPackage(toEdit, otherPackages, documentToEdit.getDocumentContainer(), addedPackagesUiList.getScene().getWindow());
        }
    }

    public void handleValidateSpdxClicked(MouseEvent event) {
        List<String> verificationResult = this.documentToEdit.verify();
        Alert alert = new Alert(verificationResult.size() == 0 ? Alert.AlertType.INFORMATION : Alert.AlertType.WARNING);
        alert.setTitle(Main.APP_TITLE);
        if (verificationResult.size() == 0) {
            alert.setHeaderText("Document valid.");
            alert.setContentText("This SPDX document is valid.");
        } else {
            alert.setHeaderText("Document invalid.");

            TextArea textArea = new TextArea(StringUtils.join(verificationResult, ",\n"));
            textArea.setEditable(false);
            textArea.setWrapText(true);
            textArea.setPrefWidth(808);
            textArea.setMaxWidth(Double.MAX_VALUE);
            textArea.setMaxHeight(Double.MAX_VALUE);
            GridPane.setVgrow(textArea, Priority.ALWAYS);
            GridPane.setHgrow(textArea, Priority.ALWAYS);

            GridPane validationCongtent = new GridPane();
            validationCongtent.setMaxWidth(Double.MAX_VALUE);
            validationCongtent.add(new Label("Problems:"), 0, 0);
            validationCongtent.add(textArea, 0, 1);

            // Set expandable Exception into the dialog pane.
            alert.getDialogPane().setContent(validationCongtent);
        }
        ((Stage)alert.getDialogPane().getScene().getWindow()).getIcons().addAll(UiUtils.ICON_IMAGE_VIEW.getImage());
        alert.showAndWait();
    }


    public void handleDirectoryTreeClicked(MouseEvent event) {
        btnAddPackage.setDisable(dirTree.getSelectionModel().isEmpty());
    }


    @FXML
    void handleTxtDocumentNameChanged(String newName) {
        this.documentToEdit.setName(newName);
    }


}
