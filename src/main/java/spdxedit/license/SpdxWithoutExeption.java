package spdxedit.license;

import org.spdx.rdfparser.InvalidSPDXAnalysisException;
import org.spdx.rdfparser.license.AnyLicenseInfo;
import org.spdx.rdfparser.model.SpdxPackage;

import java.util.Objects;

/**
 * Convenience methods to invoke SPDX code without the incessant SPDXInvalidAnalysisExceptions
 */
public class SpdxWithoutExeption {
    public static AnyLicenseInfo getLicenseDeclared(SpdxPackage pkg){
        try{
           return  pkg.getLicenseDeclared();
        } catch (InvalidSPDXAnalysisException e){
            throw new RuntimeException(e);
        }
    }

    public static void setLicenseDeclared(SpdxPackage pkg, AnyLicenseInfo license){
        try {
            Objects.requireNonNull(pkg).setLicenseDeclared(license);
        } catch (InvalidSPDXAnalysisException e) {
            e.printStackTrace();
        }
    }

    public static void setLicenseConcluded(SpdxPackage pkg, AnyLicenseInfo license){
        try {
            Objects.requireNonNull(pkg).setLicenseConcluded(license);
        } catch (InvalidSPDXAnalysisException e) {
            e.printStackTrace();
        }
    }
}
