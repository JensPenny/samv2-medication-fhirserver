package be.fhir.penny;

import be.fhir.penny.db.AmpRepository;
import be.fhir.penny.db.SQLiteDbProvider;
import be.fhir.penny.model.Samv2MedicinalProductDefinition;
import be.fhir.penny.provider.MedicinalProductDefinitionProvider;
import org.hl7.fhir.r5.model.IdType;
import org.junit.Test;
import org.sqlite.SQLiteOpenMode;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.StringJoiner;
import java.util.stream.Collectors;

public class AmpIntegrationTest {
    private static final String SAMV2_SQLITE_LOCATION = "testdb/8058/opt/samtosql/8058.db"; //Links to the testdb folder

    @Test
    public void testAmpRepo() {
        //Initialize the DB layer. Error completely if this fails
        SQLiteDbProvider samv2SqliteProvider; //SQLite connection to the samv2 database
        try {
            samv2SqliteProvider = new SQLiteDbProvider(SAMV2_SQLITE_LOCATION, SQLiteOpenMode.READONLY);
            AmpRepository ampRepo = new AmpRepository(samv2SqliteProvider);
            Collection<AmpRepository.AmpInfoContainer> ampInfos = ampRepo.getAmpInfo("SAM000025-00");
        } catch (SQLException e) {
            throw new RuntimeException("Failed to open DB" + SAMV2_SQLITE_LOCATION, e);
        }
    }

    @Test
    public void testReadAllMedicinalProductDefinitions() {
        //First, select all valid ampcodes
        //Then, transform to definitions and check if you can find out useful stuff

        SQLiteDbProvider samv2SqliteProvider; //SQLite connection to the samv2 database
        try {
            samv2SqliteProvider = new SQLiteDbProvider(SAMV2_SQLITE_LOCATION, SQLiteOpenMode.READONLY);
            AmpRepository ampRepo = new AmpRepository(samv2SqliteProvider);
            MedicinalProductDefinitionProvider provider = new MedicinalProductDefinitionProvider(ampRepo);
            Set<String> ampCodes = new HashSet<>();
            try (PreparedStatement statement = samv2SqliteProvider.getConnection().prepareStatement("select distinct code from AMP_FAMHP where validTo is null or validTo > date('now')")) {
                ResultSet resultSet = statement.executeQuery();
                while (resultSet.next()) {
                    String ampCode = resultSet.getString("code");
                    ampCodes.add(ampCode);
                }
            }

            System.out.println("evaluating " + ampCodes.size() + "amps");
            for (String ampCode : ampCodes) {
                Samv2MedicinalProductDefinition def = provider.readMedicinalProductDefinition(new IdType("SAM-AMPCODE", ampCode));

                System.out.println("Name: " + def.getName().get(0).getProductName());
                if (def.getClassification().size() > 1) {
                    String asString = def.getClassification().stream().map(atc -> atc.getText()).collect(Collectors.joining(","));
                    System.out.println("code " + ampCode + " has multiple ATC's: " + asString);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to open DB" + SAMV2_SQLITE_LOCATION, e);
        }

    }


}
