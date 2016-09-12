package spdxedit;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.net.MediaType;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.ResIterator;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.impl.PropertyImpl;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.text.WordUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spdx.rdfparser.InvalidSPDXAnalysisException;
import org.spdx.rdfparser.SpdxDocumentContainer;
import org.spdx.rdfparser.SpdxPackageVerificationCode;
import org.spdx.rdfparser.license.AnyLicenseInfo;
import org.spdx.rdfparser.license.ExtractedLicenseInfo;
import org.spdx.rdfparser.license.SpdxNoAssertionLicense;
import org.spdx.rdfparser.model.*;
import org.spdx.rdfparser.model.Relationship.RelationshipType;
import org.spdx.rdfparser.model.SpdxFile.FileType;
import org.spdx.rdfparser.referencetype.ReferenceType;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class SpdxLogic {


	private static final Logger logger = LoggerFactory.getLogger(SpdxLogic.class);
	private static final String SPDX_SPEC_VERSION="SPDX-2.1";

	public static SpdxDocument createEmptyDocument(String uri) {

		SpdxDocumentContainer container = null;
		try {
			container = new SpdxDocumentContainer(uri, SPDX_SPEC_VERSION);
			container.getSpdxDocument().getCreationInfo().setCreators(new String[] { "Tool: SPDX Edit" });
			return container.getSpdxDocument();
		} catch (InvalidSPDXAnalysisException e) {
			throw new RuntimeException("Unable to create blank SPDX document", e);
		}

	}

	public static void addPackageToDocument(SpdxDocument document, SpdxPackage pkg) {
		try {
			pkg.addRelationship(new Relationship(document, RelationshipType.DESCRIBED_BY, null));
			document.addRelationship(new Relationship(pkg, RelationshipType.DESCRIBES, null));
		} catch (InvalidSPDXAnalysisException e) {
			throw new RuntimeException("Unable to add package to document");

		}

	}

	public static Stream<SpdxPackage> getSpdxPackagesInDocument(SpdxDocument document) {
        final Property rdfType = new PropertyImpl("http://www.w3.org/1999/02/22-rdf-syntax-ns#", "type");
        final String packageTypeUri = "http://spdx.org/rdf/terms#Package";
        ResIterator resIterator = document.getDocumentContainer().getModel().listResourcesWithProperty(rdfType, document.getDocumentContainer().getModel().getResource(packageTypeUri));
        List<SpdxPackage> result = new LinkedList<SpdxPackage>();
        try {
            while (resIterator.hasNext()) {
                Resource resource = resIterator.nextResource();
                result.add(new SpdxPackage(document.getDocumentContainer(), resource.asNode()));
            }
            return result.stream();
        } catch (InvalidSPDXAnalysisException e) {
            throw new RuntimeException(e);
        }
	}

	public static SpdxDocument createDocumentWithPackages(Iterable<SpdxPackage> packages) {
		try {
			// TODO: Add document properties dialog where real URL can be
			// provided.
			SpdxDocument document = createEmptyDocument("http://url.example.com/spdx/builder");
			for (SpdxPackage pkg : packages) {
				Relationship describes = new Relationship(pkg, RelationshipType.DESCRIBES, null);
				// No inverse relationship in case of multiple generations.
				document.addRelationship(describes);
			}
			return document;
		} catch (InvalidSPDXAnalysisException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Creates a new package with the specified license, name, comment, and root
	 * path.
	 *
	 * @param pkgRootPath
	 *            The path from which the files will be included into the
	 *            package. If absent, creates a "remote" package, i.e. one
	 *            without files, just referencing a remote dependency.
	 * @param name
	 * @param omitHiddenFiles
	 * @param declaredLicense
	 * @param downloadLocation
	 * @return
	 */
	public static SpdxPackage createSpdxPackageForPath(Optional<Path> pkgRootPath, AnyLicenseInfo declaredLicense,
			String name, String downloadLocation, final boolean omitHiddenFiles) {
		Objects.requireNonNull(pkgRootPath);
		try {

			SpdxPackage pkg = new SpdxPackage(name, declaredLicense,
					new AnyLicenseInfo[] {} /* Licences from files */,
					null /* Declared licenses */, declaredLicense, downloadLocation, new SpdxFile[] {} /* Files */,
					new SpdxPackageVerificationCode(null, new String[] {}));
			pkg.setLicenseInfosFromFiles(new AnyLicenseInfo[] { new SpdxNoAssertionLicense() });
			pkg.setCopyrightText("NOASSERTION");

			if (pkgRootPath.isPresent()) {
				// Add files in path
				List<SpdxFile> addedFiles = new LinkedList<>();
				String baseUri = pkgRootPath.get().toUri().toString();
				FileVisitor<Path> fileVisitor = new FileVisitor<Path>() {
					@Override
					public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
						if (omitHiddenFiles && dir.getFileName().toString().startsWith(".")) {
							return FileVisitResult.SKIP_SUBTREE;
						} else
							return FileVisitResult.CONTINUE;
					}

					@Override
					public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
						// Skip if omitHidden is set and this file is hidden.
						if (omitHiddenFiles && (file.getFileName().toString().startsWith(".") || Files.isHidden(file)))
							return FileVisitResult.CONTINUE;
						try {
							SpdxFile addedFile = newSpdxFile(file, baseUri);
							addedFiles.add(addedFile);
						} catch (InvalidSPDXAnalysisException e) {
							throw new RuntimeException(e);
						}
						return FileVisitResult.CONTINUE;
					}

					@Override
					public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
						logger.error("Unable to add file ", file.toAbsolutePath().toString());
						return FileVisitResult.CONTINUE;
					}

					@Override
					public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
						return FileVisitResult.CONTINUE;
					}
				};
				Files.walkFileTree(pkgRootPath.get(), fileVisitor);
				SpdxFile[] files = addedFiles.stream().toArray(size -> new SpdxFile[size]);
				pkg.setFiles(files);
				recomputeVerificationCode(pkg);
			} else {
				//External package
				pkg.setFilesAnalyzed(false);
				pkg.setPackageVerificationCode(null);
			}
			return pkg;
		} catch (InvalidSPDXAnalysisException | IOException e) {
			throw new RuntimeException(e);
		}
	}

	public static SpdxFile addFileToPackage(SpdxPackage pkg, Path newFilePath, String baseUri) {
		try {
			SpdxFile newFile = SpdxLogic.newSpdxFile(newFilePath, baseUri);
			SpdxFile[] newFiles = ArrayUtils.add(pkg.getFiles(), newFile);
			pkg.setFiles(newFiles);
			pkg.setFilesAnalyzed(true);
			recomputeVerificationCode(pkg);
			return newFile;
		} catch (IOException | InvalidSPDXAnalysisException e) {
			throw new RuntimeException(e);
		}
	}

	private static SpdxFile newSpdxFile(Path file, String baseUri) throws IOException, InvalidSPDXAnalysisException {
		String checksum = getChecksumForFile(file);
		FileType[] fileTypes = SpdxLogic.getTypesForFile(file);
		String relativeFileUrl = StringUtils.removeStart(file.toUri().toString(), baseUri);
		return new SpdxFile(relativeFileUrl, fileTypes, checksum, new SpdxNoAssertionLicense(),
				new AnyLicenseInfo[] { new SpdxNoAssertionLicense() }, null, "NOASSERTION", null, null);
	}

	// TODO: Make/find a more exhaustive list
	private static final Set<String> sourceFileExtensions = ImmutableSet.of("c", "cpp", "java", "h", "cs", "cxx",
			"asmx", "mm", "m", "php", "groovy", "ruby", "py");
	private static final Set<String> binaryFileExtensions = ImmutableSet.of("class", "exe", "dll", "obj", "o", "jar",
			"bin");
	private static final Set<String> textFileExtensions = ImmutableSet.of("txt", "text");
	private static final Set<String> archiveFileExtensions = ImmutableSet.of("tar", "gz", "jar", "zip", "7z", "arj");

	// TODO: Add remaining types
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
		return fileTypes.toArray(new FileType[] {});
	}

	public static String getChecksumForFile(Path path) throws IOException {
		try (InputStream is = Files.newInputStream(path, StandardOpenOption.READ)) {
			return DigestUtils.shaHex(is);
		}
	}

	public static String toString(FileType fileType) {
		Objects.requireNonNull(fileType);
		return WordUtils.capitalize(StringUtils.lowerCase(fileType.getTag()));
	}

	public static String toString(RelationshipType relationshipType) {
		Objects.requireNonNull(relationshipType);
		return WordUtils.capitalize(StringUtils.lowerCase(StringUtils.replace(relationshipType.getTag(), "_", " ")));
	}

	/**
	 * Finds the first relationship that the source element has to the target of
	 * the specified type.
	 *
	 * @param source
	 * @param relationshipType
	 * @param target
	 * @return
	 */
	public static Optional<Relationship> findRelationship(SpdxElement source, RelationshipType relationshipType,
			SpdxElement target) {
		Objects.requireNonNull(target);
		List<Relationship> foundRelationships = Arrays.stream(source.getRelationships())
				.filter(relationship -> relationship.getRelationshipType() == relationshipType
						&& Objects.equals(target, relationship.getRelatedSpdxElement()))
				.collect(Collectors.<Relationship>toList());
		assert (foundRelationships.size() <= 1);
		return foundRelationships.size() == 0 ? Optional.empty() : Optional.of(foundRelationships.get(0));

	}

	public static void removeRelationship(SpdxElement source, RelationshipType relationshipType, SpdxElement target) {
		try {
			Objects.requireNonNull(target);
			Relationship[] newRelationships = Arrays.stream(source.getRelationships())
					// Filter out the relationship to remove
					.filter(relationship -> relationship.getRelationshipType() != relationshipType
							&& !Objects.equals(relationship.getRelatedSpdxElement(), target))
					.toArray(size -> new Relationship[size]);
			source.setRelationships(newRelationships);
		} catch (InvalidSPDXAnalysisException e) {
			throw new RuntimeException("Illegal SPDX", e); // Never should
															// happen
		}
	}

	/**
	 * Updates whether or not a file has the specified relationship to the
	 * package.
	 *
	 * @param file
	 * @param pkg
	 * @param relationshipType
	 * @param shouldExist
	 *            Whether or not the file should have the specified relationship
	 *            to the package.
	 */
	public static void setFileRelationshipToPackage(SpdxFile file, SpdxPackage pkg, RelationshipType relationshipType,
			boolean shouldExist) {
		// Assuming no practical usecase requiring enforcement of atomicity
		Optional<Relationship> existingRelationship = findRelationship(file, relationshipType, pkg);
		try {

			if (shouldExist && !existingRelationship.isPresent()) { // Create
																	// the
																	// relationship
																	// if empty.
				ArrayList<Relationship> newRelationships = new ArrayList<>(file.getRelationships().length + 1);
				Arrays.stream(file.getRelationships()).forEach(relationship -> newRelationships.add(relationship));
				newRelationships.add(new Relationship(pkg, relationshipType, null));
				file.setRelationships(newRelationships.toArray(new Relationship[] {}));
			}
			if (!shouldExist && existingRelationship.isPresent()) {
				ArrayList<Relationship> newRelationships = Lists.newArrayList(file.getRelationships());
				boolean removed = newRelationships.remove(existingRelationship);
				assert (removed);
				file.setRelationships(newRelationships.toArray(new Relationship[] {}));
			}
		} catch (InvalidSPDXAnalysisException e) {
			throw new RuntimeException(e);
		}
	}

	public static void removeFilesFromPackage(SpdxPackage pkg, List<SpdxFile> filesToRemove) {
		try {
			SpdxFile[] newFiles = Arrays.stream(pkg.getFiles())
					.filter(currentFile -> !filesToRemove.contains(currentFile)).toArray(size -> new SpdxFile[size]);
			pkg.setFiles(newFiles);
			if (newFiles.length == 0) {
				pkg.setFilesAnalyzed(false);
				pkg.setPackageVerificationCode(null);
			} else {
				recomputeVerificationCode(pkg);
			}
		} catch (InvalidSPDXAnalysisException e) {
			throw new RuntimeException(e);
		}
	}

	// Properties of a package that can be edited on the properties tab.
	// TODO: Create editors, pretty names, etc.
	static final Set<String> EDITABLE_PACKAGE_PROPERTIES = ImmutableSet.of("description", "copyrightText",
			"downloadLocation", "packageFileName", "homepage", "originator", "packageFileName", "summary", "supplier",
			"versionInfo", "comment", "sourceInfo");

	/**
	 * Utility method to make verification code use in stream processing not
	 * suicide-inducing.
	 *
	 * @param pkg
	 * @return
	 */
	private static SpdxPackageVerificationCode getVerificationCodeHandlingException(SpdxPackage pkg) {
		try {
			return pkg.getPackageVerificationCode();
		} catch (InvalidSPDXAnalysisException e) {
			throw new RuntimeException(e);
		}
	}

	private static Checksum getSha1Checksum(SpdxFile file) {
		return Arrays.stream(file.getChecksums())
				.filter(checksum -> checksum.getAlgorithm() == Checksum.ChecksumAlgorithm.checksumAlgorithm_sha1)
				.findFirst().get(); // Every file must have a sha
	}

	public static String computePackageVerificationCode(SpdxPackage pkg) {
		try {
			String combinedSha1s = Arrays.stream(pkg.getFiles())
					.filter(spdxFile -> !ArrayUtils.contains(
							getVerificationCodeHandlingException(pkg).getExcludedFileNames(), spdxFile.getName())) // Filter
																													// out
																													// excluded
																													// files
					.map(SpdxLogic::getSha1Checksum) // Get sha1 checksum for
														// each file
					.map(Checksum::getValue) // Get the string value of the
												// checksum
					.sorted() // Sort them
					.collect(Collectors.joining()) // Combine them into a single
													// string
			;
			assert (!"".equals(combinedSha1s));

			String result = DigestUtils.shaHex(combinedSha1s);
			return result;

		} catch (InvalidSPDXAnalysisException e) {
			throw new RuntimeException(e);
		}
	}

	public static void recomputeVerificationCode(SpdxPackage pkg) {
		try {
			pkg.getPackageVerificationCode().setValue(computePackageVerificationCode(pkg));
		} catch (InvalidSPDXAnalysisException e) {
			throw new RuntimeException(e);
		}
	}

	public static void excludeFileFromVerification(SpdxPackage pkg, SpdxFile file) {
		try {
			if (!ArrayUtils.contains(pkg.getPackageVerificationCode().getExcludedFileNames(), file.getName()))
				pkg.getPackageVerificationCode().addExcludedFileName(file.getName());
			recomputeVerificationCode(pkg);
		} catch (InvalidSPDXAnalysisException e) {
			throw new RuntimeException(e);
		}
	}

	public static void unexcludeFileFromVerification(SpdxPackage pkg, SpdxFile file) {
		try {
			ArrayUtils.removeElement(pkg.getPackageVerificationCode().getExcludedFileNames(), file.getName());
			recomputeVerificationCode(pkg);
		} catch (InvalidSPDXAnalysisException e) {
			throw new RuntimeException(e);
		}
	}

	public static boolean isFileExcludedFromVerification(SpdxPackage pkg, SpdxFile file) {
		try {
			return ArrayUtils.contains(pkg.getPackageVerificationCode().getExcludedFileNames(), file.getName());
		} catch (InvalidSPDXAnalysisException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Finds an extracted license in the document with the provided name and
	 * text.
	 * 
	 * @param container
	 * @param name
	 * @param text
	 * @return
	 */
	public static Optional<? extends ExtractedLicenseInfo> findExtractedLicenseByNameAndText(
			SpdxDocumentContainer container, String name, String text) {
		return Arrays.stream(container.getExtractedLicenseInfos())
				.filter(license -> StringUtils.equals(license.getName(), name))
				.filter(license -> StringUtils.equals(license.getExtractedText(), text)).findAny();

	}

	/**
	 * Finds an extracted license in the document with the provided license ID
	 */
	public static Optional<? extends ExtractedLicenseInfo> findExtractedLicenseInfoById(SpdxDocumentContainer container, String licenseId){
		Objects.requireNonNull(licenseId);
		return Arrays.stream(container.getExtractedLicenseInfos()).filter(license -> licenseId.equals(license.getLicenseId())).findAny();
	}

	/**
	 * Adds an extracted license from a file to that file and the SPDX document
	 * container. Does not verify prior existence of the license in file or
	 * document.
	 * 
	 * @param spdxFile
	 * @param documentContainer
	 */
	public static void addExtractedLicenseFromFile(SpdxFile spdxFile, SpdxDocumentContainer documentContainer,
			String licenseId, String licenseName, String licenseText) {
		ExtractedLicenseInfo newLicense = new ExtractedLicenseInfo(licenseId, licenseText);
		newLicense.setName(licenseName);
		try {
			spdxFile.setLicenseInfosFromFiles(ArrayUtils.add(spdxFile.getLicenseInfoFromFiles(), newLicense));
			documentContainer
					.setExtractedLicenseInfos(ArrayUtils.add(documentContainer.getExtractedLicenseInfos(), newLicense));
		} catch (InvalidSPDXAnalysisException e) {
			throw new RuntimeException(e);
		}
	}

	public static ReferenceType getReferenceType(String string){
		try {
			URI uri = new URI(string);
			return new ReferenceType(uri, null, null, null);
		} catch (URISyntaxException | InvalidSPDXAnalysisException e){
			throw new RuntimeException(e);
		}
	}
}
