package be.fhir.penny.provider;

import be.fhir.penny.db.AmpRepository;
import be.fhir.penny.model.Samv2MedicinalProductDefinition;
import ca.uhn.fhir.rest.annotation.IdParam;
import ca.uhn.fhir.rest.annotation.Read;
import ca.uhn.fhir.rest.annotation.RequiredParam;
import ca.uhn.fhir.rest.annotation.Search;
import ca.uhn.fhir.rest.server.IResourceProvider;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.StringType;
import org.hl7.fhir.r5.model.MedicinalProductDefinition;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
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
     * @param samId
     * @return the resource matching the parameter, or null if it does not exist
     */
    @Read
    public Samv2MedicinalProductDefinition readMedicinalProductDefinition(@IdParam IdType samId) {
        if (!samId.hasVersionIdPart()) {
            //Return latest
        }
        //todo next

        throw new ResourceNotFoundException("Unknown medicinal product definition: " + samId.toString());
    }

    /**
     * Find all AMP's by name
     *
     * @param ampName
     * @return
     */
    @Search()
    public List<Samv2MedicinalProductDefinition> findMedicinalProductsByName(@RequiredParam(name = Samv2MedicinalProductDefinition.SP_NAME) StringType ampName) {
        Collection<AmpRepository.AMP> ampsByName = repository.getAmpsByName(ampName.getValue());
        List<Samv2MedicinalProductDefinition> definitions = ampsByName.stream()
                .map(ampToFhirMedProduct())
                .collect(Collectors.toList());

        return definitions;
        //throw new ResourceNotFoundException("Unknown medicinal product definition: " + ampName.toString());
    }

    @NotNull
    private static Function<AmpRepository.AMP, Samv2MedicinalProductDefinition> ampToFhirMedProduct() {
        return amp -> {
            Samv2MedicinalProductDefinition def = new Samv2MedicinalProductDefinition();
            def.setId(amp.ampCode());
            MedicinalProductDefinition.MedicinalProductDefinitionNameComponent name = new MedicinalProductDefinition.MedicinalProductDefinitionNameComponent(amp.officialName());
            def.setName(Collections.singletonList(name));
            //Other names:
            //MedicinalProduct.MedicinalProductNameComponent officialName = new MedicinalProduct.MedicinalProductNameComponent();
            return def;
        };
    }
}
