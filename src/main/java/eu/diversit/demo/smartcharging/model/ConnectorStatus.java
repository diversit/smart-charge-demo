package eu.diversit.demo.smartcharging.model;

import eu.diversit.demo.smartcharging.model.json.ocpp.StatusNotification;
import io.vavr.collection.List;

public record ConnectorStatus(List<StatusNotification> statuses, List<Transaction> transactions) {
    public static ConnectorStatus init(StatusNotification sn) {
        return new ConnectorStatus(List.of(sn), List.empty());
    }

    public ConnectorStatus addStatus(StatusNotification newStatus) {
        return new ConnectorStatus(statuses.prepend(newStatus), transactions);
    }

    public ConnectorStatus addTransaction(Transaction newTransaction) {
        return new ConnectorStatus(statuses, transactions.prepend(newTransaction));
    }
}