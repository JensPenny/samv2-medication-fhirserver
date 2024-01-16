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


    //Generic basic queries to fetch information to fill in data models
    private static final String ampFahmpStatement = "select * from AMP_FAMHP ampf where ampf.validTo is null and ampf.code = ?";
    private static final String amppFamhpStatement = "select * from AMPP_FAMHP amppf where amppf.ampCode = ? and amppf.validTo is null";
    private static final String atcStatement = "select ampp_atc.ctiExtended, ATC.* from AMPP_TO_ATC ampp_atc " +
            "JOIN ATC on ATC.code = ampp_atc.code where ampp_atc.ctiExtended = ? and ampp_atc.validTo is null";
    private static final String ampcBcpiStatement = "select * from AMPC_BCPI ampcb where ampcb.ampCode = ? and ampcb.validTo is null";
    private static final String routeStatement =             "select amp_route.ampCode, roa.*, r.nameNl, r.nameFr, r.nameGer, r.nameEng from AMPC_TO_ROA amp_route " +
            "JOIN STDROA roa on roa.standard = 'SNOMED_CT' and roa.roaCode = amp_route.roaCode" +
            "JOIN ROA r on r.code = roa.roaCode " +
            "WHERE amp_route.validTo is null --and amp_route.ampCode = ?";
    private static final String cmrclStatement = "select * from CMRCL comm where comm.ctiExtended = ? and ifnull(comm.validTo, date('now')) >= date('now')";
    private static final String spprobStatement = "select * from SPPROB sp where sp.ctiExtended = ? and ifnull(sp.validTo, date('now')) >= date('now')";

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

    @NotNull
    private static AMP_FAHMP ampFromResult(ResultSet result) throws SQLException {
        return new AMP_FAHMP(
                result.getInt("id"),
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

    @NotNull
    private static AMPP_FAMHP amppFromResult(ResultSet result) throws SQLException {
        return new AMPP_FAMHP(
                result.getInt("id"),
                result.getString("ctiExtended"),
                result.getString("ampCode"),
                result.getString("deliveryModusCode"),
                result.getString("deliveryModusSpecificationCode"),
                result.getString("authorizationNumber"),
                result.getBoolean("orphan"),
                result.getString("leafletLinkNl"),
                result.getString("leafletLinkFr"),
                result.getString("leafletLinkEng"),
                result.getString("leafletLinkGer"),
                result.getString("spcLinkNl"),
                result.getString("spcLinkFr"),
                result.getString("spcLinkEng"),
                result.getString("spcLinkGer"),
                result.getString("rmaPatientLinkNl"),
                result.getString("rmaPatientLinkFr"),
                result.getString("rmaPatientLinkEng"),
                result.getString("rmaPatientLinkGer"),
                result.getString("rmaProfessionalLinkNl"),
                result.getString("rmaProfessionalLinkFr"),
                result.getString("rmaProfessionalLinkEng"),
                result.getString("rmaProfessionalLinkGer"),
                result.getBoolean("parallelCircuit"),
                result.getString("parallelDistributor"),
                result.getString("packMultiplier"),
                result.getString("packAmount"),
                result.getString("packAmountUnit"),
                result.getString("packDisplayValue"),
                result.getString("gtin"),
                result.getString("status"),
                result.getString("fmdProductCode"),
                result.getBoolean("fmdInScope"),
                result.getBoolean("antiTamperingDevicePresent"),
                result.getString("prescriptionNameNl"),
                result.getString("prescriptionNameFr"),
                result.getString("prescriptionNameGer"),
                result.getString("prescriptionNameEng"),
                result.getString("rmaKeyMessagesNl"),
                result.getString("rmaKeyMessagesFr"),
                result.getString("rmaKeyMessagesEng"),
                result.getString("rmaKeyMessagesGer"),
                result.getDate("validFrom"),
                result.getDate("validTo")
        );
    }

    @NotNull
    private static AMPP_TO_ATC atcFromResult(ResultSet result) throws SQLException {
        return new AMPP_TO_ATC(
                result.getString("ctiExtended"),
                result.getInt("id"),
                result.getString("code"),
                result.getString("description")
        );
    }

    @NotNull
    private static AMPC_BCPI ampcFromResult(ResultSet result) throws SQLException {
        return new AMPC_BCPI(
                result.getInt("id"),
                result.getString("ampCode"),
                result.getInt("sequenceNumber"),
                result.getInt("vmpcCode"),
                result.getString("dividable"),
                result.getString("scored"),
                result.getString("crushable"),
                result.getString("containsAlcohol"),
                result.getBoolean("sugarFree"),
                result.getString("modifiedReleaseType"),
                result.getInt("specificDrugDevice"),
                result.getString("dimensions"),
                result.getString("nameNl"),
                result.getString("nameFr"),
                result.getString("nameEng"),
                result.getString("nameGer"),
                result.getString("noteNl"),
                result.getString("noteFr"),
                result.getString("noteEng"),
                result.getString("noteGer"),
                result.getString("concentration"),
                result.getString("osmoticConcentration"),
                result.getString("caloricValue"),
                result.getDate("validFrom"),
                result.getDate("validTo")
        );
    }

    @NotNull
    private static AMPC_TO_ROA roaFromResult(ResultSet result) throws SQLException {
        return new AMPC_TO_ROA(
                result.getString("ampCode"),
                result.getInt("id"),
                result.getString("standard"),
                result.getString("code"),
                result.getInt("roaCode"),
                result.getString("nameNl"),
                result.getString("nameFr"),
                result.getString("nameGer"),
                result.getString("nameEng")
        );
    }

    @NotNull
    private static CMRCL cmrclFromResult(ResultSet result) throws SQLException {
        return new CMRCL(
                result.getInt("id"),
                result.getString("ctiExtended"),
                result.getString("endOfCommercializationNl"),
                result.getString("endOfCommercializationFr"),
                result.getString("endOfCommercializationGer"),
                result.getString("endOfCommercializationEng"),
                result.getString("reasonEndOfCommercializationNl"),
                result.getString("reasonEndOfCommercializationFr"),
                result.getString("reasonEndOfCommercializationGer"),
                result.getString("reasonEndOfCommercializationEng"),
                result.getString("additionalInformationNl"),
                result.getString("additionalInformationFr"),
                result.getString("additionalInformationGer"),
                result.getString("additionalInformationEng"),
                result.getDate("validFrom"),
                result.getDate("validTo")
        );
    }

    @NotNull
    private static SPPROB spprobFromResult(ResultSet resultSet) throws SQLException {
        return new SPPROB(
                resultSet.getInt("id"),
                resultSet.getString("ctiExtended"),
                resultSet.getDate("expectedEndDate"),
                resultSet.getString("reportedBy"),
                resultSet.getString("reportedOn"),
                resultSet.getString("contactName"),
                resultSet.getString("contactMail"),
                resultSet.getString("contactCompany"),
                resultSet.getString("contactPhone"),
                resultSet.getString("reasonNl"),
                resultSet.getString("reasonFr"),
                resultSet.getString("reasonGer"),
                resultSet.getString("reasonEng"),
                resultSet.getString("additionalInformationNl"),
                resultSet.getString("additionalInformationFr"),
                resultSet.getString("additionalInformationGer"),
                resultSet.getString("additionalInformationEng"),
                resultSet.getString("impactNl"),
                resultSet.getString("impactFr"),
                resultSet.getString("impactGer"),
                resultSet.getString("impactEng"),
                resultSet.getBoolean("limitedAvailability"),
                resultSet.getDate("validFrom"),
                resultSet.getDate("validTo")
        );
    }

    public record AMP_FAHMP(
            int id,
            String ampCode,
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

    public record AMPP_FAMHP(
            int id,
            String ctiExtended,
            String ampCode,
            String deliveryModusCode,
            String deliveryModusSpecificationCode,
            String authorizationNumber,
            boolean orphan,
            String leafletLinkNl,
            String leafletLinkFr,
            String leafletLinkEng,
            String leafletLinkGer,
            String spcLinkNl,
            String spcLinkFr,
            String spcLinkEng,
            String spcLinkGer,
            String rmaPatientLinkNl,
            String rmaPatientLinkFr,
            String rmaPatientLinkEng,
            String rmaPatientLinkGer,
            String rmaProfessionalLinkNl,
            String rmaProfessionalLinkFr,
            String rmaProfessionalLinkEng,
            String rmaProfessionalLinkGer,
            boolean parallelCircuit,
            String parallelDistributor,
            String packMultiplier,
            String packAmount,
            String packAmountUnit,
            String packDisplayValue,
            String gtin,
            String status,
            String fmdProductCode,
            boolean fmdInScope,
            boolean antiTamperingDevicePresent,
            String prescriptionNameNl,
            String prescriptionNameFr,
            String prescriptionNameGer,
            String prescriptionNameEng,
            String rmaKeyMessagesNl,
            String rmaKeyMessagesFr,
            String rmaKeyMessagesEng,
            String rmaKeyMessagesGer,
            Date validFrom,
            Date validTo
    ) {
    }

    public record AMPP_TO_ATC(
            String ctiExtended,
            int id,
            String code, //ATC Code
            String description
    ) {
    }

    public record AMPC_BCPI(
            int id,
            String ampCode,
            int sequenceNumber,
            int vmpcCode,
            String dividable,
            String scored,
            String crushable,
            String containsAlcohol,
            boolean sugarFree,
            String modifiedReleaseType,
            int specificDrugDevice,
            String dimensions,
            String nameNl,
            String nameFr,
            String nameEng,
            String nameGer,
            String noteNl,
            String noteFr,
            String noteEng,
            String noteGer,
            String concentration,
            String osmoticConcentration,
            String caloricValue,
            Date validFrom,
            Date validTo
    ) {
    }

    public record AMPC_TO_ROA(
            String ampCode,
            int id,
            String standard,
            String code,
            int roaCode,
            String nameNl,
            String nameFr,
            String nameGer,
            String nameEng
    ) {
    }

    public record CMRCL(
            int id,
            String ctiExtended,
            String endOfCommercializationNl,
            String endOfCommercializationFr,
            String endOfCommercializationGer,
            String endOfCommercializationEng,
            String reasonEndOfCommercializationNl,
            String reasonEndOfCommercializationFr,
            String reasonEndOfCommercializationGer,
            String reasonEndOfCommercializationEng,
            String additionalInformationNl,
            String additionalInformationFr,
            String additionalInformationGer,
            String additionalInformationEng,
            Date validFrom,
            Date validTo
    ) {
    }

    public record SPPROB(
            int id,
            String ctiExtended,
            Date expectedEndDate,
            String reportedBy,
            String reportedOn,
            String contactName,
            String contactMail,
            String contactCompany,
            String contactPhone,
            String reasonNl,
            String reasonFr,
            String reasonGer,
            String reasonEng,
            String additionalInformationNl,
            String additionalInformationFr,
            String additionalInformationGer,
            String additionalInformationEng,
            String impactNl,
            String impactFr,
            String impactGer,
            String impactEng,
            boolean limitedAvailability,
            Date validFrom,
            Date validTo
    ) {
    }
}
