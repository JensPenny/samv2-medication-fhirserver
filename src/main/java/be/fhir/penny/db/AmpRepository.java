package be.fhir.penny.db;

import org.jetbrains.annotations.NotNull;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Optional;

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

    public Collection<AMP_FAHMP> getAmpsByName(@NotNull final String name) {
        Collection<AMP_FAHMP> ampsByName = new ArrayList<>();
        try (Statement statement = provider.getConnection().createStatement()) {
            //todo input sanitation pls
            ResultSet result = statement.executeQuery("Select * from AMP_FAMHP " +
                    "where officialName like '%" + name + "%' " +      //Check if name contains - needs more info later
                    "and ifnull(validTo, date('now')) >= date('now')"); //Only valid amps
            while (result.next()) {
                AMP_FAHMP amp = ampFromResult(result);
                ampsByName.add(amp);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        return ampsByName;
    }

    @NotNull
    private static AMP_FAHMP ampFromResult(ResultSet result) throws SQLException {
        return new AMP_FAHMP(
                result.getString("code"),
                result.getString("officialName"),
                result.getBoolean("blackTriangle"),
                result.getInt("companyActorNumber"),
                result.getString("medicineType"),
                result.getString("nameEng"),
                result.getString("nameFr"),
                result.getString("nameGer"),
                result.getString("nameNl"),
                result.getString("prescriptionNameEng"),
                result.getString("prescriptionNameFr"),
                result.getString("prescriptionNameGer"),
                result.getString("prescriptionNameNl"),
                result.getString("status"),
                result.getDate("validFrom"),
                result.getDate("validTo"),
                result.getInt("vmpCode")
        );
    }

    public Optional<AMP_FAHMP> getCurrentAmpById(@NotNull final String ampCode) {
        try (Statement statement = provider.getConnection().createStatement()) {
            //todo input sanitation pls
            ResultSet result = statement.executeQuery("Select * from AMP_FAMHP " +
                    "where code = '" + ampCode + "' " +
                    "and ifnull(validTo, date('now')) >= date('now')"); //Only valid amps
            while (result.next()) {
                AMP_FAHMP amp = ampFromResult(result);
                return Optional.of(amp);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        return Optional.empty();
    }

    public record AMP_FAHMP(String ampCode,
                            String officialName,
                            boolean blacktriangle,
                            Integer companyActorNumber,
                            String medicineType,
                            String nameEng,
                            String nameFr,
                            String nameGer,
                            String nameNl,
                            String prescriptionNameEng,
                            String prescriptionNameFr,
                            String prescriptionNameGer,
                            String prescriptionNameNl,
                            String status,
                            Date validFrom,
                            Date validTo,
                            Integer vmpCode
                      ) {
    }
}
