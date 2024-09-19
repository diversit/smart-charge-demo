package eu.diversit.demo.smartcharging.model;

import eu.diversit.demo.smartcharging.model.json.ocpp.MeterValue;
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

    /**
     * Add given metervalues to the transaction with given id
     *
     * @param transactionId Transaction id
     * @param meterValue    List of metervalues
     * @return Updated connector status
     */
    public ConnectorStatus addMeterValues(Integer transactionId, java.util.List<MeterValue> meterValue) {
        var updatedTransactions = transactions.indexWhereOption(t -> t.id().equals(transactionId))
                .map(pos ->
                        transactions.update(pos, t -> t.addMeterValues(meterValue))
                )
                .getOrElse(transactions);

        return new ConnectorStatus(
                statuses,
                updatedTransactions
        );
    }
}