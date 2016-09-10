package spdxedit;

import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.image.ImageView;
import javafx.stage.Modality;
import javafx.stage.Stage;

/**
 * Common UI utilities
 */
public class UiUtils {
    public static final ImageView ICON_IMAGE_VIEW = new ImageView(MainSceneController.class.getResource("/img/document-8x.png").toString());

    /**
     * Get a modal dialog with the application icon
     */
    public static <T> Dialog<T> newDialog(String title, ButtonType...buttonTypes){
        Dialog<T> result = new Dialog<T>();
        result.setTitle(title);
        result.initModality(Modality.APPLICATION_MODAL);
        result.getDialogPane().getButtonTypes().addAll(buttonTypes);
        ((Stage)result.getDialogPane().getScene().getWindow()).getIcons().addAll(ICON_IMAGE_VIEW.getImage());
        return result;
    }
}
