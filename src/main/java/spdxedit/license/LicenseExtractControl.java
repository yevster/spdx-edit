/*
SPDX-License-Identifier: Apache-2.0
 */
package spdxedit.license;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.Pane;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.util.Objects;

/**
 * Embeddable control to edit extracted licenses
 */
public class LicenseExtractControl {


    @FXML
    private TextField txtLicenseId;

    @FXML
    private TextField txtLicenseName;

    @FXML
    private TextArea txtLicenseText;

    private String initialNameValue;
    private String initialTextValue;
    private String initialLicenseId;


    /**
     * Creates a new LicenseExtractControl.
     *
     * @param initialNameValue
     * @param initialTextValue
     * @param initialLicenseId
     */
    public LicenseExtractControl(String initialNameValue, String initialTextValue, String initialLicenseId) {
        this.initialNameValue = initialNameValue;
        this.initialTextValue = initialTextValue;
        this.initialLicenseId = initialLicenseId;
    }


    @FXML
    void initialize() {
        assert txtLicenseName != null : "fx:id=\"txtLicenseName\" was not injected: check your FXML file 'extractLicenseDialog.fxml'.";
        assert txtLicenseText != null : "fx:id=\"txtLicenseText\" was not injected: check your FXML file 'extractLicenseDialog.fxml'.";
        assert initialLicenseId != null : "fx:id=\"initialLicenseId\" was not injected: check your FXML file 'extractLicenseDialog.fxml'.";
        txtLicenseName.setText(initialNameValue);
        txtLicenseText.setText(initialTextValue);
        txtLicenseId.setText(StringUtils.removeStart(initialLicenseId, "LicenseRef-"));

    }

    public String getLicenseId() {
        return "LicenseRef-" + Objects.requireNonNull(txtLicenseId).getText();
    }

    /**
     * Returns the selected license name. Throws {@link NullPointerException} if called before the control has been initialized.
     */
    public String getLicenseName() {
        return Objects.requireNonNull(txtLicenseName).getText();
    }

    /**
     * Returns the selected license text. Throws {@link NullPointerException} if called before the control has been initialized.
     */
    public String getLicenseText() {
        return Objects.requireNonNull(txtLicenseText).getText();
    }

    public Pane getUi() {
        try {
            FXMLLoader loader = new FXMLLoader(LicenseExtractControl.class.getResource("/ExtractLicenseDialog.fxml"));
            loader.setController(this);
            Pane pane = loader.load();
            return pane;
        } catch (IOException ioe) {
            throw new RuntimeException("Unable to load scene for License editor dialog");
        }
    }
}
