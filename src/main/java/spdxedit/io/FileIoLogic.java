package spdxedit.io;

import com.google.common.base.Joiner;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TextArea;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.riot.RDFFormat;
import org.apache.jena.riot.system.PrefixMap;
import org.apache.jena.riot.system.PrefixMapFactory;
import org.apache.jena.riot.writer.JsonLDWriter;
import org.apache.jena.riot.writer.TurtleWriter;
import org.apache.jena.sparql.core.DatasetGraphFactory;
import org.spdx.rdfparser.InvalidSPDXAnalysisException;
import org.spdx.rdfparser.SPDXDocumentFactory;
import org.spdx.rdfparser.SpdxDocumentContainer;
import org.spdx.rdfparser.model.SpdxDocument;
import org.spdx.tag.CommonCode;
import org.spdx.tools.TagToRDF;

import java.io.*;
import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

/**
 * Created by ybronshteyn on 1/29/17.
 */
public class FileIoLogic {

    private static final RDFFormat JSON_LD_FORMAT = RDFFormat.JSONLD_COMPACT_PRETTY;

    public static void writeRdfXml(File file, SpdxDocument document) throws IOException {
        try (FileWriter writer = new FileWriter(file)) {
            document.getDocumentContainer().getModel().write(writer);
        }
    }

    public static SpdxDocument loadRdfXml(File file) throws IOException, InvalidSPDXAnalysisException {
        return SPDXDocumentFactory.createSpdxDocument(file.getAbsolutePath());
    }


    public static void writeTagValue(File file, SpdxDocument document) throws IOException {
        Properties constants = CommonCode
                .getTextFromProperties("org/spdx/tag/SpdxTagValueConstants.properties");
        try (FileOutputStream os = new FileOutputStream(file); PrintWriter out = new PrintWriter(os);) {
            // print document to a file using tag-value format
            CommonCode.printDoc(document, out, constants);
        } catch (InvalidSPDXAnalysisException e) {
            throw new RuntimeException(("Illegal SPDX - unable to convert to tag/value"), e);
        }
    }

    public static SpdxDocument loadTagValue(File file) throws IOException, InvalidSPDXAnalysisException {
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
        } catch (Exception e) {
            if (e instanceof InvalidSPDXAnalysisException) throw (InvalidSPDXAnalysisException) e;
            if (e instanceof IOException) throw (IOException) e;
            throw new IOException("Unable to read/parse tag-value file " + file.getAbsolutePath(), e);
        }
    }

    public static void writeTurtle(File file, SpdxDocument document) throws IOException {
        try (FileWriter writer = new FileWriter(file)) {
            TurtleWriter turtleWriter = new TurtleWriter();
            Model model = document.getDocumentContainer().getModel();
            PrefixMap prefixMap = PrefixMapFactory.create(model.getNsPrefixMap());
            turtleWriter.write(writer, model.getGraph(), prefixMap, document.getDocumentUri(), null);
        } catch (InvalidSPDXAnalysisException e) {
            throw new RuntimeException("Document namespace missing. The document is not complete");
        }
    }

    public static SpdxDocument readTurtle(File file) throws IOException, InvalidSPDXAnalysisException {
        try (FileReader reader = new FileReader(file)) {
            Model model = ModelFactory.createDefaultModel();
            model.getReader("TURTLE").read(model, reader, getBaseUrl(file));
            SpdxDocumentContainer container = new SpdxDocumentContainer(model);
            return container.getSpdxDocument();
        }
    }

    public static void writeJsonLd(File file, SpdxDocument document) throws IOException {
        try (FileWriter writer = new FileWriter(file)) {
            Model model = document.getDocumentContainer().getModel();
            JsonLDWriter jsonLDWriter = new JsonLDWriter(JSON_LD_FORMAT);
            PrefixMap prefixMap = PrefixMapFactory.create(model.getNsPrefixMap());
            jsonLDWriter.write(writer, DatasetGraphFactory.create(model.getGraph()), prefixMap, document.getDocumentUri(), null);
        } catch (InvalidSPDXAnalysisException e) {
            throw new RuntimeException("Document namespace missing. The document is not complete");
        }
    }

    public static SpdxDocument readJsonLd(File file) throws IOException, InvalidSPDXAnalysisException {
        try (FileReader reader = new FileReader(file)) {
            Model model = ModelFactory.createDefaultModel();
            model.getReader(JSON_LD_FORMAT.getLang().getName()).read(model, reader, getBaseUrl(file));
            SpdxDocumentContainer container = new SpdxDocumentContainer(model);
            return container.getSpdxDocument();

        }
    }

    private static String getBaseUrl(File file) throws IOException {
        return  Paths.get(file.getAbsolutePath()).toUri().toString();
    }

}
