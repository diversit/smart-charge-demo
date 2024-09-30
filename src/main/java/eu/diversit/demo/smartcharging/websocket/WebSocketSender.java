package eu.diversit.demo.smartcharging.websocket;

import eu.diversit.demo.smartcharging.model.ChargeBoxId;
import eu.diversit.demo.smartcharging.model.json.Action;
import eu.diversit.demo.smartcharging.model.json.OcppJsonMessage;
import io.quarkus.websockets.next.OpenConnections;
import io.vavr.collection.List;
import io.vavr.control.Option;
import io.vertx.core.json.JsonObject;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class WebSocketSender {

    @Inject
    OpenConnections openConnections;

    /**
     * Sends the command to the websocket for given charge box id.
     *
     * @param chargeBoxId
     * @param action
     * @param payload
     * @return The send command with the message id
     */
    public Option<SendAction> sendCommand(ChargeBoxId chargeBoxId, Action.ByCentralSystem action, Option<Object> payload) {

        return List.ofAll(openConnections.stream())
                .filter(conn1 -> conn1.pathParam("chargeBoxId").equals(chargeBoxId.value()))
                .headOption().map(conn -> {
                    // create a Call object
                    var messageId = OcppJsonMessage.MessageId.createUnique();
                    var call = new OcppJsonMessage.Call(
                            messageId,
                            action,
                            payload.map(JsonObject::mapFrom)
                    );
                    conn.sendTextAndAwait(call);

                    return new SendAction(action, messageId);
                });
    }

    public record SendAction(Action.ByCentralSystem action, OcppJsonMessage.MessageId messageId) {
    }
}
