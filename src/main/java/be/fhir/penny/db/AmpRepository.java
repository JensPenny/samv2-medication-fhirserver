package be.fhir.penny.db;

import org.jetbrains.annotations.NotNull;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.*;

/**
 * todo: log timings
 * Class that will fetch the AMP items from the (SQLite) repository
 */
public final class AmpRepository {

    @NotNull
    private final DbProvider provider;

    //A selection of basic statements needed to fill a Medicinal Product Definition
    private final List<String> ampcodeStatements = Arrays.asList(
            "select * from AMP_FAMHP ampf where ampf.validTo is null and ampf.code = ?",        //? = amp-code
            "select * from AMPP_FAMHP amppf where amppf.ampCode = ? and amppf.validTo is null", //? = amp-code
            "select ampp_atc.ctiExtended, ATC.* from AMPP_TO_ATC ampp_atc " +
                    "JOIN ATC on ATC.code = ampp_atc.code where ampp_atc.ctiExtended = ? and ampp_atc.validTo is null", //? = cti-extended
            "select * from AMPC_BCPI ampcb where ampcb.ampCode = ? and ampcb.validTo is null",     //? = amp-code
            "select amp_route.ampCode from AMPC_TO_ROA amp_route " +
                    "JOIN STDROA roa on roa.standard = 'SNOMED_CT' and roa.roaCode = amp_route.roaCode" +
                    "where amp_route.ampCode = ? and amp_route.validTo is null",    //? = amp-code
            "select * from CMRCL comm where comm.ctiExtended = ? and ifnull(comm.validTo, date('now')) >= date('now')",
            "select * from SPPROB sp where sp.ctiExtended = ? and ifnull(sp.validTo, date('now')) >= date('now')"
    );

    //Specialized queries to fetch the initial data required for the top calls
    private static final String ampByName = "Select * from AMP_FAMHP where officialName like ? and ifnull(validTo, date('now')) >= date('now')";

    public AmpRepository(@NotNull final DbProvider provider) {
        this.provider = provider;
    }

    public Collection<AMP_FAHMP> getAmpsByName(@NotNull final String name) {
        Collection<AMP_FAHMP> ampsByName = new ArrayList<>();
        try (PreparedStatement statement = provider.getConnection().prepareStatement(ampByName)) {
            //todo input sanitation pls
            statement.setString(0, name);
            ResultSet result = statement.executeQuery();
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
