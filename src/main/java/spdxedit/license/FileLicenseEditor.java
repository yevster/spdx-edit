package spdxedit.license;


import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import org.spdx.rdfparser.InvalidSPDXAnalysisException;
import org.spdx.rdfparser.SpdxDocumentContainer;
import org.spdx.rdfparser.license.AnyLicenseInfo;
import org.spdx.rdfparser.model.SpdxFile;

public class FileLicenseEditor {

    public static void editConcludedLicense(SpdxFile file, SpdxDocumentContainer container) {
        LicenseEditControl control = new LicenseEditControl(container, true);
        if (file.getLicenseConcluded() != null) {
            control.setInitialValue(file.getLicenseConcluded());
        }

        DialogPane dialogPane = new DialogPane();
        dialogPane.getButtonTypes().add(ButtonType.OK);
        dialogPane.setContent(control.getUi());

        Dialog<AnyLicenseInfo> dialog = new Dialog<>();
        dialog.setDialogPane(dialogPane);
        dialog.setResultConverter(buttonType -> {
            if (buttonType == ButtonType.OK) {
                return control.getValue();
            } else {
                throw new RuntimeException("Unexepected button!");
            }
        });

        dialog.showAndWait();

        try {
            file.setLicenseConcluded(control.getValue());
        } catch (InvalidSPDXAnalysisException e) {
            throw new RuntimeException(e);

        }

    }

}
