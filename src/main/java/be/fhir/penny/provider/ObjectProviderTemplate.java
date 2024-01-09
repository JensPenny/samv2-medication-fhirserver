package be.fhir.penny.provider;

import be.fhir.penny.db.CodeSystemRepository;
import ca.uhn.fhir.rest.annotation.*;
import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.param.TokenParam;
import ca.uhn.fhir.rest.server.IResourceProvider;
import ca.uhn.fhir.rest.server.exceptions.NotImplementedOperationException;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r5.model.CodeSystem;
import org.hl7.fhir.r5.model.IdType;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * A unused template with basic CRUD-field implemented
 */
public class ObjectProviderTemplate implements IResourceProvider {

    private final CodeSystemRepository repository;

    public ObjectProviderTemplate(final CodeSystemRepository repository) {
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

    //@Operation(name = "\\$lookup", idempotent = true)



}
