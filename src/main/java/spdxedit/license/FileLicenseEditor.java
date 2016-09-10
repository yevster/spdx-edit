package spdxedit.license;


import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.apache.commons.lang3.StringUtils;
import org.spdx.rdfparser.InvalidSPDXAnalysisException;
import org.spdx.rdfparser.SpdxDocumentContainer;
import org.spdx.rdfparser.license.AnyLicenseInfo;
import org.spdx.rdfparser.license.ExtractedLicenseInfo;
import org.spdx.rdfparser.model.SpdxFile;
import spdxedit.Main;
import spdxedit.SpdxLogic;
import spdxedit.UiUtils;

import java.util.Arrays;
import java.util.Optional;

public class FileLicenseEditor {

    public static void editConcludedLicense(SpdxFile file, SpdxDocumentContainer container) {
        LicenseEditControl control = new LicenseEditControl(container, file, true);
        if (file.getLicenseConcluded() != null) {
            control.setInitialValue(file.getLicenseConcluded());
        }

        Dialog<Boolean> dialog = UiUtils.newDialog("Edit License", ButtonType.OK);
        dialog.getDialogPane().setContent(control.getUi());

        dialog.setResultConverter(buttonType -> {
            if (buttonType == ButtonType.OK) {
                return true;
            } else {
                return false;
            }
        });

        boolean acceptChange = dialog.showAndWait().orElse(false);

        if (acceptChange) {
            try {
                file.setLicenseConcluded(control.getValue());
            } catch (InvalidSPDXAnalysisException e) {
                throw new RuntimeException(e);

            }
        }

    }

    public static void extractLicenseFromFile(SpdxFile file, SpdxDocumentContainer container) {


        Dialog dialog = new Dialog();
        dialog.setTitle(Main.APP_TITLE);
        dialog.setHeaderText("Extract license");

        ((Stage) dialog.getDialogPane().getScene().getWindow()).getIcons().addAll(UiUtils.ICON_IMAGE_VIEW.getImage());

        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.CANCEL, ButtonType.OK);

        String licenseName = "";
        String licenseText = null;
        String licenseId = "";
        //TODO: Add support for multiple extracted licenses.
        if (file.getLicenseInfoFromFiles() != null && file.getLicenseInfoFromFiles().length > 0) {
            Optional<AnyLicenseInfo> foundExtractedLicense = Arrays.stream(file.getLicenseInfoFromFiles()).filter(license -> license instanceof ExtractedLicenseInfo).findFirst();
            if (foundExtractedLicense.isPresent()) {
                licenseName = ((ExtractedLicenseInfo) foundExtractedLicense.get()).getName();
                licenseText = ((ExtractedLicenseInfo) foundExtractedLicense.get()).getExtractedText();
                licenseId = ((ExtractedLicenseInfo) foundExtractedLicense.get()).getLicenseId();

            }
        }

        LicenseExtractControl licenseExtractControl = new LicenseExtractControl(licenseName, licenseText, licenseId);

        dialog.getDialogPane().setContent(licenseExtractControl.getUi());
        Optional<ButtonType> result = dialog.showAndWait();

        licenseName = licenseExtractControl.getLicenseName();
        licenseText = licenseExtractControl.getLicenseText();
        licenseId = licenseExtractControl.getLicenseId();

        //No selection
        if (!result.isPresent() || result.get() == ButtonType.CANCEL) {
            return;
        }
        //Omitted data
        if (StringUtils.isBlank(licenseName)) {
            new Alert(Alert.AlertType.ERROR, "License name cannot be blank. Use \"NOASSERTION\" instead", ButtonType.OK).showAndWait();
            return;
        }
        if (StringUtils.isBlank(licenseText)) {
            new Alert(Alert.AlertType.ERROR, "License text cannot be blank.", ButtonType.OK).showAndWait();
            return;
        }
        if (StringUtils.isBlank(licenseId)) {
            new Alert(Alert.AlertType.ERROR, "License ID cannot be blank.", ButtonType.OK).showAndWait();
            return;
        }
        //License already extracted
        if (SpdxLogic.findExtractedLicenseByNameAndText(container, licenseName, licenseText).isPresent()) {
            new Alert(Alert.AlertType.WARNING, "License " + licenseName + " with the same text has already been extracted.", ButtonType.OK).showAndWait();
            return;
        }
        //License with ID already exists
        if (SpdxLogic.findExtractedLicenseInfoById(container, licenseId).isPresent()) {
            new Alert(Alert.AlertType.WARNING, "License with ID " + licenseId + " already exists.", ButtonType.OK).showAndWait();
            return;
        }


        SpdxLogic.addExtractedLicenseFromFile(file, container, licenseId, licenseName, licenseText);


    }

}
