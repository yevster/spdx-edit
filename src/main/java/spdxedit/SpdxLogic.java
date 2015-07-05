package spdxedit;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.net.MediaType;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.text.WordUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spdx.rdfparser.InvalidSPDXAnalysisException;
import org.spdx.rdfparser.SpdxDocumentContainer;
import org.spdx.rdfparser.SpdxPackageVerificationCode;
import org.spdx.rdfparser.license.AnyLicenseInfo;
import org.spdx.rdfparser.license.ListedLicenses;
import org.spdx.rdfparser.model.*;
import org.spdx.rdfparser.model.Relationship.RelationshipType;
import org.spdx.rdfparser.model.SpdxFile.FileType;

import javax.management.relation.Relation;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.stream.Collectors;


public class SpdxLogic {
    private static final Logger logger = LoggerFactory.getLogger(SpdxLogic.class);


    public static SpdxDocument createDocumentWithPackages(Iterable<SpdxPackage> packages) {
        try {
            //TODO: Add document properties dialog where real URL can be provided.
            SpdxDocumentContainer container = new SpdxDocumentContainer("http://url.example.com/spdx/builder", "SPDX-2.0");
            SpdxDocument document = container.getSpdxDocument();

            for (SpdxPackage pkg : packages) {
                Relationship describes = new Relationship(pkg, RelationshipType.relationshipType_describes, null);
                //No inverse relationship in case of multiple generations.
                document.addRelationship(describes);
            }
            return document;
        } catch (InvalidSPDXAnalysisException e) {
            throw new RuntimeException(e);
        }
    }

    public static SpdxPackage createSpdxPackageForPath(Path path, String licenseId, String name, String comment) {
        Objects.requireNonNull(path);
        try {
            AnyLicenseInfo license = ListedLicenses.getListedLicenses().getListedLicenseById(licenseId);

            SpdxPackage pkg = new SpdxPackage(name, license,
                    new AnyLicenseInfo[]{} /* Licences from files*/,
                    null /*Declared licenses*/,
                    null /*Download location*/,
                    null /*Download Location*/,
                    new SpdxFile[]{} /*Files*/,
                    new SpdxPackageVerificationCode(name, new String[]{}));
            pkg.setComment(comment);

            //Add files in path

            SpdxFile[] files = Files.walk(path)
                    .filter(path1 -> !Files.isDirectory(path1))
                    .map(path2 -> {
                        FileType[] fileTypes = getTypesForFile(path2);
                        try {
                            String checksum = getChecksumForFile(path2);
                            SpdxFile addedFile = new SpdxFile(path2.getFileName().toString(), fileTypes, checksum, null, null, null, null, null, null);
                            return addedFile;
                        } catch (IOException ioe) {
                            logger.error("Unable to get checksum for file " + path.toAbsolutePath().toString() + ". File omitted from package.");
                            return null; //To be filtered downstream
                        } catch (InvalidSPDXAnalysisException e) {
                            throw new RuntimeException(e);
                        }
                    })
                    .filter(file1 -> file1 != null)
                    .toArray(SpdxFile[]::new);
            pkg.setFiles(files);
            return pkg;
        } catch (InvalidSPDXAnalysisException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    //TODO: Make/find a more exhaustive list
    private static final Set<String> sourceFileExtensions = ImmutableSet.of("c", "cpp", "java", "h", "cs", "cxx", "asmx", "mm", "m", "php", "groovy", "ruby", "py");
    private static final Set<String> binaryFileExtensions = ImmutableSet.of("class", "exe", "dll", "obj", "o", "jar", "bin");
    private static final Set<String> textFileExtensions = ImmutableSet.of("txt", "text");
    private static final Set<String> archiveFileExtensions = ImmutableSet.of("tar", "gz", "jar", "zip", "7z", "arj");

    //TODO: Add remaining types
    public static FileType[] getTypesForFile(Path path) {
        String extension = StringUtils.lowerCase(StringUtils.substringAfterLast(path.getFileName().toString(), "."));
        ArrayList<FileType> fileTypes = new ArrayList<>();
        if (sourceFileExtensions.contains(extension)) {
            fileTypes.add(SpdxFile.FileType.fileType_source);
        }
        if (binaryFileExtensions.contains(extension)) {
            fileTypes.add(FileType.fileType_binary);
        }
        if (textFileExtensions.contains(extension)) {
            fileTypes.add(FileType.fileType_text);
        }
        if (archiveFileExtensions.contains(extension)) {
            fileTypes.add(FileType.fileType_archive);
        }
        if ("spdx".equals(extension)) {
            fileTypes.add(FileType.fileType_spdx);
        }
        try {
            String mimeType = Files.probeContentType(path);
            if (StringUtils.startsWith(mimeType, MediaType.ANY_AUDIO_TYPE.type())) {
                fileTypes.add(FileType.fileType_audio);
            }
            if (StringUtils.startsWith(mimeType, MediaType.ANY_IMAGE_TYPE.type())) {
                fileTypes.add(FileType.fileType_image);
            }
            if (StringUtils.startsWith(mimeType, MediaType.ANY_APPLICATION_TYPE.type())) {
                fileTypes.add(FileType.fileType_application);
            }

        } catch (IOException ioe) {
            logger.warn("Unable to access file " + path.toString() + " to determine its type.", ioe);
        }
        return fileTypes.toArray(new FileType[]{});
    }


    public static String getChecksumForFile(Path path) throws IOException {

        final MessageDigest checksumDigest;
        try {
            checksumDigest = MessageDigest.getInstance("SHA1");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
        try (DigestInputStream dis = new DigestInputStream(Files.newInputStream(path, StandardOpenOption.READ), checksumDigest)) {
            byte[] resultBytes = checksumDigest.digest();
            return Hex.encodeHexString(resultBytes);
        }
    }

    public static String toString(FileType fileType) {
        Objects.requireNonNull(fileType);
        return WordUtils.capitalize(StringUtils.lowerCase(fileType.getTag()));
    }


    /**
     * Finds the first relationship that the source element has to the target of the specified type.
     *
     * @param source
     * @param relationshipType
     * @param target
     * @return
     */
    public static Optional<Relationship> findRelationship(SpdxElement source, RelationshipType relationshipType, SpdxElement target) {
        Objects.requireNonNull(target);
        List<Relationship> foundRelationships = Arrays.stream(source.getRelationships())
                .filter(relationship -> relationship.getRelationshipType() == relationshipType && Objects.equals(target, relationship.getRelatedSpdxElement()))
                .collect(Collectors.<Relationship>toList());
        assert (foundRelationships.size() <= 1);
        return foundRelationships.size() == 0 ? Optional.empty() : Optional.of(foundRelationships.get(0));

    }

    /**
     * Updates whether or not a file has the specified relationship to the package.
     *
     * @param file
     * @param pkg
     * @param relationshipType
     * @param shouldExist      Whether or not the file should have the specified relationship to the package.
     */
    public static void setFileRelationshipToPackage(SpdxFile file, SpdxPackage pkg, RelationshipType relationshipType, boolean shouldExist) {
        // Assuming no practical usecase requiring enforcement of atomicity
        Optional<Relationship> existingRelationship = findRelationship(file, relationshipType, pkg);
        try {

            if (shouldExist && !existingRelationship.isPresent()) { //Create the relationship if empty.
                ArrayList<Relationship> newRelationships = new ArrayList<>(file.getRelationships().length + 1);
                Arrays.stream(file.getRelationships()).forEach(relationship -> newRelationships.add(relationship));
                newRelationships.add(new Relationship(pkg, relationshipType, null));
                file.setRelationships(newRelationships.toArray(new Relationship[]{}));
            }
            if (!shouldExist && existingRelationship.isPresent()) {
                ArrayList<Relationship> newRelationships = Lists.newArrayList(file.getRelationships());
                boolean removed = newRelationships.remove(existingRelationship);
                assert (removed);
                file.setRelationships(newRelationships.toArray(new Relationship[]{}));
            }
        } catch (InvalidSPDXAnalysisException e) {
            throw new RuntimeException(e);
        }
    }
}

