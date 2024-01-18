package be.fhir.penny.provider;

import be.fhir.penny.db.AmpRepository;
import be.fhir.penny.model.Samv2MedicinalProductDefinition;
import ca.uhn.fhir.rest.annotation.IdParam;
import ca.uhn.fhir.rest.annotation.Read;
import ca.uhn.fhir.rest.annotation.RequiredParam;
import ca.uhn.fhir.rest.annotation.Search;
import ca.uhn.fhir.rest.server.IResourceProvider;
import ca.uhn.fhir.rest.server.exceptions.NotImplementedOperationException;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import org.hl7.fhir.r5.model.IdType;
import org.hl7.fhir.r5.model.StringType;
import org.hl7.fhir.r5.model.CodeableConcept;
import org.hl7.fhir.r5.model.Coding;
import org.hl7.fhir.r5.model.MarketingStatus;
import org.hl7.fhir.r5.model.MedicinalProductDefinition;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class MedicinalProductDefinitionProvider implements IResourceProvider {

    private final AmpRepository ampRepository;

    public MedicinalProductDefinitionProvider(@NotNull final AmpRepository ampRepository) {
        this.ampRepository = ampRepository;
    }

    @Override
    public Class<Samv2MedicinalProductDefinition> getResourceType() {
        return Samv2MedicinalProductDefinition.class;
    }

    /**
     * The read operation.
     * Takes a single parameter, and returns a single resource
     *
     * @param ampCode the sam id for the AMP level, plus optionally a version-id
     * @return the resource matching the parameter, or null if it does not exist
     */
    @Read
    public Samv2MedicinalProductDefinition readMedicinalProductDefinition(@IdParam IdType ampCode) {
        if (!ampCode.hasVersionIdPart()) {
            Collection<AmpRepository.AmpInfoContainer> ampInfo = ampRepository.getAmpInfo(ampCode.getIdPart());
            if (ampInfo.isEmpty()) {
                throw new ResourceNotFoundException("Unknown current medicinal product definition: " + ampCode);
            }

            if (ampInfo.size() > 1) {
                throw new IllegalStateException("Multiple medicinal product definitions found for ampcode " + ampCode);
            }

            return ampToFhirMedProduct().apply(ampInfo.stream().findAny().get());
        } else {
            throw new NotImplementedOperationException("Versioned results for this operation are not implemented yet");
        }
    }

    /**
     * Find all AMP's by name
     *
     * @param ampName
     * @return
     */
    @Search()
    public List<Samv2MedicinalProductDefinition> findMedicinalProductsByName(@RequiredParam(name = Samv2MedicinalProductDefinition.SP_NAME) StringType ampName) {
        Collection<AmpRepository.AMP_FAHMP> ampsByName = ampRepository.getAmpsByName(ampName.getValue());
        List<Samv2MedicinalProductDefinition> definitions = Collections.emptyList();//todo

        return definitions;
        //throw new ResourceNotFoundException("Unknown medicinal product definition: " + ampName.toString());
    }

    @NotNull
    private Function<AmpRepository.AmpInfoContainer, Samv2MedicinalProductDefinition> ampToFhirMedProduct() {
        return ampInfo -> {
            //Fetch all companion objects needed to fill this definition. Later we'll optimize to do 1 query

            AmpRepository.AMP_FAHMP amp = ampInfo.amp();
            Samv2MedicinalProductDefinition def = new Samv2MedicinalProductDefinition();
            def.setId(amp.ampCode());
            List<MedicinalProductDefinition.MedicinalProductDefinitionNameComponent> names = new ArrayList<>();
            MedicinalProductDefinition.MedicinalProductDefinitionNameComponent baseName = new MedicinalProductDefinition.MedicinalProductDefinitionNameComponent(amp.officialName());
            names.add(baseName);

            if (amp.prescriptionNameNl() != null) {
                names.add(createName(amp.prescriptionNameNl(), "BE", "nl-BE"));
            }
            if (amp.prescriptionNameEng() != null) {
                names.add(createName(amp.prescriptionNameEng(), "BE", "en"));
            }
            if (amp.prescriptionNameFr() != null) {
                names.add(createName(amp.prescriptionNameFr(), "BE", "fr-BE"));
            }
            if (amp.prescriptionNameGer() != null) {
                names.add(createName(amp.prescriptionNameGer(), "BE", "de-BE"));
            }

            def.setName(names);

            if (amp.blacktriangle()) {
                def.setAdditionalMonitoringIndicator(new CodeableConcept(new Coding("http://hl7.org/fhir/medicinal-product-additional-monitoring", "BlackTriangleMonitoring", "Requirement for Black Triangle Monitoring")));
            }

            def.setType(new CodeableConcept(new Coding("http://hl7.org/fhir/medicinal-product-type", "MedicinalProduct", "Medicinal Product")));
            def.setDomain(new CodeableConcept(new Coding("http://hl7.org/fhir/medicinal-product-domain", "Human", "Human use")));
            def.setVersion(String.valueOf(amp.validFrom().getTime())); //Todo - check what we can use for this. Samv2 version? Startdate? ...
            Status statusFromDates = createStatusFromDates(amp.validFrom(), amp.validTo());
            def.setStatus(statusFromDates.status);
            def.setStatusDate(statusFromDates.statusDate);
            //def.setCombinedPharmaceuticalDoseForm() //not implemented atm - possibly PHFRM

            Collection<AmpRepository.AMPC_TO_ROA> routeOfAdministrations = ampInfo.roas();
            Set<String> addedSnomedCodes = new HashSet<>();
            for (AmpRepository.AMPC_TO_ROA route : routeOfAdministrations) {
                if (Objects.equals(route.standard(), "SNOMED_CT") && !addedSnomedCodes.contains(route.code())) {
                    def.addRoute(new CodeableConcept(new Coding("http://snomed.info/sct", route.code(), route.nameNl())));
                    addedSnomedCodes.add(route.code());
                }
            }
            //def.addSpecialMeasures() //not needed
            //def.setPediatricUseIndicator() //not provided in samv2

            Collection<AmpRepository.AMPP_TO_ATC> atcs = ampInfo.atcs();
            Set<String> addedAtcCodes = new HashSet<>();
            for (AmpRepository.AMPP_TO_ATC atc : atcs) {
                if (!addedAtcCodes.contains(atc.code())) {
                    def.addClassification(new CodeableConcept(new Coding("http://www.whocc.no/atc", atc.code(), atc.description())));
                    addedAtcCodes.add(atc.code());
                }
            }

            //See discussion in 3.MedicinalProductDefinition-first-mapping-notes.md#status
            MarketingStatus marketingStatus = new MarketingStatus();
            marketingStatus.setStatus(new CodeableConcept(new Coding("samv2_amp_status", amp.status(), amp.status()))); //The docs say: This attribute provides information on the status of the marketing of the medicinal product See ISO/TS 20443 for more information and examples, but I don't have free access to those
            def.addMarketingStatus(marketingStatus);

            return def;
        };
    }

    @SuppressWarnings("SameParameterValue") //For now country is always BE
    private static MedicinalProductDefinition.MedicinalProductDefinitionNameComponent createName(String name, String country, String language) {
        MedicinalProductDefinition.MedicinalProductDefinitionNameComponent component = new MedicinalProductDefinition.MedicinalProductDefinitionNameComponent(name);

        if (country != null && language != null) {
            MedicinalProductDefinition.MedicinalProductDefinitionNameUsageComponent usage = new MedicinalProductDefinition.MedicinalProductDefinitionNameUsageComponent();
            usage.setCountry(new CodeableConcept(new Coding("urn:iso:std:iso:3166", country, country))); //Todo - change as a valueset lookup?
            usage.setLanguage(new CodeableConcept(new Coding("urn:ietf:bcp:47", language, language)));
            component.addUsage(usage);
        }

        return component;
    }

    private static final String statusSystem = "http://hl7.org/fhir/publication-status";

    private static Status createStatusFromDates(Date from, Date to) {

        Date now = new Date();
        if (to != null && now.after(to)) {
            //The resource lies in the past
            return new Status(new CodeableConcept(new Coding(statusSystem, "retired", "Retired")), to);
        }

        if (from == null || now.before(from)) {
            return new Status(new CodeableConcept(new Coding(statusSystem, "draft", "Draft")), from);
        }

        return new Status(new CodeableConcept(new Coding(statusSystem, "active", "Active")), from);

    }

    private record Status (CodeableConcept status, Date statusDate){}
}
