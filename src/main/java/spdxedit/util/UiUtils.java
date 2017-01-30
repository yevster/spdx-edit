package spdxedit.util;

import javafx.scene.Node;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Modality;
import javafx.stage.Stage;
import spdxedit.MainSceneController;

/**
 * Common UI utilities
 */
public class UiUtils {
    public static final ImageView ICON_IMAGE_VIEW = new ImageView(MainSceneController.class.getResource("/img/document-8x.png").toString());
    public static final ImageView ICON_IMAGE_VIEW_SMALL = new ImageView(MainSceneController.class.getResource("/img/document-2x.png").toString());

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

    public static AnchorPane wrapInAnchor(Node control){
        AnchorPane result = new AnchorPane();
        result.getChildren().addAll(control);
        AnchorPane.setRightAnchor(control, 0D);
        AnchorPane.setLeftAnchor(control, 0D);
        AnchorPane.setTopAnchor(control, 0D);
        AnchorPane.setBottomAnchor(control, 0D);
        return result;
    }
}
