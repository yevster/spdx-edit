package  spdxedit.io;

import org.spdx.rdfparser.model.SpdxDocument;

import java.io.File;
import java.io.IOException;

@FunctionalInterface
public interface FileOutputStrategy {
     void write(File file, SpdxDocument document) throws IOException;
}