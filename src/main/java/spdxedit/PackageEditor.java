package spdxedit;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import javafx.beans.property.ReadOnlyStringWrapper;
import com.google.common.collect.Ordering;
import javafx.collections.ListChangeListener;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Window;
import javafx.util.Callback;
import javafx.util.StringConverter;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.controlsfx.control.CheckListView;
import org.controlsfx.control.PropertySheet;
import org.controlsfx.property.BeanPropertyUtils;
import org.controlsfx.property.editor.Editors;
import org.controlsfx.property.editor.PropertyEditor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spdx.rdfparser.InvalidSPDXAnalysisException;
import org.spdx.rdfparser.model.Relationship;
import org.spdx.rdfparser.model.Relationship.RelationshipType;
import org.spdx.rdfparser.model.SpdxFile;
import org.spdx.rdfparser.model.SpdxFile.FileType;
import org.spdx.rdfparser.model.SpdxPackage;
import spdxedit.util.StringableWrapper;

import java.io.IOException;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

import java.util.stream.Collectors;
import java.util.stream.Stream;

public class PackageEditor {

    private static final Logger logger = LoggerFactory.getLogger(PackageEditor.class);

    @FXML
    private TitledPane tabFiles;

    @FXML
    private TreeTableView<SpdxFile> filesTable;

    @FXML
    private TreeTableColumn<SpdxFile, String> tblColumnFile;

    @FXML
    private TitledPane tabRelationships;

    @FXML
    private Button btnOk;

    @FXML
    private Button btnDeleteFileFromPackage;

    @FXML
    private Button btnAddFile;

    @FXML
    private Button btnCopyright;

    /**
     * Package properties
     */


    @FXML
    private TitledPane tabPackageProperties;

    @FXML
    private PropertySheet pkgPropertySheet;

    /***
     * FILE INFORMATION REPRESENTATIONS
     ***/

    @FXML
    private CheckListView<StringableWrapper<FileType>> chkListFileTypes;

    @FXML
    private CheckBox chkDataFile;

    @FXML
    private CheckBox chkTestCase;

    @FXML
    private CheckBox chkDocumentation;

    @FXML
    private CheckBox chkOptionalComponent;

    @FXML
    private CheckBox chkMetafile;

    @FXML
    private CheckBox chkBuildTool;

    @FXML
    private CheckBox chkExcludeFile;

    /**
     * PACKAGE RELATIONSHIP REPRESENTATIONS
     **/
    @FXML
    private ListView<SpdxPackage> lstTargetPackages;

    @FXML
    private ChoiceBox<RelationshipType> chcNewRelationshipType;

    @FXML
    private Button btnAddRelationship;

    @FXML
    private Button btnRemoveRelationship;


    @FXML
    private ListView<StringableWrapper<RelationshipType>> lstPackageRelationships;

    private static final StringConverter<RelationshipType> RELATIONSHIP_TYPE_STRING_CONVERTER = new StringConverter<RelationshipType>() {
        @Override
        public String toString(RelationshipType relationshipType) {
            return SpdxLogic.toString(relationshipType);
        }

        @Override
        public RelationshipType fromString(String string) {
            throw new UnsupportedOperationException("Shoudln't have to convert strings to relationship types");
        }
    };


    //The package being edited
    private SpdxPackage pkg;

    //The file currently being edited
    private SpdxFile currentFile;

    //Packages to which the edited package can have a relationship
    private List<SpdxPackage> otherPackages;


    @FXML
    private void initialize() {
        assert tabFiles != null : "fx:id=\"tabFiles\" was not injected: check your FXML file 'PackageEditor.fxml'.";
        assert tabPackageProperties != null : "fx:id=\"tabPackageProperties\" was not injected: check your FXML file 'PackageEditor.fxml'.";


        assert filesTable != null : "fx:id=\"filesTable\" was not injected: check your FXML file 'PackageEditor.fxml'.";
        assert tblColumnFile != null : "fx:id=\"tblColumnFile\" was not injected: check your FXML file 'PackageEditor.fxml'.";
        assert chkListFileTypes != null : "fx:id=\"chkListFileTypes\" was not injected: check your FXML file 'PackageEditor.fxml'.";
        assert tabRelationships != null : "fx:id=\"tabRelationships\" was not injected: check your FXML file 'PackageEditor.fxml'.";
        assert btnOk != null : "fx:id=\"btnOk\" was not injected: check your FXML file 'PackageEditor.fxml'.";
        assert btnDeleteFileFromPackage != null : "fx:id=\"btnDeleteFileFromPackage\" was not injected: check your FXML file 'PackageEditor.fxml'.";
        assert btnAddFile != null : "fx:id=\"btnAddFile\" was not injected: check your FXML file 'PackageEditor.fxml'.";
        assert btnCopyright != null : "fx:id=\"btnCopyright\" was not injected: check your FXML file 'PackageEditor.fxml'.";


        //File relationship checkboxes
        assert chkDataFile != null : "fx:id=\"chkDataFile\" was not injected: check your FXML file 'PackageEditor.fxml'.";
        assert chkTestCase != null : "fx:id=\"chkTestCase\" was not injected: check your FXML file 'PackageEditor.fxml'.";
        assert chkDocumentation != null : "fx:id=\"chkDocumentation\" was not injected: check your FXML file 'PackageEditor.fxml'.";
        assert chkOptionalComponent != null : "fx:id=\"chkOptionalComponent\" was not injected: check your FXML file 'PackageEditor.fxml'.";
        assert chkMetafile != null : "fx:id=\"chkMetafile\" was not injected: check your FXML file 'PackageEditor.fxml'.";
        assert chkBuildTool != null : "fx:id=\"chkBuildTool\" was not injected: check your FXML file 'PackageEditor.fxml'.";
        assert chkExcludeFile != null : "fx:id=\"chkExcludeFile\" was not injected: check your FXML file 'PackageEditor.fxml'.";

        //Initialise file relationship checkbox handling
        //TODO: Could make this easier by extending the CheckBox control?
        chkDataFile.selectedProperty().addListener((observable, oldValue, newValue) -> addOrRemoveFileRelationshipToPackage(RelationshipType.relationshipType_dataFile, newValue));
        chkTestCase.selectedProperty().addListener((observable, oldValue, newValue) -> addOrRemoveFileRelationshipToPackage(RelationshipType.relationshipType_testcaseOf, newValue));
        chkDocumentation.selectedProperty().addListener((observable, oldValue, newValue) -> addOrRemoveFileRelationshipToPackage(RelationshipType.relationshipType_documentation, newValue));
        chkOptionalComponent.selectedProperty().addListener((observable, oldValue, newValue) -> addOrRemoveFileRelationshipToPackage(RelationshipType.relationshipType_optionalComponentOf, newValue));
        chkMetafile.selectedProperty().addListener((observable, oldValue, newValue) -> addOrRemoveFileRelationshipToPackage(RelationshipType.relationshipType_metafileOf, newValue));
        chkBuildTool.selectedProperty().addListener((observable, oldValue, newValue) -> addOrRemoveFileRelationshipToPackage(RelationshipType.relationshipType_buildToolOf, newValue));
        chkExcludeFile.selectedProperty().addListener((observable, oldValue, newValue) -> handleChkExcludeFileChange(newValue));

        //Package relationship controls
        assert lstTargetPackages != null : "fx:id=\"lstTargetPackages\" was not injected: check your FXML file 'PackageEditor.fxml'.";
        assert chcNewRelationshipType != null : "fx:id=\"chcNewRelationshipType\" was not injected: check your FXML file 'PackageEditor.fxml'.";
        assert btnAddRelationship != null : "fx:id=\"btnAddRelationship\" was not injected: check your FXML file 'PackageEditor.fxml'.";
        assert btnRemoveRelationship != null : "fx:id=\"btnRemoveRelationship\" was not injected: check your FXML file 'PackageEditor.fxml'.";


        //Package properties
        assert pkgPropertySheet != null : "fx:id=\"pkgPropertySheet\" was not injected: check your FXML file 'PackageEditor.fxml'.";
        pkgPropertySheet.setSearchBoxVisible(false);
        pkgPropertySheet.setMode(PropertySheet.Mode.NAME);
        pkgPropertySheet.setModeSwitcherVisible(false);


        //Initialize package relationship controls
        lstTargetPackages.getSelectionModel().selectedItemProperty().addListener((observable1, oldValue1, newValue1) -> handleTargetPackageSelected(newValue1));
        lstTargetPackages.setCellFactory(listView -> new MainSceneController.SpdxPackageListCell());
        lstPackageRelationships.getSelectionModel().selectedItemProperty().addListener((observable1, oldValue1, newValue1) -> btnRemoveRelationship.setDisable(newValue1 == null));
        //Package relationship types
        chcNewRelationshipType.setConverter(RELATIONSHIP_TYPE_STRING_CONVERTER);
        chcNewRelationshipType.getItems().setAll(
                Stream.of(RelationshipType.relationshipType_dynamicLink,
                        RelationshipType.relationshipType_staticLink,
                        RelationshipType.relationshipType_generatedFrom,
                        RelationshipType.relationshipType_generates,
                        RelationshipType.relationshipType_hasPrerequisite,
                        RelationshipType.relationshipType_prerequisiteFor,
                        RelationshipType.relationshipType_other)
                        .collect(Collectors.toList()));
        chcNewRelationshipType.getSelectionModel().selectFirst();
        assert (otherPackages != null); //Constructor finished executing
        lstTargetPackages.getItems().setAll(otherPackages);

        //Initialize other elements
        tblColumnFile.setCellValueFactory(
                (TreeTableColumn.CellDataFeatures<SpdxFile, String> param) ->
                        new ReadOnlyStringWrapper(param.getValue().getValue().getName()));
        //Load all file types into the file type list in order.
        chkListFileTypes.getItems().setAll(Stream.of(FileType.values())
                .sorted(Ordering.usingToString()) //Sort
                .map(fileType -> StringableWrapper.wrap(fileType, SpdxLogic::toString)) //Wrap so that the nice toString function gets used by the checkbox
                .collect(Collectors.toList()));
        chkListFileTypes.getCheckModel().getCheckedItems().addListener(this::handleFileTypeCheckedOrUnchecked);
        filesTable.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> handleFileSelected(newValue));
        filesTable.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        filesTable.setShowRoot(false);


    }

    /**
     * Opens the modal package editor for the provided package.
     *
     * @param pkg               The package to edit.
     * @param relatablePackages Packages to which the edited package may optionally have defined relationships
     * @param parentWindow      The parent window.
     */
    public static void editPackage(final SpdxPackage pkg, final List<SpdxPackage> relatablePackages, Window parentWindow) {

        final PackageEditor packageEditor = new PackageEditor(pkg, relatablePackages);
        final Stage dialogStage = new Stage();
        dialogStage.setTitle("Edit SPDX Package: " + pkg.getName());
        dialogStage.initStyle(StageStyle.DECORATED);
        dialogStage.initModality(Modality.APPLICATION_MODAL);
        dialogStage.setY(parentWindow.getX() + parentWindow.getWidth() / 2);
        dialogStage.setY(parentWindow.getY() + parentWindow.getHeight() / 2);
        dialogStage.setResizable(false);
        try {
            FXMLLoader loader = new FXMLLoader(NewPackageDialog.class.getResource("/PackageEditor.fxml"));
            loader.setController(packageEditor);
            Pane pane = loader.load();
            Scene scene = new Scene(pane);
            dialogStage.setScene(scene);
            //Populate the file list on appearance
            dialogStage.setOnShown(event ->
            {
                try {
                    final SpdxFile dummyfile = new SpdxFile(pkg.getName(), null, null, null, null, null, null, null, null, null, null, null, null);
                    TreeItem<SpdxFile> root = new TreeItem<>(dummyfile);
                    packageEditor.filesTable.setRoot(root);

                    root.getChildren().setAll(Stream.of(pkg.getFiles())
                            .sorted(Comparator.comparing(file -> StringUtils.lowerCase(file.getName()))) //Sort by file name
                            .map(TreeItem<SpdxFile>::new)
                            .collect(Collectors.toList()));
                } catch (InvalidSPDXAnalysisException e) {
                    logger.error("Unable to get files for package " + pkg.getName(), e);
                }
                packageEditor.pkgPropertySheet.getItems().setAll(BeanPropertyUtils.getProperties(pkg, propertyDescriptor ->
                        SpdxLogic.EDITABLE_PACKAGE_PROPERTIES.contains(propertyDescriptor.getName())));
                packageEditor.tabFiles.setExpanded(true);
            });

            //Won't assign this event through FXML - don't want to propagate the stage beyond this point.
            packageEditor.btnOk.setOnMouseClicked(event -> dialogStage.close());
            dialogStage.showAndWait();


        } catch (IOException ioe) {
            throw new RuntimeException("Unable to load dialog", ioe);
        }
    }

    private PackageEditor(SpdxPackage pkg, List<SpdxPackage> relatablePackages) {
        this.pkg = pkg;
        this.otherPackages = relatablePackages;
    }

    //Load the values for the file in all file editing contorls
    private void handleFileSelected(TreeItem<SpdxFile> newSelection) {
        btnDeleteFileFromPackage.setDisable(newSelection == null);
        if (newSelection == null) {
            currentFile = null;
        } else {
            //Set currentFile to null to make sure we don't accidentally edit the previous file
            currentFile = null;
            //Set the file type checkbox values to reflect this file's types
            chkListFileTypes.getCheckModel().clearChecks();

            //If multiple selections, then disable everything but the delete button
            boolean multipleSelections = filesTable.getSelectionModel().getSelectedItems().size() > 1;
            chkListFileTypes.setDisable(multipleSelections);
            //Disable/enable usage checkboxes for multiple selection
            Stream.of(chkExcludeFile, chkBuildTool, chkMetafile, chkOptionalComponent, chkDocumentation, chkTestCase, chkDataFile).forEach(checkbox -> checkbox.setDisable(multipleSelections));
            if (multipleSelections) return;


            //The element lookup by index seems to be broken on the CheckListView control,
            //so we'll have to provide the indices

            chkListFileTypes.getItems().forEach(item -> {
                if (ArrayUtils.contains(newSelection.getValue().getFileTypes(), item.getValue()))
                    chkListFileTypes.getCheckModel().check(item);
                else chkListFileTypes.getCheckModel().clearCheck(item);
            });


            //Reset the relationship checkboxes
            chkDataFile.setSelected(SpdxLogic.findRelationship(newSelection.getValue(), RelationshipType.relationshipType_dataFile, pkg).isPresent());
            chkTestCase.setSelected(SpdxLogic.findRelationship(newSelection.getValue(), RelationshipType.relationshipType_testcaseOf, pkg).isPresent());
            chkDocumentation.setSelected(SpdxLogic.findRelationship(newSelection.getValue(), RelationshipType.relationshipType_documentation, pkg).isPresent());
            chkMetafile.setSelected(SpdxLogic.findRelationship(newSelection.getValue(), RelationshipType.relationshipType_metafileOf, pkg).isPresent());
            chkOptionalComponent.setSelected(SpdxLogic.findRelationship(newSelection.getValue(), RelationshipType.relationshipType_optionalComponentOf, pkg).isPresent());
            chkBuildTool.setSelected(SpdxLogic.findRelationship(newSelection.getValue(), RelationshipType.relationshipType_buildToolOf, pkg).isPresent());

            chkExcludeFile.setSelected(SpdxLogic.isFileExcludedFromVerification(pkg, newSelection.getValue()));

            //Set the file relationship checkboxes
            Relationship[] relationships = newSelection.getValue().getRelationships();

            currentFile = newSelection.getValue();
        }
    }

    private void handleFileTypeCheckedOrUnchecked(ListChangeListener.Change<? extends StringableWrapper<FileType>> change) {
        if (currentFile == null) return;
        FileType[] newFileTypes = change.getList().stream()
                .map(wrappedType -> wrappedType.getValue()) //Unwrap the stringable wrapper
                .toArray(size -> new FileType[size]); //Get array
        try {
            currentFile.setFileTypes(newFileTypes);
        } catch (InvalidSPDXAnalysisException e) {
            logger.error("Unable to update types of file " + currentFile.getName());
        }
    }

    public void handleDeleteFileFromPackageClick(MouseEvent event) {
        List<TreeItem<SpdxFile>> itemsToRemove = ImmutableList.copyOf(filesTable.getSelectionModel().getSelectedItems());
        List<Integer> selectedIndices = ImmutableList.copyOf(filesTable.getSelectionModel().getSelectedIndices());
        filesTable.getSelectionModel().clearSelection();
        filesTable.getRoot().getChildren().removeAll(itemsToRemove);
        SpdxLogic.removeFilesFromPackage(pkg, itemsToRemove.stream().map(TreeItem::getValue).collect(Collectors.toList()));

    }

    private void addOrRemoveFileRelationshipToPackage(RelationshipType relationshipType, boolean shouldExist) {
        if (currentFile != null) {
            SpdxLogic.setFileRelationshipToPackage(currentFile, pkg, relationshipType, shouldExist);
        }
    }

    public void handleBtnAddRelationshipClick(MouseEvent event) {
        assert (lstTargetPackages.getSelectionModel().getSelectedItems().size() > 0);
        assert (chcNewRelationshipType.getSelectionModel().getSelectedIndex() >= 0);
        SpdxPackage targetPackage = lstTargetPackages.getSelectionModel().getSelectedItem();
        RelationshipType relationshipType = chcNewRelationshipType.getSelectionModel().getSelectedItem();
        //TODO: remove existing relationship types from dropdown
        try {
            pkg.addRelationship(new Relationship(targetPackage, relationshipType, null));
            lstPackageRelationships.getItems().add(StringableWrapper.wrap(relationshipType, SpdxLogic::toString));
        } catch (InvalidSPDXAnalysisException e) {
            logger.error("Unable to add package relationship", e);
            new Alert(Alert.AlertType.ERROR, "Unable to add relationship").showAndWait();
        }
    }

    public void handleBtnRemoveRelationshipClick(MouseEvent event) {
        assert (lstTargetPackages.getSelectionModel().getSelectedItems().size() > 0);
        assert (lstPackageRelationships.getSelectionModel().getSelectedItems().size() > 0);
        StringableWrapper<RelationshipType> wrappedRelationshipType = lstPackageRelationships.getSelectionModel().getSelectedItem();
        lstPackageRelationships.getItems().remove(wrappedRelationshipType);
        SpdxLogic.removeRelationship(pkg, wrappedRelationshipType.getValue(), lstTargetPackages.getSelectionModel().getSelectedItem());

    }

    private void handleTargetPackageSelected(SpdxPackage pkg) {
        //Get the relationshpis the edited package has to the selected target package.
        List<StringableWrapper<RelationshipType>> relationshipTypes = Arrays.stream(this.pkg.getRelationships())
                .filter(relationship -> Objects.equals(relationship.getRelatedSpdxElement(), pkg))
                .map(Relationship::getRelationshipType)
                .map(relationshipType -> StringableWrapper.wrap(relationshipType, SpdxLogic::toString))
                .collect(Collectors.toList());
        lstPackageRelationships.getItems().setAll(relationshipTypes);
        btnAddRelationship.setDisable(pkg == null);
    }

    private void handleChkExcludeFileChange(boolean newValue) {
        if (currentFile != null) {
            if (newValue) {
                SpdxLogic.excludeFileFromVerification(pkg, currentFile);
            } else {
                SpdxLogic.unexcludeFileFromVerification(pkg, currentFile);
            }
        }
    }

    public void handleBtnCopyrightClick(MouseEvent event){
        String oldCopyright = currentFile.getCopyrightText();
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle(currentFile.getName() + " copyright");
        dialog.setHeaderText("Enter the copyright text");
        dialog.setContentText(oldCopyright);
        Optional<String> newCopyrightText = dialog.showAndWait();
        if (newCopyrightText.isPresent()){
            currentFile.setCopyrightText(newCopyrightText.get());
        }

    }

}
