package eu.diversit.demo.smartcharging.rest;

import eu.diversit.demo.smartcharging.model.ChargeBoxId;
import eu.diversit.demo.smartcharging.model.ChargePoint;
import eu.diversit.demo.smartcharging.model.Connector;
import eu.diversit.demo.smartcharging.model.MeterValue;
import eu.diversit.demo.smartcharging.model.json.ocpp.StatusNotification;
import io.vavr.Tuple;
import io.vavr.collection.HashMap;
import io.vavr.collection.List;
import io.vavr.collection.Map;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import java.util.function.Function;

@Path("/api")
public class RestApi {

    @Inject
    private ChargePoint chargePoint;

    @GET
    @Path("/chargepoint")
    public ChargePointStateDto getChargePointState() {

        var state = chargePoint.getState();
        var transactions = state.transactions();

        var connectors = state.connectorStatuses()
                .filter((conn, _) -> conn.value() > 0) // exclude connector 0
                .map((conn, statuses) -> {
                    var lastestStatus = statuses.head();

                    Map<String, Object> attributes = HashMap.empty();

                    // find latest transaction for given connector
                    attributes = attributes.merge(
                            transactions.find(t -> t.connector().equals(conn))
                                    .map(t ->
                                            HashMap.<String, Object>of(
                                                    "txId", t.id().value(),
                                                    "idTag", t.idTag().value(),
                                                    "meterStart", t.meterStart().value(),
                                                    "lastMeterValue", t.latestMeterValue().map(MeterValue::value).getOrNull(),
                                                    "totalCharged", t.totalCharged(),
                                                    "txStartTimestamp", t.startTimestamp().toInstant().toEpochMilli(),
                                                    "txStopTimestamp", t.stopTimestamp().map(zdt -> zdt.toInstant().toEpochMilli()).getOrNull()
                                            )).getOrElse(HashMap.empty()));

                    attributes = attributes.put("timestamp", lastestStatus.getTimestamp().map(zdt -> zdt.toInstant().toEpochMilli()).orElse(null));

                    return Tuple.of(conn.value(), new ConnectorStatusDto(
                            lastestStatus.getStatus().toString(),
                            attributes
                    ));
                });

        return new ChargePointStateDto(
                state.chargeBoxId().map(ChargeBoxId::value).getOrElse("Not connected"),
                getConnector0StatusValue(state.connectorStatuses(), sn -> sn.getStatus().value()),
                getConnector0StatusValue(state.connectorStatuses(), sn -> sn.getTimestamp().map(zdt -> zdt.toInstant().toEpochMilli()).orElse(null)),
                connectors
        );
    }

    private <T> T getConnector0StatusValue(Map<Connector, List<StatusNotification>> connectorStatuses,
                                           Function<StatusNotification, T> getValue) {
        return connectorStatuses.get(Connector.of(0))
                .flatMap(statuses -> statuses.headOption())
                .map(getValue)
                .getOrNull();
    }

    public record ChargePointStateDto(
            String chargePointId,
            String status,
            Long timestamp,
            Map<Integer, ConnectorStatusDto> connectors
    ) {
    }

    public record ConnectorStatusDto(
            String status,
            Map<String, Object> attributes
    ) {
    }
}
