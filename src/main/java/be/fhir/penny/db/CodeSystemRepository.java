package be.fhir.penny.db;

import org.hl7.fhir.r5.model.CodeSystem;

import java.sql.SQLException;
import java.sql.Statement;

public class CodeSystemRepository {
    private final DbProvider provider;

    public CodeSystemRepository(SQLiteDbProvider fhirSqliteProvider) {
        this.provider = fhirSqliteProvider;
        createTables();
    }


    private void createTables() {
        String codeSystemTables = """
                CREATE TABLE IF NOT EXISTS codesystems (
                  id INTEGER PRIMARY KEY AUTOINCREMENT,
                  url TEXT NOT NULL UNIQUE,
                  version TEXT,
                  name TEXT NOT NULL,
                  title TEXT,
                  status TEXT,
                  experimental BOOLEAN,
                  publisher TEXT,
                  description TEXT,
                  content TEXT NOT NULL
                );
                """;

        try (Statement statement = provider.getConnection().createStatement()) {
            statement.execute(codeSystemTables);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    //Todo change return value to optional / outcome / try / ...
    public String create(CodeSystem toCreate) {
        //todo - a LOT of nullchecks :(
        StringBuilder command = new StringBuilder("INSERT INTO codesystems (url, version, name, title, status, experimental, publisher, description, content) VALUES (\n")
                .append("'").append(toCreate.getUrl()).append("'").append('\n')
                .append("'").append(toCreate.getVersion()).append("'").append('\n')
                .append("'").append(toCreate.getName()).append("'").append('\n')
                .append("'").append(toCreate.getTitle()).append("'").append('\n')
                .append("'").append(toCreate.getStatus()).append("'").append('\n')
                .append("'").append(toCreate.getExperimental()).append("'").append('\n')
                .append("'").append(toCreate.getPublisher()).append("'").append('\n')
                .append("'").append(toCreate.getDescription()).append("'").append('\n')
                .append("'").append(toCreate.getContent().toCode()).append("'").append('\n') //Todo - check if ok
                ;

        try (Statement statement = provider.getConnection().createStatement()) {
            statement.execute(command.toString());
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        return "ok";
    }

    //Based on https://github.com/hapifhir/hapi-fhir/blob/648d14c52cd3a4c79ad5a2562e16ec437aed1439/hapi-fhir-jpaserver-base/src/main/java/ca/uhn/fhir/jpa/provider/TerminologyUploaderProvider.java#L67
    public void uploadExternalCodesystem() {

    }
}
