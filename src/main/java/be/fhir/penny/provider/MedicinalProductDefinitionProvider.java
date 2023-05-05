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
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.StringType;
import org.hl7.fhir.r5.model.*;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

public class MedicinalProductDefinitionProvider implements IResourceProvider {

    private final AmpRepository repository;

    public MedicinalProductDefinitionProvider(@NotNull final AmpRepository repository) {
        this.repository = repository;
    }

    @Override
    public Class<Samv2MedicinalProductDefinition> getResourceType() {
        return Samv2MedicinalProductDefinition.class;
    }

    /**
     * The read operation.
     * Takes a single parameter, and returns a single resource
     *
     * @param samId the sam id for the AMP level, plus optionally a version-id
     * @return the resource matching the parameter, or null if it does not exist
     */
    @Read
    public Samv2MedicinalProductDefinition readMedicinalProductDefinition(@IdParam IdType samId) {
        if (!samId.hasVersionIdPart()) {
            Optional<AmpRepository.AMP_FAHMP> currentAmpById = repository.getCurrentAmpById(samId.getIdPart());
            if (currentAmpById.isEmpty()) {
                throw new ResourceNotFoundException("Unknown current medicinal product definition: " + samId);
            }
            return ampToFhirMedProduct().apply(currentAmpById.get());
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
        Collection<AmpRepository.AMP_FAHMP> ampsByName = repository.getAmpsByName(ampName.getValue());
        List<Samv2MedicinalProductDefinition> definitions = ampsByName.stream()
                .map(ampToFhirMedProduct())
                .collect(Collectors.toList());

        return definitions;
        //throw new ResourceNotFoundException("Unknown medicinal product definition: " + ampName.toString());
    }

    @NotNull
    private static Function<AmpRepository.AMP_FAHMP, Samv2MedicinalProductDefinition> ampToFhirMedProduct() {
        return amp -> {
            Samv2MedicinalProductDefinition def = new Samv2MedicinalProductDefinition();
            def.setId(amp.ampCode());
            MedicinalProductDefinition.MedicinalProductDefinitionNameComponent name = new MedicinalProductDefinition.MedicinalProductDefinitionNameComponent(amp.officialName());
            def.setName(Collections.singletonList(name));

            //Add the other names if applicable
            //if (amp.prescriptionNameNl() != null) {
                //MedicinalProductDefinition.MedicinalProductDefinitionNameComponent prescriptionNl = new MedicinalProductDefinition.MedicinalProductDefinitionNameComponent();
                //prescriptionNl.a
            //}

            if (amp.blacktriangle()) {
                def.setAdditionalMonitoringIndicator(new CodeableConcept(new Coding("http://hl7.org/fhir/medicinal-product-additional-monitoring" , "BlackTriangleMonitoring", "Requirement for Black Triangle Monitoring")));
            }

            //See discussion in 3.MedicinalProductDefinition-first-mapping-notes.md#status
            MarketingStatus marketingStatus = new MarketingStatus();
            marketingStatus.setStatus(new CodeableConcept(new Coding("samv2_amp_status", amp.status(), amp.status()))); //The docs say: This attribute provides information on the status of the marketing of the medicinal product See ISO/TS 20443 for more information and examples, but I don't have free access to those
            def.addMarketingStatus(marketingStatus);

            return def;
        };
    }

    private static MedicinalProductDefinition.MedicinalProductDefinitionNameComponent createName(String name, String country, String language) {
        MedicinalProductDefinition.MedicinalProductDefinitionNameComponent component = new MedicinalProductDefinition.MedicinalProductDefinitionNameComponent(name);

        if (country != null && language != null) {
            MedicinalProductDefinition.MedicinalProductDefinitionNameUsageComponent usage = new MedicinalProductDefinition.MedicinalProductDefinitionNameUsageComponent();
            //usage.setCountry()
            component.addUsage(usage);
        }

        return component;
    }
}
