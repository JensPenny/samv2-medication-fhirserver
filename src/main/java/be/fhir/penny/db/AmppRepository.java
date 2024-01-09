package be.fhir.penny.db;

import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Date;

public class AmppRepository {
    public Collection<AMPP_FAHMP> getAmppsByAmp(@NotNull final String ampCode) {
        throw new IllegalStateException("Not yet implemented");
    }

    public record AMPP_FAHMP(
            String ctiExtended,
            String ampCode,
            String atcCodesCsv,
            String deliveryModusCode,
            String deliveryModusSpecificationCode,
            String authorizationNumber,
            boolean isOrphaned,
            String leafletLinkNl,
            String leafletLinkFr,
            String leafletLinkEng,
            String leafletLinkGer,
            String spcLinkNl,
            String spcLinkFr, String spcLinkEng, String spcLinkGer,
            String rmaPatientLinkNl,
            String rmaPatientLinkFr,
            String rmaPatientLinkEng,
            String rmaPatientLinkGer,
            String rmaProfessionalLinkNl,
            String rmaProfessionalLinkFr,
            String rmaProfessionalLinkEng,
            String rmaProfessionalLinkGer,
            String parallelCircuit,
            String parallelDistributor,
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
            String rmaKeyMessagesGer,
            String rmaKeyMessagesEng,
            Date validFrom,
            Date validTo
    ) {
    }
}
