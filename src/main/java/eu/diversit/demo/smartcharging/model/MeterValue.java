package eu.diversit.demo.smartcharging.model;

public record MeterValue(Integer value) {

    public static MeterValue of(Integer value) {
        return new MeterValue(value);
    }
}
