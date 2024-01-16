package be.fhir.penny;

import be.fhir.penny.db.AmpRepository;
import be.fhir.penny.db.SQLiteDbProvider;
import org.junit.Test;
import org.sqlite.SQLiteOpenMode;

import java.sql.SQLException;
import java.util.Collection;

public class AmpIntegrationTest {
    private static final String SAMV2_SQLITE_LOCATION = "testdb/8058/opt/samtosql/8058.db"; //Links to the testdb folder

    @Test
    public void testAmpRepo(){
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

}
