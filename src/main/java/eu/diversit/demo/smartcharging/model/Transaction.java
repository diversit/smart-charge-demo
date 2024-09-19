package eu.diversit.demo.smartcharging.model;

import io.vavr.collection.List;
import io.vavr.control.Option;

import java.time.ZonedDateTime;

public record Transaction(Integer id,
                          IdTag idTag,
                          MeterValue meterStart,
                          ZonedDateTime startTimestamp,
                          List<eu.diversit.demo.smartcharging.model.json.ocpp.MeterValue> meterValues,
                          Option<MeterValue> meterStop) {

    public static Transaction create(Integer id,
                                     IdTag idTag,
                                     MeterValue meterStart,
                                     ZonedDateTime startTimestamp) {
        return new Transaction(id, idTag, meterStart, startTimestamp, List.empty(), Option.none());
    }
}