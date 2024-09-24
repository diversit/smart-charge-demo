package eu.diversit.demo.smartcharging.model;

import eu.diversit.demo.smartcharging.model.json.ocpp.SampledValue;
import eu.diversit.demo.smartcharging.model.json.ocpp.StopTransaction;
import io.vavr.collection.List;
import io.vavr.control.Option;

import java.time.ZonedDateTime;
import java.util.Objects;

import static io.vavr.control.Option.some;

public record Transaction(TransactionId id,
                          Connector connector,
                          IdTag idTag,
                          MeterValue meterStart,
                          ZonedDateTime startTimestamp,
                          // each measurement contains a list of meter values
                          // and each meter value may have multiple sample values for different measurands
                          List<List<eu.diversit.demo.smartcharging.model.json.ocpp.MeterValue>> meterValues,
                          Option<MeterValue> meterStop,
                          Option<ZonedDateTime> stopTimestamp,
                          Option<StopTransaction.Reason> stopReason) {

    public static Transaction create(TransactionId id,
                                     Connector connector,
                                     IdTag idTag,
                                     MeterValue meterStart,
                                     ZonedDateTime startTimestamp) {
        return new Transaction(id, connector, idTag, meterStart, startTimestamp, List.empty(), Option.none(), Option.none(), Option.none());
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (other == null || getClass() != other.getClass()) return false;
        Transaction that = (Transaction) other;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    public Transaction addMeterValues(List<eu.diversit.demo.smartcharging.model.json.ocpp.MeterValue> newMeterValues) {
        return new Transaction(
                id,
                connector,
                idTag,
                meterStart,
                startTimestamp,
                meterValues.prepend(newMeterValues), // latest on top
                meterStop,
                stopTimestamp,
                stopReason
        );
    }

    /**
     * Stop the transaction
     *
     * @param meterValue
     * @param timestamp
     * @param reason
     * @return
     */
    public Transaction stop(MeterValue meterValue, ZonedDateTime timestamp, Option<StopTransaction.Reason> reason) {
        return new Transaction(
                id,
                connector,
                idTag,
                meterStart,
                startTimestamp,
                meterValues,
                some(meterValue),
                some(timestamp),
                reason
        );
    }

    /**
     * Returns the latest meter values either from the meterStop
     * or from the last received meter value
     * or None when no meter values has been received yet.
     *
     * @return The latest meter value
     */
    public Option<MeterValue> latestMeterValue() {

        return meterStop.orElse(() -> meterValues
                .headOption()
                .flatMap(meterValuesList -> meterValuesList
                        .headOption()
                        .map(mv ->
                                List.ofAll(mv.getSampledValue())
                                        .filter(sv -> Option.ofOptional(sv.getMeasurand()).exists(m -> m == SampledValue.Measurand.ENERGY_ACTIVE_IMPORT_REGISTER))
                                        .head()
                        ).map(sv -> new MeterValue(Integer.parseInt(sv.getValue())))
                )
        );
    }


    /**
     * Gets the latest meter values either from the meterStop or from the last received meter value.
     * Returns difference between latest meter value and meterStart value.
     *
     * @return The total energy charged in this transaction
     */
    public Integer totalCharged() {

        return latestMeterValue()
                .map(mv -> mv.value() - meterStart.value())
                .getOrElse(0);
    }
}