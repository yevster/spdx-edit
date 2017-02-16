package spdxedit;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.Pane;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spdx.rdfparser.SpdxDocumentContainer;
import org.spdx.rdfparser.model.SpdxPackage;
import spdxedit.license.LicenseEditControl;
import spdxedit.util.UiUtils;

import java.io.IOException;
import java.nio.file.Path;
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
    private CheckBox chkOmitHiddenFiles;

    @FXML
    private CheckBox chkRemotePackage;

    @FXML
    private TextArea downloadLocation;

    @FXML
    private TitledPane paneDeclaredLicense;


    private Path path;

    private SpdxDocumentContainer documentContainer;

    private LicenseEditControl declaredLicenseEdit;

    public static SpdxPackage createPackageWithPrompt(Window parentWindow, Optional<Path> path, SpdxDocumentContainer documentContainer) {
        final NewPackageDialog controller = new NewPackageDialog(path, documentContainer);
        final Stage dialogStage = new Stage();
        dialogStage.setTitle("Create SPDX Package");
        dialogStage.initModality(Modality.APPLICATION_MODAL);
        dialogStage.setY(parentWindow.getX() + parentWindow.getWidth() / 2);
        dialogStage.setY(parentWindow.getY() + parentWindow.getHeight() / 2);
        try {
            FXMLLoader loader = new FXMLLoader(NewPackageDialog.class.getResource("/NewPackageDialog.fxml"));
            loader.setController(controller);
            Pane pane = loader.load();
            Scene scene = new Scene(pane);
            dialogStage.getIcons().clear();
            dialogStage.getIcons().add(UiUtils.ICON_IMAGE_VIEW.getImage());
            dialogStage.setScene(scene);
            dialogStage.setOnShown(event -> {
                if (path.isPresent()) {
                    controller.name.setText(path.get().getFileName().toString());
                }
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

    private NewPackageDialog(Optional<Path> path, SpdxDocumentContainer documentContainer) {
        this.path = path.orElse(null);
        this.documentContainer = documentContainer;
    }

    @FXML
    void initialize() {
        assert ok != null : "fx:id=\"ok\" was not injected: check your FXML file 'NewPackageDialog.fxml'.";
        assert cancel != null : "fx:id=\"cancel\" was not injected: check your FXML file 'NewPackageDialog.fxml'.";
        assert name != null : "fx:id=\"name\" was not injected: check your FXML file 'NewPackageDialog.fxml'.";
        assert downloadLocation != null : "fx:id=\"downloadLocation\" was not injected: check your FXML file 'NewPackageDialog.fxml'.";
        assert chkOmitHiddenFiles != null : "fx:id=\"chkOmitHiddenFiles\" was not injected: check your FXML file 'NewPackageDialog.fxml'.";
        assert chkRemotePackage != null : "fx:id=\"chkRemotePackage\" was not injected: check your FXML file 'NewPackageDialog.fxml'.";
        assert paneDeclaredLicense != null : "fx:id=\"paneDeclaredLicense\" was not injected: check your FXML file 'NewPackageDialog.fxml'.";

        declaredLicenseEdit = new LicenseEditControl(this.documentContainer, null, false);
        paneDeclaredLicense.setContent(declaredLicenseEdit.getUi());


    }


    private SpdxPackage createSpdxPackageFromInputs() {
        Optional<Path> pathForPackage = chkRemotePackage.isSelected() ? Optional.empty() : Optional.of(this.path);
        return SpdxLogic.createSpdxPackageForPath(pathForPackage, this.documentContainer.getSpdxDocument(), declaredLicenseEdit.getValue(), name.getText(), downloadLocation.getText(), chkOmitHiddenFiles.isSelected());
    }


}
