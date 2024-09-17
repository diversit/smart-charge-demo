package eu.diversit.demo.smartcharging.model;

import eu.diversit.demo.smartcharging.model.json.ocpp.BootNotification;
import io.vavr.collection.Map;

public record ChargePointState(
        ChargeBoxId chargeBoxId,
        BootNotification bootNotification,
        Map<Integer, ConnectorStatus> connectorStatuses
) {
}
