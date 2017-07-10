package spdxedit.io;

import com.google.common.collect.ImmutableList;
import org.spdx.rdfparser.InvalidSPDXAnalysisException;
import org.spdx.rdfparser.model.SpdxDocument;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * Created by ybronshteyn on 1/29/17.
 */
public enum FileDataType {
    RDF_XML("RDF-XML", FileIoLogic::writeRdfXml, FileIoLogic::loadRdfXml, "rdf", "spdx"),
    TAG("Tag:Value", FileIoLogic::writeTagValue, FileIoLogic::loadTagValue, "spdx"),
    TURTLE("RDF-Turtle", FileIoLogic::writeTurtle, FileIoLogic::readTurtle, "ttl", "turtle", "spdx"),
    JSON_LD("JSON-LD", FileIoLogic::writeJsonLd, FileIoLogic::readJsonLd, "json"),
    RDF_JSON("RDF-JSON", FileIoLogic::writeRdfJson, FileIoLogic::readRdfJson, "json"),
    N_TRIPLES("N-TRIPLES", FileIoLogic::writeNtripples, FileIoLogic::readNTripples, "ntripples");

    /*
    JSONLD("JSON-LD", "json"),
    RDF_JSON("RDF/JSON", "json")*/;

    private final String displayName;
    private final List<String> extensions;
    private final FileOutputStrategy fileOutputLogic;
    private final FileInputStrategy fileInputLogic;

    FileDataType(String displayName, FileOutputStrategy fileOutputLogic, FileInputStrategy fileInputLogic, String... extensions) {
        this.displayName = displayName;
        this.extensions = ImmutableList.copyOf(extensions);
        this.fileInputLogic = fileInputLogic;
        this.fileOutputLogic = fileOutputLogic;
    }

    public List<String> getExtensions() {
        return extensions;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void writeToFile(File file, SpdxDocument document) throws IOException {
        fileOutputLogic.write(file, document);
    }

    public SpdxDocument readFromFile(File file) throws IOException, InvalidSPDXAnalysisException {
        return fileInputLogic.read(file);
    }

    @Override
    public String toString() {
        return getDisplayName();
    }
}
