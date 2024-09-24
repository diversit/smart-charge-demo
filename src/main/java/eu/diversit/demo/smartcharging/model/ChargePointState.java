package eu.diversit.demo.smartcharging.model;

import eu.diversit.demo.smartcharging.model.json.ocpp.BootNotification;
import eu.diversit.demo.smartcharging.model.json.ocpp.StatusNotification;
import io.vavr.collection.List;
import io.vavr.collection.Map;
import io.vavr.control.Option;

public record ChargePointState(
        Option<ChargeBoxId> chargeBoxId,
        Option<BootNotification> bootNotification,
        Map<Connector, List<StatusNotification>> connectorStatuses,
        List<Transaction> transactions
) {
}
