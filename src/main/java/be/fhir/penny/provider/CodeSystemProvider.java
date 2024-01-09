package be.fhir.penny.provider;

import be.fhir.penny.db.CodeSystemRepository;
import be.fhir.penny.terminology.FileBackedFileDescriptor;
import be.fhir.penny.terminology.FileDescriptor;
import be.fhir.penny.servlet.FHIRConfig;
import be.fhir.penny.servlet.UploadStatistics;
import be.fhir.penny.terminology.LoadedFileDescriptors;
import ca.uhn.fhir.context.BaseRuntimeChildDefinition;
import ca.uhn.fhir.context.BaseRuntimeElementCompositeDefinition;
import ca.uhn.fhir.context.BaseRuntimeElementDefinition;
import ca.uhn.fhir.i18n.Msg;
import ca.uhn.fhir.rest.annotation.*;
import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.param.TokenParam;
import ca.uhn.fhir.rest.server.IResourceProvider;
import ca.uhn.fhir.rest.server.exceptions.InternalErrorException;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import ca.uhn.fhir.rest.server.exceptions.NotImplementedOperationException;
import ca.uhn.fhir.util.ParametersUtil;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.csv.QuoteMode;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.instance.model.api.*;
import org.hl7.fhir.r5.model.CodeSystem;
import org.hl7.fhir.r5.model.ConceptMap;
import org.hl7.fhir.r5.model.IdType;
import org.hl7.fhir.r5.model.ValueSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

import static be.fhir.penny.terminology.FileDescriptor.LOINC_URI;
import static be.fhir.penny.terminology.LoincUploadPropertiesEnum.*;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.apache.commons.lang3.StringUtils.trim;

//todo split and sort methods. Zip and csv parsers should be their own packages
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

        try {
            LOGGER.info("starting conversion of file descriptors for upload");
            List<FileDescriptor> localFiles = convertCompositeToFiles(files);

            LOGGER.info("found {} file descriptors: {}", localFiles.size(), localFiles.stream().map(Object::toString).collect(Collectors.joining(",")));

            String codeSystemUrl = boxedCodeSystem.getValue();
            codeSystemUrl = trim(codeSystemUrl);

            UploadStatistics stats;
            LOGGER.info("starting export for {}", codeSystemUrl);
            switch (codeSystemUrl) {
                case LOINC_URI:
                    stats = loadLoinc(localFiles, request);
                    break;
                /*
            case ICD10_URI:
                stats = myTerminologyLoaderSvc.loadIcd10(localFiles, theRequestDetails);
                break;
            case ICD10CM_URI:
                stats = myTerminologyLoaderSvc.loadIcd10cm(localFiles, theRequestDetails);
                break;
            case IMGTHLA_URI:
                stats = myTerminologyLoaderSvc.loadImgthla(localFiles, theRequestDetails);
                break;
            case SCT_URI:
                stats = myTerminologyLoaderSvc.loadSnomedCt(localFiles, theRequestDetails);
                break;
                 */
                default:
                    //stats = myTerminologyLoaderSvc.loadCustom(codeSystemUrl, localFiles, theRequestDetails);
                    throw new NotImplementedOperationException("CodeSystem export not implemented for " + codeSystemUrl);
            }

            IBaseParameters retVal = ParametersUtil.newInstance(FHIRConfig.context);
            ParametersUtil.addParameterToParametersBoolean(FHIRConfig.context, retVal, "success", true);
            ParametersUtil.addParameterToParametersInteger(FHIRConfig.context, retVal, "conceptCount", stats.getUpdatedConceptCount());
            ParametersUtil.addParameterToParametersReference(FHIRConfig.context, retVal, "target", stats.getTarget().getValue());

            return retVal;
        } finally {
            //todo - end request
        }
    }

    private UploadStatistics loadLoinc(List<FileDescriptor> files,
                                       HttpServletRequest request) {
        LoadedFileDescriptors loadedFileDescriptors = new LoadedFileDescriptors(files);
        try (loadedFileDescriptors) {
            Properties uploadProperties = getProperties(loadedFileDescriptors, LOINC_UPLOAD_PROPERTIES_FILE.getCode());
            //Verified working to this part 2023-05-09
            String codeSystemVersionId = uploadProperties.getProperty(LOINC_CODESYSTEM_VERSION.getCode());


            boolean isMakeCurrentVersion = Boolean.parseBoolean(
                    uploadProperties.getProperty(LOINC_CODESYSTEM_MAKE_CURRENT.getCode(), "true"));

            if (StringUtils.isBlank(codeSystemVersionId) && ! isMakeCurrentVersion) {
                throw new InvalidRequestException(Msg.code(864) + "'" + LOINC_CODESYSTEM_VERSION.getCode() +
                        "' property is required when '" + LOINC_CODESYSTEM_MAKE_CURRENT.getCode() + "' property is 'false'");
            }

            List<String> mandatoryFilenameFragments = Arrays.asList(
                    uploadProperties.getProperty(LOINC_ANSWERLIST_FILE.getCode(), LOINC_ANSWERLIST_FILE_DEFAULT.getCode()),
                    uploadProperties.getProperty(LOINC_ANSWERLIST_LINK_FILE.getCode(), LOINC_ANSWERLIST_LINK_FILE_DEFAULT.getCode()),
                    uploadProperties.getProperty(LOINC_DOCUMENT_ONTOLOGY_FILE.getCode(), LOINC_DOCUMENT_ONTOLOGY_FILE_DEFAULT.getCode()),
                    uploadProperties.getProperty(LOINC_FILE.getCode(), LOINC_FILE_DEFAULT.getCode()),
                    uploadProperties.getProperty(LOINC_HIERARCHY_FILE.getCode(), LOINC_HIERARCHY_FILE_DEFAULT.getCode()),
                    uploadProperties.getProperty(LOINC_IEEE_MEDICAL_DEVICE_CODE_MAPPING_TABLE_FILE.getCode(), LOINC_IEEE_MEDICAL_DEVICE_CODE_MAPPING_TABLE_FILE_DEFAULT.getCode()),
                    uploadProperties.getProperty(LOINC_IMAGING_DOCUMENT_CODES_FILE.getCode(), LOINC_IMAGING_DOCUMENT_CODES_FILE_DEFAULT.getCode()),
                    uploadProperties.getProperty(LOINC_PART_FILE.getCode(), LOINC_PART_FILE_DEFAULT.getCode()),
                    uploadProperties.getProperty(LOINC_PART_RELATED_CODE_MAPPING_FILE.getCode(), LOINC_PART_RELATED_CODE_MAPPING_FILE_DEFAULT.getCode()),
                    uploadProperties.getProperty(LOINC_RSNA_PLAYBOOK_FILE.getCode(), LOINC_RSNA_PLAYBOOK_FILE_DEFAULT.getCode()),
                    uploadProperties.getProperty(LOINC_UNIVERSAL_LAB_ORDER_VALUESET_FILE.getCode(), LOINC_UNIVERSAL_LAB_ORDER_VALUESET_FILE_DEFAULT.getCode())
            );
            loadedFileDescriptors.verifyMandatoryFilesExist(mandatoryFilenameFragments);

            LOGGER.info("verified mandatory files");

            List<String> splitPartLinkFilenameFragments = Arrays.asList(
                    uploadProperties.getProperty(LOINC_PART_LINK_FILE_PRIMARY.getCode(), LOINC_PART_LINK_FILE_PRIMARY_DEFAULT.getCode()),
                    uploadProperties.getProperty(LOINC_PART_LINK_FILE_SUPPLEMENTARY.getCode(), LOINC_PART_LINK_FILE_SUPPLEMENTARY_DEFAULT.getCode())
            );
            loadedFileDescriptors.verifyPartLinkFilesExist(splitPartLinkFilenameFragments, uploadProperties.getProperty(LOINC_PART_LINK_FILE.getCode(), LOINC_PART_LINK_FILE_DEFAULT.getCode()));

            List<String> optionalFilenameFragments = Arrays.asList(
                    uploadProperties.getProperty(LOINC_GROUP_FILE.getCode(), LOINC_GROUP_FILE_DEFAULT.getCode()),
                    uploadProperties.getProperty(LOINC_GROUP_TERMS_FILE.getCode(), LOINC_GROUP_TERMS_FILE_DEFAULT.getCode()),
                    uploadProperties.getProperty(LOINC_PARENT_GROUP_FILE.getCode(), LOINC_PARENT_GROUP_FILE_DEFAULT.getCode()),
                    uploadProperties.getProperty(LOINC_TOP2000_COMMON_LAB_RESULTS_SI_FILE.getCode(), LOINC_TOP2000_COMMON_LAB_RESULTS_SI_FILE_DEFAULT.getCode()),
                    uploadProperties.getProperty(LOINC_TOP2000_COMMON_LAB_RESULTS_US_FILE.getCode(), LOINC_TOP2000_COMMON_LAB_RESULTS_US_FILE_DEFAULT.getCode()),
                    uploadProperties.getProperty(LOINC_MAPTO_FILE.getCode(), LOINC_MAPTO_FILE_DEFAULT.getCode()),

                    //-- optional consumer name
                    uploadProperties.getProperty(LOINC_CONSUMER_NAME_FILE.getCode(), LOINC_CONSUMER_NAME_FILE_DEFAULT.getCode()),
                    uploadProperties.getProperty(LOINC_LINGUISTIC_VARIANTS_FILE.getCode(), LOINC_LINGUISTIC_VARIANTS_FILE_DEFAULT.getCode())

            );
            loadedFileDescriptors.verifyOptionalFilesExist(optionalFilenameFragments);

            LOGGER.info("Beginning LOINC processing");
            if (isMakeCurrentVersion) {
                return processLoincFiles(loadedFileDescriptors, request, uploadProperties);
            } else {
                throw new NotImplementedOperationException("only implemented a make-current version of the loinc uploads");
            }
        }
    }

    private UploadStatistics processLoincFiles(LoadedFileDescriptors loadedFileDescriptors, HttpServletRequest request, Properties uploadProperties) {
        return null;
    }

    Properties getProperties(LoadedFileDescriptors theDescriptors, String thePropertiesFile) {
        Properties retVal = new Properties();

        try (InputStream propertyStream = CodeSystemProvider.class.getResourceAsStream("/term/loincupload.properties")) {
            retVal.load(propertyStream);
        } catch (IOException e) {
            throw new InternalErrorException(Msg.code(866) + "Failed to process loinc.properties", e);
        }

        for (FileDescriptor next : theDescriptors.getUncompressedFileDescriptors()) {
            if (next.getFilename().endsWith(thePropertiesFile)) {
                try {
                    try (InputStream inputStream = next.getInputStream()) {
                        retVal.load(inputStream);
                    }
                } catch (IOException e) {
                    throw new InternalErrorException(Msg.code(867) + "Failed to read " + thePropertiesFile, e);
                }
            }
        }
        return retVal;
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
                    //files.add(new ByteArrayFileDescriptor(nextUrl, nextData));
                }
            }
        }

        return convertedFiles;
    }
    //@Operation(name = "\\$lookup", idempotent = true)

}
