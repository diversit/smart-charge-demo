package eu.diversit.demo.smartcharging.rest;

import eu.diversit.demo.smartcharging.model.ChargeBoxId;
import eu.diversit.demo.smartcharging.model.ChargePoint;
import eu.diversit.demo.smartcharging.model.MeterValue;
import io.vavr.Tuple;
import io.vavr.collection.Map;
import io.vavr.control.Option;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import java.time.ZonedDateTime;

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
                .map((conn, statuses) -> {
                    var connector = "connector" + conn.value();
                    var lastestStatus = statuses.head();

                    // find latest transaction for given connector
                    var lastConnectorTransaction = transactions.find(t -> t.connector().equals(conn))
                            .map(t -> new TransactionDto(
                                    t.id().value(),
                                    t.idTag().value(),
                                    t.meterStart().value(),
                                    t.latestMeterValue().map(MeterValue::value).getOrNull(),
                                    t.totalCharged(),
                                    t.startTimestamp(),
                                    t.stopTimestamp().getOrNull()
                            ));

                    return Tuple.of(connector, new ConnectorStatusDto(
                            lastestStatus.getStatus().toString(),
                            lastestStatus.getTimestamp().orElse(null),
                            lastConnectorTransaction
                    ));
                });

        var dto = new ChargePointStateDto(
                state.chargeBoxId().map(ChargeBoxId::value).getOrElse("Not connected"),
                connectors
        );

        return dto;
    }

    public record ChargePointStateDto(
            String chargePointId,
            Map<String, ConnectorStatusDto> connectors
    ) {
    }

    public record ConnectorStatusDto(
            String status,
            ZonedDateTime timestamp,
            Option<TransactionDto> transaction
    ) {
    }

    public record TransactionDto(
            Integer id,
            String idTag,
            Integer meterStart,
            Integer lastMeterValue,
            Integer totalCharged,
            ZonedDateTime startedAt,
            ZonedDateTime stoppedAt
    ) {
    }
}
