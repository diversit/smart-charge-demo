package eu.diversit.demo.smartcharging.websocket;

import eu.diversit.demo.smartcharging.model.json.OcppJsonMessage;
import eu.diversit.demo.smartcharging.model.json.ocpp.HeartbeatResponse;
import io.quarkus.websockets.next.*;
import io.vavr.control.Option;
import io.vertx.core.json.JsonObject;
import jakarta.inject.Inject;

import java.io.IOException;
import java.time.ZonedDateTime;

/**
 * OCPP WebSocket endpoint using Quarkus WebSocket Next extension.
 * https://quarkus.io/guides/websockets-next-reference
 * <p>
 * The supported subprotocols must be defined in {@code application.properties}
 * otherwise the connections get closed directly.
 */
@WebSocket(path = "/ocpp/{chargeBoxId}")
public class OcppEndpointNext {

    @Inject
    OpenConnections connections;

    @Inject
    WebSocketConnection connection;

    /**
     * Handling websocket events
     **/

    @OnOpen
    public void onOpen(HandshakeRequest handshakeRequest, @PathParam("chargeBoxId") String chargeBoxId) throws IOException {
        System.out.println("On Open (connection " + connection.id() + "): " + handshakeRequest.header("Sec-WebSocket-Protocol"));
        System.out.println("Open connections: " + connections.listAll().size());
    }

    @OnClose
    public void onClose(CloseReason closeReason, @PathParam("chargeBoxId") String chargeBoxId) {
        System.out.println("On Close (connection " + connection.id() + ")");
    }

    @OnError
    public void onError(Throwable throwable, @PathParam("chargeBoxId") String chargeBoxId) throws IOException {
        System.out.println("On Error (connection " + connection.id() + "): " + throwable);
    }

    @OnTextMessage(
            codec = OcppJsonMessageCodec.class,
            outputCodec = OcppJsonMessageCodec.class
    )
    public OcppJsonMessage onMessage(OcppJsonMessage ocppMessage, @PathParam("chargeBoxId") String chargeBoxId) throws IOException {
        System.out.println("On Message (connection " + connection.id() + "): " + ocppMessage);

        return new OcppJsonMessage.CallResult(
                ocppMessage.messageId(),
                Option.of(JsonObject.mapFrom(new HeartbeatResponse(
                        ZonedDateTime.now()
                )))
        );
    }
}
