package be.fhir.penny.db;

import org.jetbrains.annotations.NotNull;
import org.sqlite.SQLiteConfig;
import org.sqlite.SQLiteOpenMode;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class SQLiteDbProvider implements DbProvider {

    /**
     * We use a single sqlite connection, since it's just a file based handle either way.
     */
    @NotNull
    private final Connection connection;

    public SQLiteDbProvider(String sqliteFile,
                            SQLiteOpenMode mode) throws SQLException {
        SQLiteConfig config = new SQLiteConfig();
        config.setReadOnly(mode == SQLiteOpenMode.READONLY);
        config.setSharedCache(true);
        config.setBusyTimeout(30*1000);
        config.setOpenMode(mode);
        connection = DriverManager.getConnection("jdbc:sqlite:" + sqliteFile, config.toProperties());
    }

    @NotNull
    @Override
    public Connection getConnection() {
        return connection;
    }
}
