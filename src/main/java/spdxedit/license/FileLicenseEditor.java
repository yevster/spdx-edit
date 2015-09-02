package spdxedit.license;


import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Window;
import org.spdx.rdfparser.InvalidSPDXAnalysisException;
import org.spdx.rdfparser.SpdxDocumentContainer;
import org.spdx.rdfparser.model.SpdxFile;

public class FileLicenseEditor {
    private final SpdxFile editedFile;

    private LicenseEditControl concludedLicenseEditControl;



    private FileLicenseEditor(SpdxFile file, SpdxDocumentContainer documentContainer){
        this.editedFile  = file;
        concludedLicenseEditControl = new LicenseEditControl(documentContainer);
    }

   public static void editConcludedLicense(SpdxFile file, SpdxDocumentContainer container) {

       Stage dialog = new Stage();
       dialog.initStyle(StageStyle.UTILITY);
       LicenseEditControl control = new LicenseEditControl(container);
       Scene scene = new Scene(control.getUi());
       dialog.setScene(scene);
       dialog.showAndWait();
       try {
           file.setLicenseConcluded(control.getValue());
       } catch (InvalidSPDXAnalysisException e) {
           throw new RuntimeException(e);

       }

   }

}
