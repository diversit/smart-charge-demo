package eu.diversit.demo.smartcharging.model;

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

    public Transaction addMeterValues(java.util.List<eu.diversit.demo.smartcharging.model.json.ocpp.MeterValue> newMeterValues) {
        return new Transaction(
                id,
                connector,
                idTag,
                meterStart,
                startTimestamp,
                meterValues.append(List.ofAll(newMeterValues)),
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
}