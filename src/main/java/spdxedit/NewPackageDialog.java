package spdxedit;

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
import org.spdx.rdfparser.license.ListedLicenses;
import org.spdx.rdfparser.model.SpdxPackage;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Optional;

public class NewPackageDialog {

    private static final Logger logger = LoggerFactory.getLogger(NewPackageDialog.class);

    @FXML
    private Button ok;

    @FXML
    private Button cancel;

    @FXML
    private TextField name;

    @FXML
    private ComboBox<String> licenseSelection;

    @FXML
    private CheckBox chkOmitHiddenFiles;

    @FXML
    private CheckBox chkRemotePackage;

    @FXML
    private TextArea downloadLocation;

    private Path path;

    private Window parentWindow;

    public static SpdxPackage createPackageWithPrompt(Window parentWindow, Optional<Path> path) {
        final NewPackageDialog controller = new NewPackageDialog(path);
        final Stage dialogStage = new Stage();
        dialogStage.setTitle("Create SPDX Package");
        dialogStage.initStyle(StageStyle.UTILITY);
        dialogStage.initModality(Modality.APPLICATION_MODAL);
        dialogStage.setY(parentWindow.getX() + parentWindow.getWidth() / 2);
        dialogStage.setY(parentWindow.getY() + parentWindow.getHeight() / 2);
        try {
            FXMLLoader loader = new FXMLLoader(NewPackageDialog.class.getResource("/NewPackageDialog.fxml"));
            loader.setController(controller);
            Pane pane = loader.load();
            Scene scene = new Scene(pane);
            dialogStage.setScene(scene);
            dialogStage.setOnShown(event -> {
                controller.licenseSelection.getItems().clear();
                Arrays.stream(ListedLicenses.getListedLicenses().getSpdxListedLicenseIds()).sorted().forEachOrdered(
                        id -> controller.licenseSelection.getItems().add(id)
                );
                if (path.isPresent()) {
                    controller.name.setText(path.get().getFileName().toString());
                }
                controller.licenseSelection.getSelectionModel().selectFirst();

            });
            //Won't assign this event through FXML - don't want to propagate the stage beyond this point.
            controller.ok.setOnMouseClicked(event -> dialogStage.close());
            if (!path.isPresent()){//No path provided. Must be remote.
                controller.chkRemotePackage.setSelected(true);
                controller.chkRemotePackage.setDisable(true);
                controller.chkOmitHiddenFiles.setSelected(false);
                controller.chkOmitHiddenFiles.setDisable(true);
            }

            dialogStage.showAndWait();
            return controller.createSpdxPackageFromInputs();

        } catch (IOException ioe) {
            throw new RuntimeException("Unable to load dialog", ioe);
        }
    }

    private NewPackageDialog(Optional<Path> path) {
        this.path = path.orElse(null);
    }

    @FXML
    void initialize() {
        assert ok != null : "fx:id=\"ok\" was not injected: check your FXML file 'NewPackageDialog.fxml'.";
        assert cancel != null : "fx:id=\"cancel\" was not injected: check your FXML file 'NewPackageDialog.fxml'.";
        assert name != null : "fx:id=\"name\" was not injected: check your FXML file 'NewPackageDialog.fxml'.";
        assert licenseSelection != null : "fx:id=\"licenseSelection\" was not injected: check your FXML file 'NewPackageDialog.fxml'.";
        assert downloadLocation != null : "fx:id=\"downloadLocation\" was not injected: check your FXML file 'NewPackageDialog.fxml'.";
        assert chkOmitHiddenFiles != null : "fx:id=\"chkOmitHiddenFiles\" was not injected: check your FXML file 'NewPackageDialog.fxml'.";
        assert chkRemotePackage != null : "fx:id=\"chkRemotePackage\" was not injected: check your FXML file 'NewPackageDialog.fxml'.";

    }


    private SpdxPackage createSpdxPackageFromInputs() {
        Optional<Path> pathForPackage = chkRemotePackage.isSelected() ? Optional.empty() : Optional.of(this.path);
        return SpdxLogic.createSpdxPackageForPath(pathForPackage, licenseSelection.getValue(), name.getText(), downloadLocation.getText(), chkOmitHiddenFiles.isSelected());
    }


}
