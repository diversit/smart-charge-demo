package eu.diversit.demo.smartcharging;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class TransactionIdProvider {

    Integer currentTxId = 0;

    public Integer nextTransactionId() {
        currentTxId++;
        return currentTxId;
    }
}
