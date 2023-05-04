package be.fhir.penny.db;

import org.jetbrains.annotations.NotNull;
import org.sqlite.SQLiteConfig;
import org.sqlite.SQLiteOpenMode;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class SQLiteDbProvider implements DbProvider {
    private static final String SQLITE_LOCATION = "/opt/samtosql/6836.db";

    /**
     * We use a single sqlite connection, since it's just a file based handle either way.
     */
    @NotNull
    private final Connection connection;

    public SQLiteDbProvider() throws SQLException {
        SQLiteConfig config = new SQLiteConfig();
        config.setReadOnly(true);
        config.setSharedCache(true);
        config.setBusyTimeout(30*1000);
        config.setOpenMode(SQLiteOpenMode.READONLY);
        connection = DriverManager.getConnection("jdbc:sqlite:" + SQLITE_LOCATION, config.toProperties());
    }

    @Override
    public Connection getConnection() {
        return null;
    }
}
