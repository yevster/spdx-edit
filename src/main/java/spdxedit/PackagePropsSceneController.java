package spdxedit;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.SplitMenuButton;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Window;
import org.spdx.rdfparser.InvalidSPDXAnalysisException;
import org.spdx.rdfparser.SpdxPackageVerificationCode;
import org.spdx.rdfparser.license.AnyLicenseInfo;
import org.spdx.rdfparser.license.ListedLicenses;
import org.spdx.rdfparser.model.SpdxFile;
import org.spdx.rdfparser.model.SpdxPackage;

import java.io.IOException;
import java.nio.file.Path;

public class PackagePropsSceneController {

    @FXML
    private Button ok;

    @FXML
    private Button cancel;

    @FXML
    private TextField name;

    @FXML
    private SplitMenuButton licenseSelection;

    @FXML
    private TextArea comment;

    private Path path;

    private Window parentWindow;

    public static final SpdxPackage createPackageWithPrompt(Window parentWindow, Path path) {
        PackagePropsSceneController controller = new PackagePropsSceneController(path);
        final Stage dialogStage = new Stage();
        dialogStage.setTitle("Create SPDX Package");
        dialogStage.initStyle(StageStyle.UTILITY);
        dialogStage.initModality(Modality.APPLICATION_MODAL);
        dialogStage.setY(parentWindow.getX() + parentWindow.getWidth() / 2);
        dialogStage.setY(parentWindow.getY() + parentWindow.getHeight() / 2);
        try {
            FXMLLoader loader = new FXMLLoader(PackagePropsSceneController.class.getResource("/PackagePropsScene.fxml"));
            loader.setController(controller);
            Scene scene = new Scene(loader.load());
            dialogStage.setScene(scene);
            dialogStage.showAndWait();
            return controller.createSpdxPackageFromInputs();

        } catch (IOException ioe) {
            throw new RuntimeException("Unable to load dialog", ioe);
        }
    }

    private PackagePropsSceneController(Path path) {
        this.path = path;

    }


    private SpdxPackage createSpdxPackageFromInputs() {
        try {
            AnyLicenseInfo license = ListedLicenses.getListedLicenses().getListedLicenseById(licenseSelection.getText());

            SpdxPackage pkg = new SpdxPackage(name.getText(), license,
                    new AnyLicenseInfo[]{} /* Licences from files*/,
                    null /*Declared licenses*/,
                    null /*Download location*/,
                    null /*Download Location*/,
                    new SpdxFile[]{} /*Files*/,
                    new SpdxPackageVerificationCode(name.getText(), new String[]{}));
            return pkg;


        } catch (InvalidSPDXAnalysisException e) {
            throw new RuntimeException("SPDX error", e);
        }
    }


}
