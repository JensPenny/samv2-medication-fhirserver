package be.fhir.penny.db;

import org.jetbrains.annotations.NotNull;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;

/**
 * todo: log timings
 * Class that will fetch the AMP items from the (SQLite) repository
 */
public final class AmpRepository {

    @NotNull
    private final DbProvider provider;

    public AmpRepository(@NotNull final DbProvider provider) {
        this.provider = provider;
    }

    public Collection<AMP> getAmpsByName(@NotNull final String name) {
        Collection<AMP> ampsByName = new ArrayList<>();
        try (Statement statement = provider.getConnection().createStatement()) {
            //todo input sanitation pls
            ResultSet result = statement.executeQuery("Select * from AMP_FAMHP " +
                    "where officialName like '%" + name + "%' " +      //Check if name contains - needs more info later
                    "and ifnull(validTo, date('now')) >= date('now')"); //Only valid amps
            while (result.next()) {
                AMP amp = new AMP(result.getString("code"),
                        result.getString("officialName"));
                ampsByName.add(amp);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        return ampsByName;
    }

    public record AMP(String ampCode, String officialName) {
    }
}
