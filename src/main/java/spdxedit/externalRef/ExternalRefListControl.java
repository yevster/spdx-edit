package spdxedit.externalRef;

import com.google.common.collect.Lists;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.text.WordUtils;
import org.spdx.rdfparser.InvalidSPDXAnalysisException;
import org.spdx.rdfparser.model.ExternalRef;
import org.spdx.rdfparser.model.SpdxPackage;
import org.spdx.rdfparser.referencetype.ListedReferenceTypes;
import spdxedit.SpdxLogic;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;


/**
 * Created by yevster on 9/10/2016.
 */
public class ExternalRefListControl {

    private static final List<String> categories = Arrays.stream(ExternalRef.ReferenceCategory.values()).map(ExternalRef.ReferenceCategory::getTag)
            .map(WordUtils::capitalize).collect(Collectors.toList());

    private static class ExternalReferenceCell extends ListCell<ExternalRef> {
        private ListedReferenceTypes lrt = ListedReferenceTypes.getListedReferenceTypes();
        private HBox layout = new HBox();
        private final ChoiceBox<String> chcCategory;
        private TextField txtRefType = new TextField();
        private TextField txtLocator = new TextField();

        private Optional<ExternalRef> value = Optional.empty();

        protected ExternalReferenceCell() {
            chcCategory = new ChoiceBox();
            chcCategory.getItems().addAll(categories);
            layout.setMaxWidth(Double.MAX_VALUE);
            txtRefType.setMinWidth(300);
            txtRefType.setMaxWidth(300);
            txtLocator.setMaxWidth(Double.MAX_VALUE);

            txtRefType.textProperty().addListener((observable, oldValue, newValue) -> {
                try {
                    if (value.isPresent()) {
                        value.get().setReferenceType(SpdxLogic.getReferenceType(newValue));
                    }
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });

            chcCategory.valueProperty().addListener((observable, oldValue, newValue) -> {
                if (value.isPresent()) {
                    try {
                        value.get().setReferenceCategory(ExternalRef.ReferenceCategory.fromTag(StringUtils.upperCase(newValue)));
                    } catch (InvalidSPDXAnalysisException e) {
                        throw new RuntimeException(e);
                    }
                }
            });

            txtLocator.textProperty().addListener((observable, oldValue, newValue) -> {
                if (value.isPresent()) {
                    value.get().setReferenceLocator(newValue);
                }
            });

            layout.getChildren().addAll(chcCategory, txtRefType, txtLocator);
        }

        @Override
        protected void updateItem(ExternalRef item, boolean empty) {
            super.updateItem(item, empty);

            this.value = empty ? Optional.empty() : Optional.of(item);
            this.layout.setVisible(!empty);
            try {
                txtRefType.setText(empty ? "" : item.getReferenceType().getReferenceTypeUri().toString());
                txtLocator.setText(empty ? "" : item.getReferenceLocator());
                if (!empty){
                    chcCategory.setValue(WordUtils.capitalize(item.getReferenceCategory().getTag()));
                }
            } catch (InvalidSPDXAnalysisException e) {
                throw new RuntimeException(e);
            }

            setGraphic(layout);
        }
    }


    @FXML
    private ListView<ExternalRef> lstExternalRefs;

    @FXML
    private Button btnAdd;

    @FXML
    private Button btnRemove;

    private SpdxPackage pkg;

    public ExternalRefListControl(SpdxPackage pkg) {
        this.pkg = pkg;
    }


    @FXML
    void initialize() {
        assert btnAdd != null : "fx:id=\"btnAdd\" was not injected: check your FXML file 'ExternalRefList.fxml'.";
        assert btnRemove != null : "fx:id=\"btnRemove\" was not injected: check your FXML file 'ExternalRefList.fxml'.";
        assert lstExternalRefs != null : "fx:id=\"lstExternalRefs\" was not injected: check your FXML file 'ExternalRefList.fxml'.";

        btnRemove.setDisable(false);
        btnAdd.setOnAction(event -> {
            try {
                ArrayList<ExternalRef> externalRefs = Lists.newArrayList(pkg.getExternalRefs());
                ExternalRef newRef = new ExternalRef(ExternalRef.ReferenceCategory.referenceCategory_packageManager, ListedReferenceTypes.getListedReferenceTypes().getListedReferenceTypeByName("maven-central"), "", "Comment");
                lstExternalRefs.getItems().addAll(newRef);
                externalRefs.add(newRef);
                pkg.setExternalRefs(externalRefs.toArray(new ExternalRef[externalRefs.size()]));
            } catch (InvalidSPDXAnalysisException e) {
                throw new RuntimeException(e);
            }
        });

        lstExternalRefs.setCellFactory(param -> new ExternalReferenceCell());
        lstExternalRefs.getItems().clear();

        try {
            lstExternalRefs.getItems().addAll(pkg.getExternalRefs());
        } catch (InvalidSPDXAnalysisException e) {
            throw new RuntimeException(e);
        }
    }

    public Node getUi() {
        try {
            FXMLLoader loader = new FXMLLoader(ExternalRefListControl.class.getResource("/ExternalRefList.fxml"));
            loader.setController(this);
            AnchorPane pane = loader.load();
            return pane;
        } catch (IOException ioe) {
            throw new RuntimeException("Unable to load scene for License editor dialog");
        }
    }
}


