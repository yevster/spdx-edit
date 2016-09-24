package spdxedit;

import com.google.common.collect.ImmutableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import org.spdx.rdfparser.model.SpdxPackage;
import spdxedit.util.UiUtils;

import java.util.List;
import java.util.function.Consumer;

/**
 * Created by yevster on 9/23/16.
 */
public class PackagePropertyEditor {
    private final SpdxPackage spdxPackage;

    final static int rowHeight = 66;
    final static int left = 45;
    final static int fieldLeft = 160;


    public PackagePropertyEditor(SpdxPackage pkg) {
        this.spdxPackage = pkg;

    }

    public void initialize(TitledPane parentContainer) {
        AnchorPane control = new AnchorPane();

        parentContainer.setContent(control);
        int top = 40;

        VBox vbox = new VBox();
        control.getChildren().addAll(vbox);
        AnchorPane.setBottomAnchor(vbox,0D);
        AnchorPane.setTopAnchor(vbox,0D);
        AnchorPane.setLeftAnchor(vbox,0D);
        AnchorPane.setRightAnchor(vbox,0D);

        vbox.setPadding(new Insets(20));
        vbox.setSpacing(11);

        List<Pane> rows = ImmutableList.<Pane>builder().add(createTextBoxEditor("Version Info:", spdxPackage.getVersionInfo(), spdxPackage::setVersionInfo, top += rowHeight),
                createTextAreaEditor("Description:", spdxPackage.getDescription(), spdxPackage::setDescription, top += rowHeight),
                createTextBoxEditor("Summary:", spdxPackage.getSummary(), spdxPackage::setSummary, top += rowHeight),
                createTextBoxEditor("Download Location:", spdxPackage.getDownloadLocation(), spdxPackage::setDownloadLocation, top += rowHeight),
                createTextBoxEditor("Source Info:", spdxPackage.getSourceInfo(), spdxPackage::setSourceInfo, top += rowHeight),
                createTextBoxEditor("Copyright Text:", spdxPackage.getCopyrightText(), spdxPackage::setCopyrightText, top += rowHeight),
                createTextBoxEditor("Package File Name:", spdxPackage.getPackageFileName(), spdxPackage::setPackageFileName, top += rowHeight),
                createTextBoxEditor("Homepage:", spdxPackage.getHomepage(), spdxPackage::setHomepage, top += rowHeight),
                createTextBoxEditor("Originator:", spdxPackage.getOriginator(), spdxPackage::setOriginator, top += rowHeight),
                createTextBoxEditor("Supplier:", spdxPackage.getSupplier(), spdxPackage::setSupplier, top += rowHeight)).build();
        vbox.getChildren().addAll(rows);

        rows.stream().map(Pane::getChildren).flatMap(List::stream).filter(n -> n instanceof TextField).forEach(Node::autosize);
        rows.forEach(Node::autosize);
    }

    private static final Pane createTextBoxEditor(String label, String value, Consumer<String> onUpdate, int top) {

        HBox box = new HBox();
        box.setPrefWidth(Double.MAX_VALUE);
        box.setMaxWidth(Double.MAX_VALUE);
        box.setAlignment(Pos.CENTER_LEFT);
        box.setSpacing(2);
        TextField result = new TextField(value);

        result.setMinWidth(750);
        result.setMaxWidth(Double.MAX_VALUE);
        result.setLayoutX(fieldLeft);
        result.setLayoutY(top);
        result.textProperty().addListener((observable, oldValue, newValue) -> onUpdate.accept(newValue));
        AnchorPane ap = UiUtils.wrapInAnchor(result);

        Label l = new Label(label);
        l.setMinWidth(fieldLeft);
        box.getChildren().addAll(l, ap);

        return UiUtils.wrapInAnchor(box);
    }

    private static final Pane createTextAreaEditor(String label, String value, Consumer<String> onUpdate, int top) {

        HBox box = new HBox();
        box.setPrefWidth(Double.MAX_VALUE);
        box.setMaxWidth(Double.MAX_VALUE);
        box.setAlignment(Pos.CENTER_LEFT);
        box.setSpacing(2);
        TextArea result = new TextArea(value);

        result.setMinWidth(750);
        result.setMaxWidth(Double.MAX_VALUE);
        result.setLayoutX(fieldLeft);
        result.setLayoutY(top);
        result.setMinHeight(rowHeight);
        result.setWrapText(true);
        result.textProperty().addListener((observable, oldValue, newValue) -> onUpdate.accept(newValue));
        AnchorPane ap = UiUtils.wrapInAnchor(result);

        Label l = new Label(label);
        l.setMinWidth(fieldLeft);
        box.getChildren().addAll(l, ap);

        return UiUtils.wrapInAnchor(box);
    }


}
