package spdxedit.util;

import javafx.util.StringConverter;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spdx.rdfparser.InvalidSPDXAnalysisException;
import org.spdx.rdfparser.license.ExtractedLicenseInfo;
import org.spdx.rdfparser.license.ListedLicenses;
import org.spdx.rdfparser.license.SpdxListedLicense;
import org.spdx.rdfparser.model.SpdxDocument;

/**
 * Contains converters between SPDX model elements and strings that implement
 * FX's StringConverter interface
 */
public class StringConverters {
    private static final Logger logger = LoggerFactory.getLogger(StringConverters.class);

    public static final StringConverter<SpdxListedLicense> SPDX_LISTED_LICENSE_CONVERTER = new StringConverter<SpdxListedLicense>() {
        @Override
        public String toString(SpdxListedLicense license) {
            return license.getName();
        }

        @Override
        public SpdxListedLicense fromString(String name) {
            try {
                ListedLicenses listedLicenses = ListedLicenses.getListedLicenses();
                SpdxListedLicense result = null;
                for (String licenseId : listedLicenses.getSpdxListedLicenseIds()) {
                    SpdxListedLicense license = listedLicenses.getListedLicenseById(licenseId);
                    if (StringUtils.equals(license.getName(), name)) {
                        result = license;
                        break;
                    }
                }
                if (result == null) {
                    logger.error("Unable to find listed license for name " + name);
                }
                return result;
            } catch (InvalidSPDXAnalysisException e) {
                //Should never ever happen, so fail heinously!
                throw new RuntimeException(e);
            }
        }
    };


    public static StringConverter<ExtractedLicenseInfo> createStringConverterForDocument(final SpdxDocument document) {

        return new StringConverter<ExtractedLicenseInfo>() {
            @Override
            public String toString(ExtractedLicenseInfo object) {
                return object.getName();
            }

            @Override
            public ExtractedLicenseInfo fromString(String string) {
                try {
                    ExtractedLicenseInfo result = null;
                    for (ExtractedLicenseInfo extractedLicenseInfo : document.getExtractedLicenseInfos()) {
                        if (StringUtils.equals(extractedLicenseInfo.getName(), string)) {
                            result = extractedLicenseInfo;
                            break;
                        }
                    }
                    if (result == null) {
                        logger.error("Unable to find extracted license with name " + string);
                    }
                    return result;
                } catch (InvalidSPDXAnalysisException e) {
                    throw new RuntimeException(e);
                }
            }
        };

    }
}