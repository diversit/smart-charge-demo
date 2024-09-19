package eu.diversit.demo.smartcharging.model;

import io.vavr.collection.List;
import io.vavr.control.Option;

import java.time.ZonedDateTime;

public record Transaction(Integer id,
                          IdTag idTag,
                          MeterValue meterStart,
                          ZonedDateTime startTimestamp,
                          List<List<eu.diversit.demo.smartcharging.model.json.ocpp.MeterValue>> meterValues,
                          Option<MeterValue> meterStop,
                          Option<ZonedDateTime> stopTimestamp) {

    public static Transaction create(Integer id,
                                     IdTag idTag,
                                     MeterValue meterStart,
                                     ZonedDateTime startTimestamp) {
        return new Transaction(id, idTag, meterStart, startTimestamp, List.empty(), Option.none(), Option.none());
    }

    public Transaction addMeterValues(java.util.List<eu.diversit.demo.smartcharging.model.json.ocpp.MeterValue> newMeterValues) {
        return new Transaction(
                id,
                idTag,
                meterStart,
                startTimestamp,
                meterValues.append(List.ofAll(newMeterValues)),
                meterStop,
                stopTimestamp
        );
    }
}