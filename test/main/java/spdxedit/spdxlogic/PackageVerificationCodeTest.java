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

import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

/**
 * Created by ybronshteyn on 8/17/15.
 */
public class PackageVerificationCodeTest {

    @Test
    public void oneFilePackageTest() throws URISyntaxException, InvalidSPDXAnalysisException{
        SpdxPackage pkg = new SpdxPackage("Dummy name", ListedLicenses.getListedLicenses().getListedLicenseById("GPL-2.0"),
                new AnyLicenseInfo[]{} /* Licences from files*/,
                null /*Declared licenses*/,
                null /*Download location*/,
                null /*Download Location*/,
                new SpdxFile[]{} /*Files*/,
                new SpdxPackageVerificationCode("Dummy name", new String[]{}));
        Path filePath = Paths.get(this.getClass().getClassLoader().getResource("hashTestFiles/ChecksumTest1.dat").toURI());
        SpdxLogic.addFileToPackage(pkg, filePath, "dummyBaseUri");

        String expectedPackageVerificationCode = "7c4a8d09ca3762af61e59520943dc26494f8941b";
        Assert.assertEquals(expectedPackageVerificationCode, pkg.getPackageVerificationCode().getValue());


    }

    @Test
    public void twoFilePackageTest() throws URISyntaxException {
        Path directoryPath = Paths.get(this.getClass().getClassLoader().getResource("hashTestFiles").toURI());
        SpdxPackage pkg = SpdxLogic.createSpdxPackageForPath(Optional.of(directoryPath), "GPL-2.0", "FOO", "NO COMMENT", true);
        String actualVerificationCode = SpdxLogic.computePackageVerificationCode(pkg);
        final String expectedVerificationCode = "43b1e995dbead10e335145327cf24f8d0ec38f88";
        Assert.assertEquals(expectedVerificationCode, actualVerificationCode);
        SpdxLogic.recomputeVerificationCode(pkg);
        try {
            Assert.assertEquals(expectedVerificationCode, pkg.getPackageVerificationCode().getValue());
        } catch (Exception e){
            throw new RuntimeException(e);
        }
    }
}
