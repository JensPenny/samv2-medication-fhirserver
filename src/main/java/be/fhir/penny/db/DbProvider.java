package be.fhir.penny.db;

import java.sql.Connection;

public interface DbProvider {

    Connection getConnection();
}
