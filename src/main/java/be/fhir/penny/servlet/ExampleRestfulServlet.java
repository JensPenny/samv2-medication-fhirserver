package be.fhir.penny.servlet;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import be.fhir.penny.db.AmpRepository;
import be.fhir.penny.db.CodeSystemRepository;
import be.fhir.penny.db.SQLiteDbProvider;
import be.fhir.penny.provider.CodeSystemProvider;
import be.fhir.penny.provider.MedicinalProductDefinitionProvider;
import be.fhir.penny.provider.OrganizationResourceProvider;
import be.fhir.penny.provider.PatientResourceProvider;
import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.narrative.DefaultThymeleafNarrativeGenerator;
import ca.uhn.fhir.narrative.INarrativeGenerator;
import ca.uhn.fhir.rest.server.IResourceProvider;
import ca.uhn.fhir.rest.server.RestfulServer;
import ca.uhn.fhir.rest.server.interceptor.ResponseHighlighterInterceptor;
import org.sqlite.SQLiteOpenMode;

/**
 * This servlet is the actual FHIR server itself
 */
public class ExampleRestfulServlet extends RestfulServer {

	private static final long serialVersionUID = 1L;

	/**
	 * Constructor
	 */
	public ExampleRestfulServlet() {
		super(FHIRConfig.context); // This is an R5 server
	}
	
	/**
	 * This method is called automatically when the
	 * servlet is initializing.
	 */
	private static final String SAMV2_SQLITE_LOCATION = "testdb/6836/opt/samtosql/6836.db"; //Links to the testdb folder
	private static final String FHIR_SQLITE_LOCATION = "testdb/fhir_r5.db"; //Links to the testdb folder
	@Override
	public void initialize() {

		//Initialize the DB layer. Error completely if this fails
		SQLiteDbProvider samv2SqliteProvider = null; //SQLite connection to the samv2 database
		try {
			samv2SqliteProvider = new SQLiteDbProvider(SAMV2_SQLITE_LOCATION, SQLiteOpenMode.READONLY);
		} catch (SQLException e) {
			throw new RuntimeException("Failed to open DB" + SAMV2_SQLITE_LOCATION, e);
		}

		SQLiteDbProvider fhirSqliteProvider = null;  //SQLite connection to the db that will provide the rest of the FHIR concepts
		try {
			fhirSqliteProvider = new SQLiteDbProvider(FHIR_SQLITE_LOCATION, SQLiteOpenMode.READWRITE);
		} catch (SQLException e) {
			throw new RuntimeException("Failed to open DB" + FHIR_SQLITE_LOCATION, e);
		}

		/*
		 * Two resource providers are defined. Each one handles a specific
		 * type of resource.
		 */
		List<IResourceProvider> providers = new ArrayList<>();
		providers.add(new PatientResourceProvider());
		providers.add(new OrganizationResourceProvider());
		providers.add(new MedicinalProductDefinitionProvider(new AmpRepository(samv2SqliteProvider)));
		providers.add(new CodeSystemProvider(new CodeSystemRepository(fhirSqliteProvider)));
		setResourceProviders(providers);
		
		/*
		 * Use a narrative generator. This is a completely optional step, 
		 * but can be useful as it causes HAPI to generate narratives for
		 * resources which don't otherwise have one.
		 */
		INarrativeGenerator narrativeGen = new DefaultThymeleafNarrativeGenerator();
		getFhirContext().setNarrativeGenerator(narrativeGen);

		/*
		 * Use nice coloured HTML when a browser is used to request the content
		 */
		registerInterceptor(new ResponseHighlighterInterceptor());
		
	}

}
