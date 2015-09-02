
package spdxedit.license;

import java.io.IOException;
import java.net.URL;
import java.util.*;
import java.util.stream.Collectors;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.util.StringConverter;
import org.spdx.rdfparser.InvalidSPDXAnalysisException;
import org.spdx.rdfparser.SpdxDocumentContainer;
import org.spdx.rdfparser.license.*;
import org.spdx.rdfparser.model.SpdxDocument;
import org.spdx.rdfparser.model.SpdxFile;
import spdxedit.SpdxLogic;
import spdxedit.util.StringConverters;


public class LicenseEditControl {

    private SpdxDocumentContainer documentContainer;

    private AnyLicenseInfo initialValue = new SpdxNoAssertionLicense();

    @FXML
    private ResourceBundle resources;

    @FXML
    private URL location;

    @FXML
    private RadioButton rdoNone;

    @FXML
    private RadioButton rdoNoAssert;

    @FXML
    private RadioButton rdoStandard;

    @FXML
    private ChoiceBox<String> chcListedLicense;

    @FXML
    private ChoiceBox<ExtractedLicenseInfo> chcExtractedLicenses;

    @FXML
    private RadioButton rdoExtracted;

    @FXML
    private Button btnNewFromFile;

    @FXML
    void handleBtnNewFromFileClick(MouseEvent event) {

    }

    LicenseEditControl(SpdxDocumentContainer documentContainer) {
        this.documentContainer = documentContainer;
    }

    @FXML
    void initialize() {
        assert rdoNone != null : "fx:id=\"rdoNone\" was not injected: check your FXML file 'LicenseEditControl.fxml'.";
        assert rdoNoAssert != null : "fx:id=\"rdoNoAssert\" was not injected: check your FXML file 'LicenseEditControl.fxml'.";
        assert rdoStandard != null : "fx:id=\"rdoStandard\" was not injected: check your FXML file 'LicenseEditControl.fxml'.";
        assert chcListedLicense != null : "fx:id=\"chcListedLicense\" was not injected: check your FXML file 'LicenseEditControl.fxml'.";
        assert chcExtractedLicenses != null : "fx:id=\"chcExtractedLicenses\" was not injected: check your FXML file 'LicenseEditControl.fxml'.";
        assert rdoExtracted != null : "fx:id=\"rdoExtracted\" was not injected: check your FXML file 'LicenseEditControl.fxml'.";
        assert btnNewFromFile != null : "fx:id=\"btnNewFromFile\" was not injected: check your FXML file 'LicenseEditControl.fxml'.";

        //Make radio buttons mutually exclusive

        ToggleGroup licenseTypeGroup = new ToggleGroup();
        rdoExtracted.setToggleGroup(licenseTypeGroup);
        rdoStandard.setToggleGroup(licenseTypeGroup);
        rdoNone.setToggleGroup(licenseTypeGroup);
        rdoNoAssert.setToggleGroup(licenseTypeGroup);

        //Choice boxes should disable when their respective radio buttons are untoggled.
        rdoStandard.selectedProperty().addListener((observable, oldValue, newValue) -> chcListedLicense.setDisable(!newValue));
        rdoExtracted.selectedProperty().addListener((observable, oldValue, newValue) -> chcExtractedLicenses.setDisable(!newValue));

        chcListedLicense.getItems().addAll(Arrays.stream(ListedLicenses.getListedLicenses().getSpdxListedLicenseIds()).sorted().collect(Collectors.toList()));

        //Apply the initial value
        if (this.initialValue instanceof SpdxListedLicense) {
            chcListedLicense.setValue(((SpdxListedLicense) initialValue).getLicenseId());
            rdoStandard.selectedProperty().setValue(true);
        } else if (initialValue instanceof ExtractedLicenseInfo) {
            chcExtractedLicenses.getItems().clear();
            chcExtractedLicenses.getItems().addAll(Arrays.stream(documentContainer.getExtractedLicenseInfos()).collect(Collectors.toList()));
            chcExtractedLicenses.setValue((ExtractedLicenseInfo) initialValue);
            rdoExtracted.selectedProperty().setValue(true);
        } else if (initialValue instanceof SpdxNoAssertionLicense) {
            rdoNoAssert.selectedProperty().setValue(true);
        } else if (initialValue instanceof SpdxNoneLicense) {
            rdoNone.selectedProperty().setValue(true);
        } else {
            new Alert(Alert.AlertType.ERROR, "Unsupported license type: " + initialValue.getClass().getSimpleName() + ".", ButtonType.OK);
        }

    }


    /**
     * Sets the initial value of the control. Has no impact if called after the UI is initialized.
     * Must not be null.
     *
     * @param value
     */
    public void setInitialValue(AnyLicenseInfo value) {
        this.initialValue = Objects.requireNonNull(value);
    }

    public AnyLicenseInfo getValue() {
        AnyLicenseInfo result = null;
        if (rdoNoAssert.isSelected()) {
            result = new SpdxNoAssertionLicense();
        } else if (rdoNone.isSelected()) {
            result = new SpdxNoneLicense();
        } else if (rdoStandard.isSelected()) {
            try {
                result = ListedLicenses.getListedLicenses().getListedLicenseById(chcListedLicense.getValue());
            } catch (InvalidSPDXAnalysisException e) {
                throw new RuntimeException(e);
            }
        } else if (rdoExtracted.isSelected()) {
            result = chcExtractedLicenses.getValue();
        }
        if (result == null) {
            new Alert(Alert.AlertType.ERROR, "Unable to extract selected license", ButtonType.OK);
        }
        return result;
    }

    public Pane getUi() {
        try {
            FXMLLoader loader = new FXMLLoader(LicenseEditControl.class.getResource("/LicenseEdit.fxml"));
            loader.setController(this);
            Pane pane = loader.load();
            return pane;
        } catch (IOException ioe) {
            throw new RuntimeException("Unable to load scene for License editor dialog");
        }
    }

}
