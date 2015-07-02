package spdxedit;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.input.MouseEvent;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Window;
import org.apache.commons.codec.digest.DigestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spdx.rdfparser.InvalidSPDXAnalysisException;
import org.spdx.rdfparser.SPDXDocumentFactory;
import org.spdx.rdfparser.SpdxDocumentContainer;
import org.spdx.rdfparser.model.Relationship;
import org.spdx.rdfparser.model.SpdxDocument;
import org.spdx.rdfparser.model.SpdxPackage;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class MainSceneController {

    @FXML
    private TreeView<Path> dirTree;

    @FXML
    private Button chooseDir;

    @FXML
    private Button addPackage;

    @FXML
    private Button saveSpdx;

    @FXML
    private ListView<String> addedPackagesUiList;

    private List<SpdxPackage> addedPackages = new LinkedList<>();

    private static final Logger logger = LoggerFactory.getLogger(MainSceneController.class);

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


    public void handleChooseDirectoryClicked(MouseEvent event) {
        Optional<Path> chosenPath = selectDirectory(chooseDir.getParent().getScene().getWindow());
        if (!chosenPath.isPresent()) return;
        try {
            dirTree.setRoot(getTreeForPath(chosenPath.get()));
        } catch (IOException ioe) {
            logger.error("Unable to access directory " + chosenPath.toString(), ioe);
        }
    }


    public void handleSaveSpdxClicked(MouseEvent event){
        SpdxDocument document = SpdxLogic.createDocumentWithPackages(this.addedPackages);
        FileChooser chooser = new FileChooser();
        FileChooser.ExtensionFilter spdxExtensionFilter = new FileChooser.ExtensionFilter("spdx", "*.spdx");
        chooser.getExtensionFilters().add(spdxExtensionFilter);
        chooser.setSelectedExtensionFilter(spdxExtensionFilter);

        File targetFile = chooser.showSaveDialog(saveSpdx.getScene().getWindow());
        try(FileWriter writer = new FileWriter(targetFile)){
            document.getDocumentContainer().getModel().write(writer);
        } catch (IOException e) {
            logger.error("Unable to write SPDX file", e);
        }
    }

    public void handleAddPackageClicked(MouseEvent event){
        List<TreeItem<Path>> selectedNodes = dirTree.getSelectionModel().getSelectedItems();
        assert(selectedNodes.size() <= 1);
        Path path = selectedNodes.get(0).getValue();
        SpdxPackage newPackage = PackagePropsSceneController.createPackageWithPrompt(addPackage.getScene().getWindow(), path);
        addedPackages.add(newPackage);
        addedPackagesUiList.getItems().add(newPackage.getName());

    }

    public void handleDirectoryTreeClicked(MouseEvent event){
        addPackage.setDisable(dirTree.getSelectionModel().isEmpty());
    }

}
