package eu.diversit.demo.smartcharging.model;

public record TransactionId(Integer value) {

    public static TransactionId of(Integer id) {
        return new TransactionId(id);
    }
}
