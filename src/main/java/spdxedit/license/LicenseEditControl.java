
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
    private ChoiceBox<SpdxListedLicense> chcListedLicense;

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

        chcListedLicense.setConverter(StringConverters.SPDX_LISTED_LICENSE_CONVERTER);
        chcListedLicense.getItems().addAll(SpdxLogic.getAllListedLicenses().collect(Collectors.toList()));

    }


    /**
     * Sets the value of the control. Do not call before the UI of the control is initialized.
     * @param value
     */
    public void setValue(AnyLicenseInfo value){
        if (value instanceof SpdxListedLicense){
            chcListedLicense.setValue((SpdxListedLicense)value);
            rdoStandard.selectedProperty().setValue(true);
        } else if (value instanceof ExtractedLicenseInfo ){
            chcExtractedLicenses.getItems().clear();
            chcExtractedLicenses.getItems().addAll(Arrays.stream(documentContainer.getExtractedLicenseInfos()).collect(Collectors.toList()));
            chcExtractedLicenses.setValue((ExtractedLicenseInfo)value);
        }
    }

    public AnyLicenseInfo getValue(){
        AnyLicenseInfo result = null;
        if (rdoNoAssert.isSelected()){
            result = new SpdxNoAssertionLicense();
        } else  if (rdoNone.isSelected()){
            result = new SpdxNoneLicense();
        } else if (rdoStandard.isSelected()){
            result = chcListedLicense.getValue();
        } else if (rdoExtracted.isSelected()){
            result = chcExtractedLicenses.getValue();
        }
        if (result == null){
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
