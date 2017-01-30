package spdxedit;

import javafx.scene.control.ChoiceDialog;
import javafx.scene.image.ImageView;
import spdxedit.io.FileDataType;
import spdxedit.util.UiUtils;

import java.util.Optional;

/**
 * A dialog that obtains a data type
 */
public final class IoFileTypeSelectionDialog {

    private static final ChoiceDialog<FileDataType> fileTypeChoiceDialog;

    static {
        fileTypeChoiceDialog = new ChoiceDialog<>();
        fileTypeChoiceDialog.setTitle(Main.APP_TITLE);
        fileTypeChoiceDialog.setHeaderText("Select data file type:");
        fileTypeChoiceDialog.getItems().addAll(FileDataType.values());
        fileTypeChoiceDialog.setGraphic(UiUtils.ICON_IMAGE_VIEW_SMALL);
        fileTypeChoiceDialog.setSelectedItem(fileTypeChoiceDialog.getItems().get(0));
    }


    public static Optional<FileDataType> getDataType(String title) {
        return fileTypeChoiceDialog.showAndWait();

    }

}
