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
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import org.jboss.resteasy.reactive.RestPath;
import org.jboss.resteasy.reactive.RestResponse;

import java.math.BigDecimal;
import java.util.function.Function;

@Path("/api")
public class RestApi {

    @Inject
    private ChargePoint chargePoint;

    /**
     * Set charging limit on the current transaction.
     * If the new limit is 0, the transaction will be stopped.
     * If no transaction is started yet, a new transaction is started.
     */
    @PUT
    @Path("/chargepoint/{connectorNr}/chargeLimit")
    public RestResponse<Object> setChargeLimit(@RestPath Integer connectorNr,
                                               BigDecimal limit) {
        if (connectorNr == 0) {
            return RestResponse.ResponseBuilder.create(RestResponse.Status.BAD_REQUEST)
                    .entity("Connector 0 not allowed")
                    .build();
        }

        chargePoint.setChargingLimit(Connector.of(connectorNr), limit);
        return RestResponse.ok();
    }

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
                                                    "lastMeterValue", t.latestMeterValue().map(MeterValue::value).getOrElse(0),
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
