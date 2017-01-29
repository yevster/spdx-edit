package spdxedit;

import javafx.scene.control.ChoiceDialog;
import spdxedit.io.FileDataType;

import java.util.Optional;

/**
 * A dialog that obtains a data type
 */
public final class IoFileTypeSelectionDialog{

    public static Optional<FileDataType> getDataType(String title){
        ChoiceDialog<FileDataType> dialog = new ChoiceDialog<>();
        dialog.setTitle(title);
        dialog.setHeaderText("Select data file type:");
        dialog.getItems().addAll(FileDataType.values());

        return dialog.showAndWait();

    }

}
