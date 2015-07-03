package spdxedit;

import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.Pane;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Window;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spdx.rdfparser.InvalidSPDXAnalysisException;
import org.spdx.rdfparser.model.SpdxFile;
import org.spdx.rdfparser.model.SpdxPackage;

import java.io.IOException;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class PackageEditor {


    private static final Logger logger = LoggerFactory.getLogger(PackageEditor.class);

    @FXML
    private TitledPane tabFiles;

    @FXML
    private TreeTableView<SpdxFile> filesTable;

    @FXML
    private TreeTableColumn<SpdxFile, String> tblColumnFile;

    @FXML
    private TitledPane tabRelationships;

    @FXML
    private Button btnOk;

    //The package being edited
    private SpdxPackage pkg;

    @FXML
    private void initialize() {
        assert tabFiles != null : "fx:id=\"tabFiles\" was not injected: check your FXML file 'PackageEditor.fxml'.";
        assert filesTable != null : "fx:id=\"filesTable\" was not injected: check your FXML file 'PackageEditor.fxml'.";
        assert tblColumnFile != null : "fx:id=\"tblColumnFile\" was not injected: check your FXML file 'PackageEditor.fxml'.";
        assert tabRelationships != null : "fx:id=\"tabRelationships\" was not injected: check your FXML file 'PackageEditor.fxml'.";
        assert btnOk != null : "fx:id=\"btnOk\" was not injected: check your FXML file 'PackageEditor.fxml'.";
        tblColumnFile.setCellValueFactory(
                (TreeTableColumn.CellDataFeatures<SpdxFile, String> param) ->
                        new ReadOnlyStringWrapper(param.getValue().getValue().getName()));
    }


    public static void editPackage(final SpdxPackage pkg, Window parentWindow) {
        final PackageEditor packageEditor = new PackageEditor(pkg);
        final Stage dialogStage = new Stage();
        dialogStage.setTitle("Edit SPDX Package");
        dialogStage.initStyle(StageStyle.UTILITY);
        dialogStage.initModality(Modality.APPLICATION_MODAL);
        dialogStage.setY(parentWindow.getX() + parentWindow.getWidth() / 2);
        dialogStage.setY(parentWindow.getY() + parentWindow.getHeight() / 2);
        try {
            FXMLLoader loader = new FXMLLoader(PackagePropsSceneController.class.getResource("/PackageEditor.fxml"));
            loader.setController(packageEditor);
            Pane pane = loader.load();
            Scene scene = new Scene(pane);
            dialogStage.setScene(scene);
            //Populate the files tree on appearance
            dialogStage.setOnShown(event ->
            {
                try {
                    final SpdxFile dummyfile = new SpdxFile(pkg.getName(), null, null, null, null, null, null, null, null, null, null, null, null);
                    TreeItem<SpdxFile> root = new TreeItem<>(dummyfile);
                    packageEditor.filesTable.setRoot(root);

                    root.getChildren().setAll(Stream.of(pkg.getFiles()).map(spdxFile -> new TreeItem<SpdxFile>(spdxFile)).collect(Collectors.toList()));
                } catch (InvalidSPDXAnalysisException e) {
                    logger.error("Unable to get files for package " + pkg.getName(), e);
                }
            });

            //Won't assign this event through FXML - don't want to propagate the stage beyond this point.
            packageEditor.btnOk.setOnMouseClicked(event2 -> dialogStage.close());
            dialogStage.showAndWait();


        } catch (IOException ioe) {
            throw new RuntimeException("Unable to load dialog", ioe);
        }
    }

    private PackageEditor(SpdxPackage pkg) {
        this.pkg = pkg;

    }


}
