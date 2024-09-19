package eu.diversit.demo.smartcharging.websocket;

import eu.diversit.demo.smartcharging.model.ChargePoint;
import eu.diversit.demo.smartcharging.model.json.ocpp.StatusNotification;
import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.websockets.next.BasicWebSocketConnector;
import jakarta.inject.Inject;
import org.assertj.vavr.api.VavrAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.regex.Matcher;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

@QuarkusTest
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class OcppEndpointNextTest {

    // Request and response json's used for testing.
    // Note: the responses are used in a pattern match and therefore need some '\\' to make the regex parser happy
    private static final String AUTHORIZE_ALLOWED = """
            [2, "UNIQUEID", "Authorize", {"idTag": "Tag1"}]""";
    private static final String AUTHORIZE_ALLOWED_RESPONSE = """
            \\[3,"UNIQUEID",\\{"idTagInfo":\\{"status":"Accepted"}}]""";
    private static final String AUTHORIZE_INVALID = """
            [2, "UNIQUEID", "Authorize", {"idTag": "Unknown"}]""";
    private static final String AUTHORIZE_INVALID_RESPONSE = """
            \\[3,"UNIQUEID",\\{"idTagInfo":\\{"status":"Invalid"}}]""";
    private static final String BOOT_NOTIFICATION = """
            [2,"UNIQUEID","BootNotification",{"chargePointVendor":"ECOTAP","chargePointModel":"DUO2","chargePointSerialNumber":"123","chargeBoxSerialNumber":"11752628","firmwareVersion":"4.3x.32R.16","iccid":"8931081721117385147","imsi":"204080822156943","meterSerialNumber":"21930052"}]""";
    private static final String BOOT_NOTIFICATION_RESPONSE = """
            \\[3,"UNIQUEID",\\{"status":"Accepted","currentTime":"([0-9-:.TZ]+)","interval":600}]""";
    private static final String HEARTBEAT = """
            [2,"UNIQUEID","Heartbeat",{}]""";
    private static final String HEARTBEAT_RESPONSE = """
            \\[3,"UNIQUEID",\\{"currentTime":"([0-9-:.TZ]+)"}]""";
    private static final String STATUS_NOTIFICATION_CONN_0 = """
            [2,"UNIQUEID","StatusNotification",{"connectorId":0,"status":"Available","errorCode":"NoError","info":"","timestamp":"2023-09-28T08:31:30+00:00"}]""";
    private static final String STATUS_NOTIFICATION_RESPONSE = """
            \\[3,"UNIQUEID",\\{}]""";
    private static final String START_TRANSACTION = """
            [2,"UNIQUEID","StartTransaction",{"connectorId": 1, "idTag": "Tag1", "meterStart":23232, "timestamp":"2023-09-28T08:35:30+00:00"}]""";
    private static final String START_TRANSACTION_RESPONSE = """
            \\[3,"UNIQUEID",\\{"idTagInfo":\\{"status":"Accepted"},"transactionId":1}]""";

    private static final Consumer<Matcher> NO_MATCHER = _ -> {
    };
    private final DateTimeFormatter ISO_8601_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss[.SSS]XXX");

    @Inject
    BasicWebSocketConnector webSocketConnector;

    @TestHTTPResource("/ocpp")
    URI baseUri;

    @Inject
    ChargePoint chargePoint;

    private String createStatusNotification(int connectorId, String status) {
        return String.format("""
                        [2,"UNIQUEID","StatusNotification",{"connectorId":%d,"status":"%s","errorCode":"NoError","info":"","timestamp":"%s"}]""",
                connectorId,
                status,
                ZonedDateTime.now().format(ISO_8601_FORMAT)
        );
    }

    @BeforeEach
    public void clearChargePointState() {
        chargePoint.clearState();
    }

    @Test
    public void handle_bootnotification_call() throws InterruptedException {

        sendWebsocketMessage(BOOT_NOTIFICATION, BOOT_NOTIFICATION_RESPONSE, matcher -> {
            var datetime = ZonedDateTime.parse(matcher.group(1));
            assertThat(datetime).isCloseTo(ZonedDateTime.now(), within(2, ChronoUnit.SECONDS));
        });
    }

    @Test
    public void handle_heartbeat_call() throws InterruptedException {
        sendWebsocketMessage(HEARTBEAT, HEARTBEAT_RESPONSE, matcher -> {
            var datetime = ZonedDateTime.parse(matcher.group(1));
            assertThat(datetime).isCloseTo(ZonedDateTime.now(), within(2, ChronoUnit.SECONDS));
        });
    }

    @Test
    public void handle_authorize_call() throws InterruptedException {
        sendWebsocketMessage(AUTHORIZE_ALLOWED, AUTHORIZE_ALLOWED_RESPONSE, NO_MATCHER);
        sendWebsocketMessage(AUTHORIZE_INVALID, AUTHORIZE_INVALID_RESPONSE, NO_MATCHER);
    }

    @Test
    public void handle_statusnotification_call() throws InterruptedException {
        sendWebsocketMessage(STATUS_NOTIFICATION_CONN_0, STATUS_NOTIFICATION_RESPONSE, NO_MATCHER);

        VavrAssertions.assertThat(chargePoint.getState().connectorStatuses()).allSatisfy((connectorId, status) -> {
            assertThat(connectorId).isEqualTo(0);
            assertThat(status.transactions()).isEmpty();
            assertThat(status.statuses()).allSatisfy(statusNotification -> {
                assertThat(statusNotification.getStatus()).isEqualTo(StatusNotification.Status.AVAILABLE);
                assertThat(statusNotification.getConnectorId()).isEqualTo(0);
                assertThat(statusNotification.getErrorCode()).isEqualTo(StatusNotification.ErrorCode.NO_ERROR);
                assertThat(statusNotification.getTimestamp()).contains(ZonedDateTime.parse("2023-09-28T08:31:30+00:00"));
                assertThat(statusNotification.getInfo()).contains("");
                assertThat(statusNotification.getVendorId()).isEmpty();
                assertThat(statusNotification.getVendorErrorCode()).isEmpty();
            });
        });
    }

    @Test
    public void handle_starttransaction_call() throws InterruptedException {
        // send status notification first
        sendWebsocketMessage(createStatusNotification(1, "Available"), STATUS_NOTIFICATION_RESPONSE, NO_MATCHER);

        // start transaction
        sendWebsocketMessage(START_TRANSACTION, START_TRANSACTION_RESPONSE, NO_MATCHER);

        VavrAssertions.assertThat(chargePoint.getState().connectorStatuses()).allSatisfy((connectorId, status) -> {
            assertThat(connectorId).isEqualTo(1);
            assertThat(status.transactions()).allSatisfy(tx -> {
                assertThat(tx.id()).isPositive();
                assertThat(tx.meterStart().value()).isEqualTo(23232);
                assertThat(tx.idTag().value()).isEqualTo("Tag1");
                assertThat(tx.startTimestamp()).isEqualTo(ZonedDateTime.parse("2023-09-28T08:35:30+00:00"));
            });
        });
    }

    private String makeUnique(String value, String uniqueId) {
        return value.replace("UNIQUE", uniqueId);
    }

    private void sendWebsocketMessage(String request, String expectedResponse, Consumer<Matcher> matcherConsumer) throws InterruptedException {
        CountDownLatch responseLatch = new CountDownLatch(1);

        var uniqueId = UUID.randomUUID().toString();

        // open connection
        var connection = webSocketConnector.baseUri(baseUri)
                .path("/OcppEndpointTest")
                .addSubprotocol("ocpp1.6")
                .executionModel(BasicWebSocketConnector.ExecutionModel.NON_BLOCKING)
                .onTextMessage((_, json) -> {
                    assertThat(json).containsPatternSatisfying(makeUnique(expectedResponse, uniqueId), matcherConsumer);

                    responseLatch.countDown();
                })
                .connectAndAwait();

        assertThat(connection.isOpen()).isTrue();

        // send event
        connection.sendTextAndAwait(makeUnique(request, uniqueId));

        // wait for response
        assertThat(responseLatch.await(2, TimeUnit.SECONDS))
                .describedAs("No response received")
                .isTrue();

        // close connection
        connection.closeAndAwait();

        assertThat(connection.isClosed()).isTrue();
    }
}