package be.fhir.penny.terminology;

import java.io.InputStream;

public interface FileDescriptor {
    String IMGTHLA_URI = "http://www.ebi.ac.uk/ipd/imgt/hla";
    String LOINC_URI = "http://loinc.org";
    String SCT_URI = "http://snomed.info/sct";
    String ICD10_URI = "http://hl7.org/fhir/sid/icd-10";
    String ICD10CM_URI = "http://hl7.org/fhir/sid/icd-10-cm";
    String IEEE_11073_10101_URI = "urn:iso:std:iso:11073:10101";

    String getFilename();

    InputStream getInputStream();
}
