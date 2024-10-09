package eu.diversit.demo.smartcharging.model.json;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.assertj.vavr.api.VavrAssertions;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.InstanceOfAssertFactories.type;

class OcppJsonMessageTest {

    @Test
    public void should_decode_encode_callerror() {

        var json = new JsonArray("""
                [
                    4,
                    "unique-id",
                    "error code",
                    "error description",
                    {
                        "error": "details"
                    }
                ]""");

        // decode json array
        var message = OcppJsonMessage.fromJsonArray(json);

        VavrAssertions.assertThat(message).hasValueSatisfying(msg -> {
            assertThat(msg.messageType()).isEqualTo(OcppJsonMessage.MessageType.CALLERROR);
            assertThat(msg).asInstanceOf(type(OcppJsonMessage.CallError.class))
                    .satisfies(callError -> {
                        assertThat(callError.messageId().value()).isEqualTo("unique-id");
                        VavrAssertions.assertThat(callError.errorCode()).contains("error code");
                        VavrAssertions.assertThat(callError.errorDescription()).contains("error description");
                        VavrAssertions.assertThat(callError.errorDetails()).hasValueSatisfying(jsonObject -> {
                            assertThat(jsonObject).isEqualTo(JsonObject.of("error", "details"));
                        });
                    });

            // verify encoding to json array
            assertThat(msg.toJsonArray()).isEqualTo(json);
        });
    }

    @Test
    public void should_decode_encode_callresult() {

        var json = new JsonArray("""
                [
                    3,
                    "unique-id",
                    {
                        "some": "payload"
                    }
                ]""");

        // decode json array
        var message = OcppJsonMessage.fromJsonArray(json);

        VavrAssertions.assertThat(message).hasValueSatisfying(msg -> {
            assertThat(msg.messageType()).isEqualTo(OcppJsonMessage.MessageType.CALLRESULT);
            assertThat(msg).asInstanceOf(type(OcppJsonMessage.CallResult.class))
                    .satisfies(callResult -> {
                        assertThat(callResult.messageId().value()).isEqualTo("unique-id");
                        VavrAssertions.assertThat(callResult.payload()).hasValueSatisfying(jsonObject -> {
                            assertThat(jsonObject).isEqualTo(JsonObject.of("some", "payload"));
                        });
                    });

            // verify encoding to json array
            assertThat(msg.toJsonArray()).isEqualTo(json);
        });
    }

    @Test
    public void should_decode_encode_call() {

        var json = new JsonArray("""
                [
                    2,
                    "VFrf3TJ7oZdp37L5B3nSDHWPIDKwfZOxRIFh",
                    "BootNotification",
                    {
                        "chargePointVendor": "AVT-Company",
                        "chargePointModel": "AVT-Express",
                        "chargePointSerialNumber": "avt.001.13.1",
                        "chargeBoxSerialNumber": "avt.001.13.1.01",
                        "firmwareVersion": "0.9.87",
                        "iccid": "",
                        "imsi": "",
                        "meterType": "AVT NQC-ACDC",
                        "meterSerialNumber": "avt.001.13.1.01"
                    }
                ]""");

        // decode json array
        var message = OcppJsonMessage.fromJsonArray(json);

        VavrAssertions.assertThat(message).hasValueSatisfying(msg -> {
            assertThat(msg.messageType()).isEqualTo(OcppJsonMessage.MessageType.CALL);
            assertThat(msg).asInstanceOf(type(OcppJsonMessage.Call.class))
                    .satisfies(call -> {
                        assertThat(call.messageId().value()).isEqualTo("VFrf3TJ7oZdp37L5B3nSDHWPIDKwfZOxRIFh");
                        assertThat(call.action()).isEqualTo(Action.ByChargePoint.BOOTNOTIFICATION);
                        VavrAssertions.assertThat(call.payload()).hasValueSatisfying(jsonObject -> {
                            assertThat(jsonObject).isEqualTo(new JsonObject("""
                                    {
                                        "chargePointVendor": "AVT-Company",
                                        "chargePointModel": "AVT-Express",
                                        "chargePointSerialNumber": "avt.001.13.1",
                                        "chargeBoxSerialNumber": "avt.001.13.1.01",
                                        "firmwareVersion": "0.9.87",
                                        "iccid": "",
                                        "imsi": "",
                                        "meterType": "AVT NQC-ACDC",
                                        "meterSerialNumber": "avt.001.13.1.01"
                                    }"""));
                        });
                    });

            // verify encoding to json array
            assertThat(msg.toJsonArray()).isEqualTo(json);
        });
    }

    @Test
    public void decode_should_fail_unknown_message_type() {

        var json = new JsonArray("""
                [
                    0,
                    "VFrf3TJ7oZdp37L5B3nSDHWPIDKwfZOxRIFh",
                    "BootNotification",
                    {
                        "chargePointVendor": "AVT-Company",
                        "chargePointModel": "AVT-Express",
                        "chargePointSerialNumber": "avt.001.13.1",
                        "chargeBoxSerialNumber": "avt.001.13.1.01",
                        "firmwareVersion": "0.9.87",
                        "iccid": "",
                        "imsi": "",
                        "meterType": "AVT NQC-ACDC",
                        "meterSerialNumber": "avt.001.13.1.01"
                    }
                ]""");

        // decode json array
        var message = OcppJsonMessage.fromJsonArray(json);

        assertThat(message).isEmpty();
    }

    @Test
    public void decode_should_fail_unknown_action() {

        var json = new JsonArray("""
                [
                    0,
                    "VFrf3TJ7oZdp37L5B3nSDHWPIDKwfZOxRIFh",
                    "Unknown",
                    {}
                ]""");

        // decode json array
        var message = OcppJsonMessage.fromJsonArray(json);

        assertThat(message).isEmpty();
    }

    @Test
    public void decode_should_allow_absent_payload() {

        var json = new JsonArray("""
                [
                    2,
                    "VFrf3TJ7oZdp37L5B3nSDHWPIDKwfZOxRIFh",
                    "Heartbeat"
                ]""");

        // decode json array
        var message = OcppJsonMessage.fromJsonArray(json);

        VavrAssertions.assertThat(message).hasValueSatisfying(msg -> {
            assertThat(msg.messageType()).isEqualTo(OcppJsonMessage.MessageType.CALL);
            assertThat(msg.messageId().value()).isEqualTo("VFrf3TJ7oZdp37L5B3nSDHWPIDKwfZOxRIFh");
            assertThat(msg).asInstanceOf(type(OcppJsonMessage.Call.class))
                    .satisfies(call -> {
                        assertThat(call.action()).isEqualTo(Action.ByChargePoint.HEARTBEAT);
                        assertThat(call.payload()).isEmpty();
                    });

            // verify encoding to json array
            var expectedJson = new JsonArray("""
                    [
                        2,
                        "VFrf3TJ7oZdp37L5B3nSDHWPIDKwfZOxRIFh",
                        "Heartbeat",
                        null
                    ]""");
            assertThat(msg.toJsonArray()).isEqualTo(expectedJson);
        });
    }
}