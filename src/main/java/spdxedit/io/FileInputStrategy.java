package spdxedit.io;

import org.spdx.rdfparser.InvalidSPDXAnalysisException;
import org.spdx.rdfparser.model.SpdxDocument;

import java.io.File;
import java.io.IOException;

@FunctionalInterface
public interface FileInputStrategy {
    SpdxDocument read(File file) throws IOException, InvalidSPDXAnalysisException;
}
