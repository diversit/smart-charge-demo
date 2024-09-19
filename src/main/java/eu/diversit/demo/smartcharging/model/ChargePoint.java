package eu.diversit.demo.smartcharging.model;

import eu.diversit.demo.smartcharging.TransactionIdProvider;
import eu.diversit.demo.smartcharging.model.json.Action;
import eu.diversit.demo.smartcharging.model.json.OcppJsonMessage;
import eu.diversit.demo.smartcharging.model.json.ocpp.*;
import io.vavr.collection.HashMap;
import io.vavr.control.Option;
import io.vertx.core.json.JsonObject;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.ZonedDateTime;

import static io.vavr.control.Option.some;

@ApplicationScoped
public class ChargePoint {
    private static final Logger LOG = LoggerFactory.getLogger(ChargePoint.class);
    private java.util.Map<Integer, ConnectorStatus> connectors = new java.util.HashMap<>();
    private BootNotification bootNotification = null;
    private ChargeBoxId chargeBoxId = null;

    @ConfigProperty(name = "tags.allowed")
    private java.util.List<String> allowedTags;

    @Inject
    private TransactionIdProvider transactionIdProvider;

    public ChargePointState getState() {
        return new ChargePointState(
                chargeBoxId,
                bootNotification,
                HashMap.ofAll(connectors)
        );
    }

    public void clearState() {
        connectors.clear();
    }

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
            case Authorize authorize -> {
                // check if provided tag is allowed
                var status = allowedTags.contains(authorize.getIdTag()) ? IdTagInfo.Status.ACCEPTED : IdTagInfo.Status.INVALID;
                yield AuthorizeResponse.builder()
                        .withIdTagInfo(IdTagInfo.builder()
                                .withStatus(status)
                                // optionally add an expiry date
                                .build()
                        )
                        .build();
            }
            case BootNotification bn -> {
                // save the bootnotification
                this.bootNotification = bn;

                yield BootNotificationResponse.builder()
                        .withStatus(BootNotificationResponse.Status.ACCEPTED)
                        .withInterval(600) // heartbeat interval in seconds (600 = 5min)
                        .withCurrentTime(ZonedDateTime.now())
                        .build();
            }
//            case DataTransfer dt -> {}
//            case DiagnosticsStatusNotification dsn -> {}
            case Heartbeat _ -> // return response with current time
                    HeartbeatResponse.builder()
                            .withCurrentTime(ZonedDateTime.now())
                            .build();
//            case FirmwareStatusNotification fsn -> {}
//            case Action.ByChargePoint.METERVALUES metervalues -> {}
            case StartTransaction startTransaction -> {
                // must verify the validity of the idTag since might have been locally authorized from CP cache
                var status = allowedTags.contains(startTransaction.getIdTag()) ? IdTagInfo__2.Status.ACCEPTED : IdTagInfo__2.Status.INVALID;

                var txId = transactionIdProvider.nextTransactionId();

                // only create and save a transaction when status accepted
                if (IdTagInfo__2.Status.ACCEPTED.equals(status)) {
                    var transaction = Transaction.create(
                            txId,
                            new IdTag(startTransaction.getIdTag()),
                            new MeterValue(startTransaction.getMeterStart()),
                            startTransaction.getTimestamp()
                    );

                    // add transaction to connector status
                    // Note: assumes a StatusNotification has been received prior to the StartTransaction!
                    connectors.compute(startTransaction.getConnectorId(), (_, connectorStatus) -> connectorStatus.addTransaction(transaction));
                }

                yield StartTransactionResponse.builder()
                        .withIdTagInfo(IdTagInfo__2.builder()
                                .withStatus(status)
                                .build()
                        ).withTransactionId(txId) // must always provide txId regardless of IdTag status
                        .build();
            }
            case StatusNotification sn -> {
                // save the status for the connector
                connectors.compute(sn.getConnectorId(), (_, currentStatus) -> currentStatus == null ? ConnectorStatus.init(sn) : currentStatus.addStatus(sn));

                yield StatusNotificationResponse.builder()
                        .build();
            }
//            case Action.ByChargePoint.STOPTRANSACTION stoptransaction {}
            default -> {
                LOG.warn("Unsupported payload: {}", decodedPayload);

                yield null;
            }
        };
    }
}
