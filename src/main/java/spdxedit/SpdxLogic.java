package spdxedit;

import com.google.common.collect.ImmutableSet;
import com.google.common.net.MediaType;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spdx.rdfparser.InvalidSPDXAnalysisException;
import org.spdx.rdfparser.SpdxPackageVerificationCode;
import org.spdx.rdfparser.license.AnyLicenseInfo;
import org.spdx.rdfparser.license.ListedLicenses;
import org.spdx.rdfparser.model.SpdxFile;
import org.spdx.rdfparser.model.SpdxFile.FileType;
import org.spdx.rdfparser.model.SpdxPackage;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Objects;
import java.util.Set;


public class SpdxLogic {
    private static final Logger logger = LoggerFactory.getLogger(SpdxLogic.class);

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
                            SpdxFile file = new SpdxFile(path2.getFileName().toString(), fileTypes, checksum, null, null, null, null, null, null);
                            return file;
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
        DigestInputStream dis = new DigestInputStream(Files.newInputStream(path, StandardOpenOption.READ), checksumDigest);
        byte[] resultBytes = checksumDigest.digest();
        return new String(resultBytes);

    }
}

