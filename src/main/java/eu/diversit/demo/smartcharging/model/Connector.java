package eu.diversit.demo.smartcharging.model;

public record Connector(Integer value) {
    public static Connector of(Integer connectorId) {
        return new Connector(connectorId);
    }
}
