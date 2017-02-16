package spdxedit.spdxlogic;

import org.junit.Assert;
import org.junit.Test;
import org.spdx.rdfparser.InvalidSPDXAnalysisException;
import org.spdx.rdfparser.SpdxPackageVerificationCode;
import org.spdx.rdfparser.license.AnyLicenseInfo;
import org.spdx.rdfparser.license.ListedLicenses;
import org.spdx.rdfparser.model.SpdxDocument;
import org.spdx.rdfparser.model.SpdxFile;
import org.spdx.rdfparser.model.SpdxPackage;
import spdxedit.SpdxLogic;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

/**
 * Created by ybronshteyn on 8/17/15.
 */
public class PackageVerificationCodeTest {

    @Test
    public void oneFilePackageTest() throws URISyntaxException, InvalidSPDXAnalysisException, IOException {
        SpdxPackage pkg = new SpdxPackage("Dummy name", ListedLicenses.getListedLicenses().getListedLicenseById("GPL-2.0"),
                new AnyLicenseInfo[]{} /* Licences from files*/,
                null /*Declared licenses*/,
                null /*Download location*/,
                null /*Download Location*/,
                new SpdxFile[]{} /*Files*/,
                new SpdxPackageVerificationCode("Dummy name", new String[]{}));
        Path filePath = Paths.get(this.getClass().getClassLoader().getResource("hashTestFiles/ChecksumTest1.dat").toURI());
        SpdxLogic.addFileToPackage(pkg, filePath, "dummyBaseUri");

        String fileHash = "7c4a8d09ca3762af61e59520943dc26494f8941b";
        Assert.assertEquals(fileHash, SpdxLogic.getChecksumForFile(filePath));
        String expectedPackageVerificationCode = "69c5fcebaa65b560eaf06c3fbeb481ae44b8d618";
        Assert.assertEquals(expectedPackageVerificationCode, SpdxLogic.computePackageVerificationCode(pkg));
        Assert.assertEquals(expectedPackageVerificationCode, pkg.getPackageVerificationCode().getValue());


    }

    @Test
    public void twoFilePackageTest() throws URISyntaxException, InvalidSPDXAnalysisException {
        Path directoryPath = Paths.get(this.getClass().getClassLoader().getResource("hashTestFiles").toURI());
        SpdxDocument doc = SpdxLogic.createEmptyDocument("http://example.org");

        SpdxPackage pkg = SpdxLogic.createSpdxPackageForPath(Optional.of(directoryPath), doc, ListedLicenses.getListedLicenses().getListedLicenseById("GPL-2.0"), "FOO", "NO COMMENT", true);
        String actualVerificationCode = SpdxLogic.computePackageVerificationCode(pkg);
        final String expectedVerificationCode = "36b3d9fdaae5c74d3bc5528c28695236cc54dfd2";
        Assert.assertEquals(expectedVerificationCode, actualVerificationCode);
        SpdxLogic.recomputeVerificationCode(pkg);
        Assert.assertEquals(expectedVerificationCode, pkg.getPackageVerificationCode().getValue());

    }
}
