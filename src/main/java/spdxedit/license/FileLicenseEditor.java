package spdxedit.license;


import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.image.ImageView;
import org.apache.commons.lang3.StringUtils;
import org.spdx.rdfparser.InvalidSPDXAnalysisException;
import org.spdx.rdfparser.SpdxDocumentContainer;
import org.spdx.rdfparser.license.AnyLicenseInfo;
import org.spdx.rdfparser.license.ExtractedLicenseInfo;
import org.spdx.rdfparser.model.SpdxFile;
import spdxedit.Main;
import spdxedit.SpdxLogic;

import java.util.Arrays;
import java.util.Optional;

public class FileLicenseEditor {

    public static void editConcludedLicense(SpdxFile file, SpdxDocumentContainer container) {
        LicenseEditControl control = new LicenseEditControl(container, file, true);
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

    public static void extractLicenseFromFile(SpdxFile file, SpdxDocumentContainer container){


        Dialog dialog = new Dialog();
        dialog.setTitle(Main.APP_TITLE);
        dialog.setHeaderText("Extract license");
        dialog.setGraphic(new ImageView(FileLicenseEditor.class.getResource("/img/document-8x.png").toString()));
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.CANCEL, ButtonType.OK);

        String licenseName = "NOASSERTION";
        String licenseText = null;
        //TODO: Add support for multiple extracted licenses.
        if (file.getLicenseInfoFromFiles()!= null && file.getLicenseInfoFromFiles().length > 0){
            Optional<AnyLicenseInfo> foundExtractedLicense = Arrays.stream(file.getLicenseInfoFromFiles()).filter(license -> license instanceof ExtractedLicenseInfo).findFirst();
            if (foundExtractedLicense.isPresent()){
                licenseName = ((ExtractedLicenseInfo)foundExtractedLicense.get()).getName();
                licenseText = ((ExtractedLicenseInfo)foundExtractedLicense.get()).getExtractedText();
            }
        }

        LicenseExtractControl licenseExtractControl = new LicenseExtractControl(licenseName, licenseText);

        dialog.getDialogPane().setContent(licenseExtractControl.getUi());
        Optional<ButtonType> result = dialog.showAndWait();

        licenseName = licenseExtractControl.getLicenseName();
        licenseText = licenseExtractControl.getLicenseText();

        //No selection
        if (!result.isPresent() || result.get() == ButtonType.CANCEL) {
            return;
        }
        //Omitted data
        if (StringUtils.isBlank(licenseName)){
            new Alert(Alert.AlertType.ERROR, "License name cannot be blank. Use \"NOASSERTION\" instead", ButtonType.OK).showAndWait();
            return;
        }
        if (StringUtils.isBlank(licenseText)){
            new Alert(Alert.AlertType.ERROR, "License text cannot be blank.", ButtonType.OK).showAndWait();
            return;
        }
        //License already extracted
        if (SpdxLogic.findExtractedLicenseByNameAndText(container, licenseName, licenseText).isPresent()){
            new Alert(Alert.AlertType.WARNING, "License "+licenseName+" with the same text has already been extracted.", ButtonType.OK).showAndWait();
            return;
        }


        SpdxLogic.addExtractedLicenseFromFile(file, container, licenseName, licenseText);


    }

}
