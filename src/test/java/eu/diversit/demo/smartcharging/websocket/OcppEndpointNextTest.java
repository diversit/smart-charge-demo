package eu.diversit.demo.smartcharging.websocket;

import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.websockets.next.BasicWebSocketConnector;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

@QuarkusTest
class OcppEndpointNextTest {

    private static final String BOOT_NOTIFICATION = """
            [2,"UNIQUEID","BootNotification",{"chargePointVendor":"ECOTAP","chargePointModel":"DUO2","chargePointSerialNumber":"123","chargeBoxSerialNumber":"11752628","firmwareVersion":"4.3x.32R.16","iccid":"8931081721117385147","imsi":"204080822156943","meterSerialNumber":"21930052"}]""";
    private static final String BOOT_NOTIFICATION_RESPONSE = """
            \\[3,"UNIQUEID",\\{"status":"Accepted","currentTime":"([0-9-:.TZ]+)","interval":600}]""";
    private static final String HEARTBEAT_RESPONSE = """
            \\[3,"VFrf3TJ7oZdp37L5B3nSDHWPIDKwfZOxRIFh",\\{"currentTime":"([0-9-:.TZ]+)"}]""";

    @Inject
    BasicWebSocketConnector webSocketConnector;

    @TestHTTPResource("/ocpp")
    URI baseUri;

    @Test
    public void handle_bootnotification_call() throws InterruptedException {
        CountDownLatch responseLatch = new CountDownLatch(1);

        // open connection
        var connection = webSocketConnector.baseUri(baseUri)
                .path("/BootNotificationTest")
                .executionModel(BasicWebSocketConnector.ExecutionModel.NON_BLOCKING)
                .onTextMessage((_, json) -> {
                    assertThat(json).containsPatternSatisfying(BOOT_NOTIFICATION_RESPONSE, matcher -> {
                        var datetime = ZonedDateTime.parse(matcher.group(1));
                        assertThat(datetime).isCloseTo(ZonedDateTime.now(), within(2, ChronoUnit.SECONDS));
                    });

                    responseLatch.countDown();
                })
                .connectAndAwait();

        assertThat(connection.isOpen()).isTrue();

        // send event
        connection.sendTextAndAwait(BOOT_NOTIFICATION);

        // wait for response
        assertThat(responseLatch.await(50, TimeUnit.SECONDS))
                .describedAs("No response received")
                .isTrue();

        // close connection
        connection.closeAndAwait();

        assertThat(connection.isClosed()).isTrue();
    }

    @Test
    public void handle_heartbeat_call() throws InterruptedException {
        CountDownLatch responseLatch = new CountDownLatch(1);

        // open connection
        var connection = webSocketConnector.baseUri(baseUri)
                .path("/HeartBeatTest")
                .executionModel(BasicWebSocketConnector.ExecutionModel.NON_BLOCKING)
                .onTextMessage((_, json) -> {
                    assertThat(json).containsPatternSatisfying(HEARTBEAT_RESPONSE, matcher -> {
                        var datetime = ZonedDateTime.parse(matcher.group(1));
                        assertThat(datetime).isCloseTo(ZonedDateTime.now(), within(2, ChronoUnit.SECONDS));
                    });

                    responseLatch.countDown();
                })
                .connectAndAwait();

        assertThat(connection.isOpen()).isTrue();

        // send event
        connection.sendTextAndAwait("""
                [
                    2,
                    "VFrf3TJ7oZdp37L5B3nSDHWPIDKwfZOxRIFh",
                    "HeartBeat"
                ]""");

        // wait for response
        assertThat(responseLatch.await(50, TimeUnit.SECONDS))
                .describedAs("No response received")
                .isTrue();

        // close connection
        connection.closeAndAwait();

        assertThat(connection.isClosed()).isTrue();
    }
}