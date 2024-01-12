package be.fhir.penny.db;

import org.hl7.fhir.r4b.model.MedicinalProductDefinition;
import org.jetbrains.annotations.NotNull;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Collections;

public class AmpQryRepository {

    private static final String amp_med_definition_sql = "--This is an example query that attempts to extract all data needed for a fhir product definition\n" +
            "select ampf.code, \n" +
            " ampf.\"officialName\", ampf.\"prescriptionNameNl\", ampf.\"prescriptionNameFr\", ampf.\"prescriptionNameGer\", ampf.\"prescriptionNameEng\", \n" +
            " ampf.\"validFrom\", ampf.\"validTo\", \n" +
            " roa.standard, roa.code, \n" +
            " ampf.\"blackTriangle\", --additionalmonitoring\n" +
            "  ampp_atc.code, atc.description, --Classificationstatus\n" +
            "  --marketingstatus = supplyproblem en commercialisatie\n" +
            "  comm.\"validFrom\" as comm_from, comm.\"validTo\" as comm_to, --comm.\"endOfCommercializationNl\", comm.\"additionalInformationNl\",\n" +
            "  sp.\"validFrom\" as sp_from, sp.\"validTo\" as sp_to, sp.\"expectedEndDate\" as sp_end, -- sp.\"impactNl\", sp.\"reasonNl\"\n" +
            "  ampf.\"companyActorNumber\" as companyref, -- company ref as contact\n" +
            "  ampcb.\"containsAlcohol\", ampcb.crushable, ampcb.\"modifiedReleaseType\", ampcb.dividable, ampcb.scored, ampcb.\"sugarFree\"\n" +
            " from AMP_FAMHP ampf\n" +
            " left join AMPP_FAMHP amppf on amppf.\"ampCode\" = ampf.code and amppf.\"validTo\" is null\n" +
            " left join AMPP_TO_ATC ampp_atc on ampp_atc.\"ctiExtended\" = amppf.\"ctiExtended\" and ampp_atc.\"validTo\" is null\n" +
            " left join ATC atc on atc.code = ampp_atc.code\n" +
            " left join AMPC_BCPI ampcb on ampcb.\"ampCode\" = ampf.code and ampcb.\"validTo\" is null\n" +
            " left join AMPC_TO_ROA amp_route on amp_route.\"ampCode\" = ampf.code and amp_route.\"validTo\" is null\n" +
            " left join STDROA roa on roa.standard = 'SNOMED_CT' and roa.\"roaCode\" = amp_route.\"roaCode\"\n" +
            " -- legalStatusOfSupply = voorschriftplicht? not included atm\n" +
            " left join CMRCL comm on comm.\"ctiExtended\" = amppf.\"ctiExtended\" and ifnull(comm.\"validTo\", date('now')) >= date('now')\n" +
            " left join SPPROB sp on sp.\"ctiExtended\" = amppf.\"ctiExtended\" and ifnull(sp.\"validTo\", date('now')) >= date('now') \n" +
            " where ampf.\"validTo\" is null\n";

    @NotNull
    private final DbProvider provider;

    public AmpQryRepository(@NotNull final DbProvider provider) {
        this.provider = provider;
    }

    public Collection<MedicinalProductDefinition> getAmpsByName(@NotNull final String name) {
        try (PreparedStatement stat = provider.getConnection().prepareStatement(amp_med_definition_sql)){
            ResultSet res = stat.executeQuery();
            //Parse results
            //Translate to MedicinalProductDefinition
        } catch (SQLException ex) {
            throw new RuntimeException("Error executing query: ", ex);
        }
        return Collections.emptyList();
    }
}
