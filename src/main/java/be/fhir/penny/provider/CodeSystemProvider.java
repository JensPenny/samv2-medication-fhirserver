package be.fhir.penny.provider;

import be.fhir.penny.db.CodeSystemRepository;
import be.fhir.penny.servlet.FHIRConfig;
import ca.uhn.fhir.context.BaseRuntimeChildDefinition;
import ca.uhn.fhir.context.BaseRuntimeElementCompositeDefinition;
import ca.uhn.fhir.context.BaseRuntimeElementDefinition;
import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.i18n.Msg;
import ca.uhn.fhir.rest.annotation.*;
import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.param.TokenParam;
import ca.uhn.fhir.rest.server.IResourceProvider;
import ca.uhn.fhir.rest.server.exceptions.InternalErrorException;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import ca.uhn.fhir.rest.server.exceptions.NotImplementedOperationException;
import org.hl7.fhir.instance.model.api.*;
import org.hl7.fhir.r5.model.CodeSystem;
import org.hl7.fhir.r5.model.IdType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.apache.commons.lang3.StringUtils.trim;

public class CodeSystemProvider implements IResourceProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(CodeSystemProvider.class);

    private final CodeSystemRepository repository;

    public CodeSystemProvider(final CodeSystemRepository repository) {
        this.repository = repository;
    }

    @Override
    public Class<? extends IBaseResource> getResourceType() {
        return CodeSystem.class;
    }

    @Update
    public MethodOutcome update(HttpServletRequest request,
                                @ResourceParam CodeSystem toUpdate,
                                @IdParam IdType idType,
                                RequestDetails requestDetails) {
        throw new NotImplementedOperationException("Updating is not implemented");
    }

    @Create
    public MethodOutcome create(HttpServletRequest request,
                                @ResourceParam CodeSystem toCreate){
        String result = repository.create(toCreate);
        throw new NotImplementedOperationException("creating is not implemented");
    }

    @Read
    public CodeSystem read(HttpServletRequest request,
                           @IdParam IdType idType) {
        throw new NotImplementedOperationException("read not implemented");
    }

    @Delete
    public MethodOutcome delete(HttpServletRequest request,
                                @IdParam IdType idType) {
        throw new NotImplementedOperationException("delete not implemented");
    }

    @Search
    public List<CodeSystem> search(@RequiredParam(name = CodeSystem.SP_URL)TokenParam params){
        throw new NotImplementedOperationException("search not implemented");
    }


    @Operation(name="$upload-external-code-system")
    public IBaseParameters uploadExternalCodeSystem(
            HttpServletRequest request,
            @OperationParam(name= "system", min = 1, typeName = "uri") IPrimitiveType<String> boxedCodeSystem,
            @OperationParam(name="file", min = 1, max = OperationParam.MAX_UNLIMITED, typeName = "attachment") List<ICompositeType> files
    ) {

        if (boxedCodeSystem == null || boxedCodeSystem.isEmpty()) {
            throw new InvalidRequestException(Msg.code(1137) + ": Upload external codesystem requires an URL");
        }

        if (files == null || files.size() == 0) {
            throw new InvalidRequestException(Msg.code(1138) + ": No files found for external code upload of url " + boxedCodeSystem.getValue());
        }

        LOGGER.info("starting conversion of file descriptors for upload");
        List<FileDescriptor> localFiles = convertCompositeToFiles(files);

        LOGGER.info("found {} file descriptors: {}", localFiles.size(), localFiles.stream().map(Object::toString).collect(Collectors.joining(",")));

        String codeSystemUrl = boxedCodeSystem.getValue();
        codeSystemUrl = trim(codeSystemUrl);

        return null;
    }


    //Based on the JPA code that has support for this. Backported for a custom server
    // https://github.com/hapifhir/hapi-fhir/blob/648d14c52cd3a4c79ad5a2562e16ec437aed1439/hapi-fhir-jpaserver-base/src/main/java/ca/uhn/fhir/jpa/provider/TerminologyUploaderProvider.java#L404
    private List<FileDescriptor> convertCompositeToFiles(List<ICompositeType> files) {
        List<FileDescriptor> convertedFiles = new ArrayList<>();

        if (files != null) {
            for (ICompositeType file : files) {
                BaseRuntimeElementCompositeDefinition<?> elementDefinition = (BaseRuntimeElementCompositeDefinition<?>)FHIRConfig.context.getElementDefinition(file.getClass());
                BaseRuntimeChildDefinition child = elementDefinition.getChildByName("url");
                List<IBase> values = child.getAccessor().getValues(file);
                IPrimitiveType<String> uri = values
                        .stream()
                        .map(value -> (IPrimitiveType<String>) value)
                        .findFirst()
                        .orElseGet(() -> {
                            //Create new primitive
                            BaseRuntimeElementDefinition<?> newDefinition = FHIRConfig.context.getElementDefinition("uri");
                            IPrimitiveType<String> newElement = (IPrimitiveType<String>) newDefinition.newInstance();

                            //Fill it in
                            child.getMutator().setValue(file, newElement);
                            return newElement;
                        });

                String nextUrl = uri.getValue();
                if (nextUrl.startsWith("localfile:")) {
                    String nextLocalFile = nextUrl.substring("localfile:".length());


                    if (isNotBlank(nextLocalFile)) {
                        //ourLog.info("Reading in local file: {}", nextLocalFile);
                        File nextFile = new File(nextLocalFile);
                        if (!nextFile.exists() || !nextFile.isFile()) {
                            throw new InvalidRequestException(Msg.code(1141) + "Unknown file: " + nextFile.getName());
                        }
                        convertedFiles.add(new FileBackedFileDescriptor(nextFile));
                    }

                } else {
                    throw new NotImplementedOperationException("external file creation by uri not supported at this moment");
                    //nextData = AttachmentUtil.getOrCreateData(getContext(), next).getValue();
                    //ValidateUtil.isTrueOrThrowInvalidRequest(nextData != null && nextData.length > 0, "Missing Attachment.data value");
                    //files.add(new ITermLoaderSvc.ByteArrayFileDescriptor(nextUrl, nextData));
                }
            }
        }

        return convertedFiles;
    }
    //@Operation(name = "\\$lookup", idempotent = true)

    private interface FileDescriptor {
        String IMGTHLA_URI = "http://www.ebi.ac.uk/ipd/imgt/hla";
        String LOINC_URI = "http://loinc.org";
        String SCT_URI = "http://snomed.info/sct";
        String ICD10_URI = "http://hl7.org/fhir/sid/icd-10";
        String ICD10CM_URI = "http://hl7.org/fhir/sid/icd-10-cm";
        String IEEE_11073_10101_URI = "urn:iso:std:iso:11073:10101";

        String getFilename();

        InputStream getInputStream();
    }

    public static class FileBackedFileDescriptor implements FileDescriptor {
        private final File myNextFile;

        public FileBackedFileDescriptor(File theNextFile) {
            myNextFile = theNextFile;
        }

        @Override
        public String getFilename() {
            return myNextFile.getAbsolutePath();
        }

        @Override
        public InputStream getInputStream() {
            try {
                return new FileInputStream(myNextFile);
            } catch (FileNotFoundException theE) {
                throw new InternalErrorException(Msg.code(1142) + theE);
            }
        }

        @Override
        public String toString() {
            return "FileBackedFileDescriptor{" +
                    "myNextFile=" + myNextFile +
                    '}';
        }
    }
}
