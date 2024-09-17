package eu.diversit.demo.smartcharging;

import eu.diversit.demo.smartcharging.model.ChargeBoxId;
import eu.diversit.demo.smartcharging.model.json.Action;
import eu.diversit.demo.smartcharging.model.json.OcppJsonMessage;
import eu.diversit.demo.smartcharging.model.json.ocpp.BootNotification;
import eu.diversit.demo.smartcharging.model.json.ocpp.BootNotificationResponse;
import eu.diversit.demo.smartcharging.model.json.ocpp.StatusNotification;
import io.vavr.control.Option;
import io.vertx.core.json.JsonObject;
import jakarta.enterprise.context.ApplicationScoped;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Map;

import static io.vavr.control.Option.some;

@ApplicationScoped
public class ChargePoint {
    private static final Logger LOG = LoggerFactory.getLogger(ChargePoint.class);

    private Map<Integer, StatusNotification> connectors = new HashMap<>();
    private BootNotification bootNotification = null;
    private ChargeBoxId chargeBoxId = null;

    /**
     * @param chargeBoxId
     * @param ocppJsonMessage The OCPP message to handle
     * @return a response or null when no response is needed
     */
    public OcppJsonMessage handleOcppMessage(ChargeBoxId chargeBoxId, OcppJsonMessage ocppJsonMessage) {
        if (this.chargeBoxId == null) {
            this.chargeBoxId = chargeBoxId;
        }
        if (!this.chargeBoxId.equals(chargeBoxId)) {
            return new OcppJsonMessage.CallError(
                    ocppJsonMessage.messageId(),
                    some("Invalid chargebox id"),
                    some("Only supporting ChargePoint with chargebox id '" + chargeBoxId.value() + "'"),
                    some(JsonObject.of(
                            "chargeBoxId", chargeBoxId.value(),
                            "message", ocppJsonMessage
                    ))
            );
        }

        return switch (ocppJsonMessage) {
            case OcppJsonMessage.Call(var messageId, var action, var payload) -> // verify action
                    switch (action) {
                        case Action.ByCentralSystem _ -> {
                            LOG.error("Invalid call with 'ByCentralSystem' action {}", action.name());
                            yield new OcppJsonMessage.CallError(
                                    messageId,
                                    some("Invalid action '" + action.name() + "'"),
                                    Option.none(),
                                    some(JsonObject.of(
                                            "action", action.name(),
                                            "payload", payload.getOrNull()
                                    ))
                            );
                        }
                        case Action.ByChargePoint _ ->
                            // decode payload based on action
                                payload.map(jsonObject -> jsonObject.mapTo(action.getClazz()))
                                        .map(this::handleCall) // handle call
                                        .map(result -> // return result
                                                (OcppJsonMessage) new OcppJsonMessage.CallResult(
                                                        messageId,
                                                        Option.of(result).map(JsonObject::mapFrom)
                                                ))
                                        .getOrElse(() -> // otherwise return error
                                                new OcppJsonMessage.CallError(
                                                        messageId,
                                                        some("Invalid payload"),
                                                        Option.none(),
                                                        some(JsonObject.of(
                                                                "action", action.name(),
                                                                "payload", payload.getOrNull()
                                                        ))
                                                ));
                    };
            case OcppJsonMessage.CallResult(var messageId, var payload) -> {
                LOG.warn("Handling of CallResult not implemented yet");
                yield null; // no reply to a CallResult
            }
            case OcppJsonMessage.CallError(var messageId, var errorCode, var errorDescription, var errorDetails) -> {
                LOG.warn("Handling of CallError not implemented yet");
                yield null; // no reply to a CallError
            }
        };
    }

    private Object handleCall(Object decodedPayload) {
        return switch (decodedPayload) {
//            case Action.ByCentralSystem _ -> LOG.error("Invalid call with 'ByCentralSystem' action {}", action.name());
////            case Action.ByChargePoint.AUTHORIZE authorize -> {}
            case BootNotification bn -> {
                // save the bootnotification
                this.bootNotification = bn;

                yield BootNotificationResponse.builder()
                        .withStatus(BootNotificationResponse.Status.ACCEPTED)
                        .withInterval(600) // heartbeat interval in seconds (600 = 5min)
                        .withCurrentTime(ZonedDateTime.now())
                        .build();
            }
//            case Action.ByChargePoint.DATATRANSFER datatransfer -> {}
//            case Action.ByChargePoint.DIAGNOSTICSSTATUSNOTIFICATION dsn -> {}
//            case Action.ByChargePoint.HEARTBEAT heartbeat -> {}
//            case Action.ByChargePoint.FIRMWARESTATUSNOTIFICATION fsn -> {}
//            case Action.ByChargePoint.METERVALUES metervalues -> {}
//            case Action.ByChargePoint.STARTTRANSACTION starttransaction -> {}
//            case Action.ByChargePoint.STATUSNOTIFICATION statusnotification -> {}
//            case Action.ByChargePoint.STOPTRANSACTION stoptransaction {}
            default -> {
                LOG.warn("Unsupported payload: {}", decodedPayload);

                yield null;
            }
        };
    }

}
