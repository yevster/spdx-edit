package spdxedit.io;

import com.google.common.base.Joiner;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TextArea;
import org.spdx.rdfparser.InvalidSPDXAnalysisException;
import org.spdx.rdfparser.SPDXDocumentFactory;
import org.spdx.rdfparser.SpdxDocumentContainer;
import org.spdx.rdfparser.model.SpdxDocument;
import org.spdx.tag.CommonCode;
import org.spdx.tools.TagToRDF;

import java.io.*;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

/**
 * Created by ybronshteyn on 1/29/17.
 */
public class FileIoLogic {
    public static void writeRdfXml(File file, SpdxDocument document) throws IOException{
       try(FileWriter writer = new FileWriter(file)){
           document.getDocumentContainer().getModel().write(writer);
       }
    }

    public static SpdxDocument loadRdfXml(File file) throws IOException, InvalidSPDXAnalysisException{
        return SPDXDocumentFactory.createSpdxDocument(file.getAbsolutePath());
    }


    public static void writeTagValue(File file, SpdxDocument document) throws IOException{
        Properties constants = CommonCode
                .getTextFromProperties("org/spdx/tag/SpdxTagValueConstants.properties");
        try (FileOutputStream os = new FileOutputStream(file); PrintWriter out = new PrintWriter(os);) {
            // print document to a file using tag-value format
            CommonCode.printDoc(document, out, constants);
        } catch (InvalidSPDXAnalysisException e){
            throw new RuntimeException(("Illegal SPDX - unable to convert to tag/value"), e);
        }
    }

    public static SpdxDocument loadTagValue(File file) throws IOException, InvalidSPDXAnalysisException{
        try (FileInputStream in = new FileInputStream(file)) {
            List<String> warnings = new LinkedList<>();
            SpdxDocumentContainer container = TagToRDF.convertTagFileToRdf(in, "RDF/XML", warnings);
            if (warnings.size() > 0) {
                Alert warningsAlert = new Alert(Alert.AlertType.WARNING, "Warnings occured in parsing Tag document", ButtonType.OK);
                TextArea warningList = new TextArea();
                warningList.setText(Joiner.on("\n").join(warnings));
                warningsAlert.getDialogPane().setExpandableContent(warningList);
                warningsAlert.showAndWait();
            }
            return container.getSpdxDocument();
        } catch (Exception e){
            if (e instanceof InvalidSPDXAnalysisException) throw (InvalidSPDXAnalysisException)e;
            if (e instanceof IOException) throw (IOException)e;
            throw new IOException("Unable to read/parse tag-value file "+file.getAbsolutePath(), e);
        }
    }

}
