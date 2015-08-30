
package spdxedit;

import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.RadioButton;
import org.spdx.rdfparser.license.ListedLicenses;


public class LicenseEdit {

    @FXML
    private ResourceBundle resources;

    @FXML
    private URL location;

    @FXML
    private RadioButton rdoNone;

    @FXML
    private RadioButton rdoNoAssert;

    @FXML
    private RadioButton rdoExpression;

    @FXML
    private ComboBox<String> cboExpression;

    @FXML
    void initialize() {
        assert rdoNone != null : "fx:id=\"rdoNone\" was not injected: check your FXML file 'LicenseEdit.fxml'.";
        assert rdoNoAssert != null : "fx:id=\"rdoNoAssert\" was not injected: check your FXML file 'LicenseEdit.fxml'.";
        assert rdoExpression != null : "fx:id=\"rdoExpression\" was not injected: check your FXML file 'LicenseEdit.fxml'.";
        assert cboExpression != null : "fx:id=\"cboExpression\" was not injected: check your FXML file 'LicenseEdit.fxml'.";

        String[] listedLicenseIds = ListedLicenses.getListedLicenses().getSpdxListedLicenseIds();
        for (String id : listedLicenseIds){
            cboExpression.getItems().add(id);
        }



    }
}
