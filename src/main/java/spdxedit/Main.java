package spdxedit;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import spdxedit.util.UiUtils;

public class Main extends Application {

    public static final String APP_TITLE = "SPDX Edit";

    @Override
    public void start(Stage primaryStage) throws Exception {
        Parent root = FXMLLoader.load(getClass().getResource("/MainScene.fxml"));
        primaryStage.setTitle(APP_TITLE);
        Scene scene = new Scene(root);
        primaryStage.getIcons().clear();
        primaryStage.getIcons().add(UiUtils.ICON_IMAGE_VIEW.getImage());
        primaryStage.setScene(scene);
        primaryStage.show();
    }


    public static void main(String[] args) {
        launch(args);
    }
}
