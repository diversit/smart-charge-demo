package eu.diversit.demo.smartcharging.model.json;

import eu.diversit.demo.smartcharging.model.Connector;
import eu.diversit.demo.smartcharging.model.Transaction;
import eu.diversit.demo.smartcharging.model.json.ocpp.*;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class DefaultChargingProfiles {

    private DefaultChargingProfiles() {
    }

    /**
     * @param connector
     * @param activeTransaction
     * @param limit
     * @return A new SetChargingProfile for given connector and transaction with given limit.
     */
    public static SetChargingProfile createSetChargingProfile(Connector connector, Transaction activeTransaction, BigDecimal limit) {
        return SetChargingProfile.builder()
                .withConnectorId(connector.value())
                .withCsChargingProfiles(CsChargingProfiles.builder()
                        .withChargingProfileId(limit.intValue()) // use the limit as id for charging profile
                        .withTransactionId(activeTransaction.id().value())
                        .withStackLevel(100)
                        .withChargingProfilePurpose(CsChargingProfiles.ChargingProfilePurpose.TX_PROFILE) // for current active transaction
                        .withChargingProfileKind(CsChargingProfiles.ChargingProfileKind.ABSOLUTE)
                        // recurrency kind not required
                        // no valid from. Profile valid as soon as received by charge point
                        // no valid to. Profile valid until other profile is set
                        .withChargingSchedule(ChargingSchedule__2.builder()
                                // no duration. Profile valid as long as transaction lasts
                                // no startSchedule.
                                .withChargingRateUnit(ChargingSchedule__2.ChargingRateUnit.A) // set in Amps
                                .withChargingSchedulePeriod(java.util.List.of(
                                        ChargingSchedulePeriod__2.builder()
                                                .withStartPeriod(0) // start direct
                                                .withLimit(limit.setScale(1, RoundingMode.DOWN).doubleValue()) // at most 1 fraction
                                                .withNumberPhases(3) // default 3 phases
                                                .build()
                                ))
                                .withMinChargingRate(1.0) // minimal 1 Amps
                                .build()
                        )
                        .build()
                )
                .build();
    }

    /**
     * @param limit
     * @return A new SetChargingProfile for given connector and transaction with given limit.
     */
    public static ChargingProfile createChargingProfile(BigDecimal limit) {
        return ChargingProfile.builder()
                .withChargingProfileId(limit.intValue()) // use the limit as id for charging profile
                .withStackLevel(100)
                .withChargingProfilePurpose(ChargingProfile.ChargingProfilePurpose.TX_PROFILE) // for current active transaction
                .withChargingProfileKind(ChargingProfile.ChargingProfileKind.ABSOLUTE)
                // recurrency kind not required
                // no valid from. Profile valid as soon as received by charge point
                // no valid to. Profile valid until other profile is set
                .withChargingSchedule(ChargingSchedule__1.builder()
                        // no duration. Profile valid as long as transaction lasts
                        // no startSchedule.
                        .withChargingRateUnit(ChargingSchedule__1.ChargingRateUnit.A) // set in Amps
                        .withChargingSchedulePeriod(java.util.List.of(
                                ChargingSchedulePeriod__2.builder()
                                        .withStartPeriod(0) // start direct
                                        .withLimit(limit.setScale(1, RoundingMode.DOWN).doubleValue()) // at most 1 fraction
                                        .withNumberPhases(3) // default 3 phases
                                        .build()
                        ))
                        .withMinChargingRate(1.0) // minimal 1 Amps
                        .build()
                )
                .build();
    }
}
