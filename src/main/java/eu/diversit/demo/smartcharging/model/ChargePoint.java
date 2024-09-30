package eu.diversit.demo.smartcharging.model;

import eu.diversit.demo.smartcharging.TransactionIdProvider;
import eu.diversit.demo.smartcharging.model.json.Action;
import eu.diversit.demo.smartcharging.model.json.DefaultChargingProfiles;
import eu.diversit.demo.smartcharging.model.json.OcppJsonMessage;
import eu.diversit.demo.smartcharging.model.json.ocpp.*;
import eu.diversit.demo.smartcharging.ui.page.Broadcaster;
import eu.diversit.demo.smartcharging.websocket.WebSocketSender;
import io.vavr.collection.HashMap;
import io.vavr.collection.List;
import io.vavr.control.Either;
import io.vavr.control.Option;
import io.vavr.control.Try;
import io.vertx.core.json.JsonObject;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.time.ZonedDateTime;

import static io.vavr.control.Option.none;
import static io.vavr.control.Option.some;

@ApplicationScoped
public class ChargePoint {
    private static final Logger LOG = LoggerFactory.getLogger(ChargePoint.class);
    private final Broadcaster broadcaster;

    private java.util.Map<Connector, List<StatusNotification>> connectors = new java.util.HashMap<>();
    private List<Transaction> transactions = List.empty();
    private BootNotification bootNotification = null;
    private ChargeBoxId chargeBoxId = null;

    @ConfigProperty(name = "tags.allowed")
    private java.util.List<String> allowedTags;

    @Inject
    private TransactionIdProvider transactionIdProvider;

    @Inject
    private WebSocketSender outgoingMessages;

    // save the last call so can be used when receiving a response
    private WebSocketSender.SendAction lastCall = null;

    public ChargePoint(Broadcaster broadcaster) {
        this.broadcaster = broadcaster;
    }


    public ChargePointState getState() {
        return new ChargePointState(
                Option.of(chargeBoxId),
                Option.of(bootNotification),
                HashMap.ofAll(connectors),
                transactions
        );
    }

    public void clearState() {
        connectors.clear();
        transactions = List.empty();
    }

    /**
     * Set a command to the charge point websocket and store the send command
     * to be able to handle the response.
     *
     * @param action
     * @param payload
     */
    private void sendCommand(Action.ByCentralSystem action, Option<Object> payload) {
        outgoingMessages.sendCommand(chargeBoxId, action, payload)
                .peek(callSend -> {
                    lastCall = callSend;
                });
    }

    /**
     * Set the charging limit on an active transaction on given connector.
     * If no transaction is active, a new transaction is started
     *
     * @param connector
     * @param limit
     */
    public void setChargingLimit(Connector connector, BigDecimal limit) {

        // check transaction is active on given connector
        transactions.find(tx -> tx.connector().equals(connector) && tx.stopTimestamp().isEmpty())
                .peek(activeTransaction -> {
                    if (BigDecimal.ZERO.compareTo(limit) == 0) {
                        // stop active transaction
                        sendCommand(Action.ByCentralSystem.REMOTESTOPTRANSACTION, some(RemoteStopTransaction.builder()
                                .withTransactionId(activeTransaction.id().value())
                                .build()
                        ));
                    } else {
                        // create a SetChargingProfile
                        sendCommand(Action.ByCentralSystem.SETCHARGINGPROFILE, some(DefaultChargingProfiles.createSetChargingProfile(connector, activeTransaction, limit)
                        ));
                    }
                })
                .onEmpty(() -> {
                    if (BigDecimal.ZERO.compareTo(limit) < 0) { // when limit > 0
                        // Start a remote transaction with a charging profile
                        sendCommand(Action.ByCentralSystem.REMOTESTARTTRANSACTION, some(RemoteStartTransaction.builder()
                                .withConnectorId(connector.value())
                                .withIdTag("Tag1") // Must provide a valid id which will be used for authentication
                                .withChargingProfile(DefaultChargingProfiles.createChargingProfile(limit))
                                .build()
                        ));
                    }
                });

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
                                        .toEither(() -> new ProcessingCallError(
                                                some("Invalid payload"),
                                                Option.none(),
                                                some(JsonObject.of(
                                                        "action", action.name(),
                                                        "payload", payload.getOrNull()
                                                ))
                                        ))
                                        .flatMap(this::handleCall) // handle call
                                        .peek(_ -> broadcaster.broadcast(new Broadcaster.ChargePointStateUpdate(getState())))
                                        .map(result -> // return result
                                                (OcppJsonMessage) new OcppJsonMessage.CallResult(
                                                        messageId,
                                                        Option.of(result).map(JsonObject::mapFrom)
                                                ))
                                        .getOrElseGet(processingCallError ->
                                                new OcppJsonMessage.CallError(
                                                        messageId,
                                                        processingCallError.errorCode(),
                                                        processingCallError.errorDescription(),
                                                        processingCallError.errorDetails()
                                                ));
                    };
            case OcppJsonMessage.CallResult(var messageId, var payload) -> {
                if (lastCall.messageId().equals(messageId)) {
                    // received response to last call
                    switch (lastCall.action()) {
                        case Action.ByCentralSystem.REMOTESTARTTRANSACTION ->
                                decodePayload(payload, RemoteStopTransactionResponse.class)
                                        .peek(response -> {
                                            LOG.info("RemoteStartTransaction status: {}", response.getStatus());
                                        });

                        case Action.ByCentralSystem.REMOTESTOPTRANSACTION ->
                                decodePayload(payload, RemoteStopTransactionResponse.class)
                                        .peek(response -> {
                                            LOG.info("RemoteStopTransaction status: {}", response.getStatus());
                                        });

                        case Action.ByCentralSystem.SETCHARGINGPROFILE ->
                                decodePayload(payload, SetChargingProfileResponse.class)
                                        .peek(response -> {
                                            LOG.info("SetChargingProfile status: {}", response.getStatus());
                                        });

                        default -> LOG.warn("Handling of CallResult to {} not implemented yet", lastCall.action());
                    }
                } else {
                    LOG.warn("Received unexpected message result");
                }
                yield null; // no reply to a CallResult
            }
            case OcppJsonMessage.CallError(var messageId, var errorCode, var errorDescription, var errorDetails) -> {
                LOG.warn("Handling of CallError not implemented yet");
                yield null; // no reply to a CallError
            }
        };
    }

    private <T> Try<T> decodePayload(Option<JsonObject> payload, Class<T> expectedType) {
        return payload.toTry()
                .mapTry(p -> p.mapTo(expectedType))
                .onFailure(throwable -> LOG.error("Failed to decode payload to type {}", expectedType, throwable));
    }

    private Either<ProcessingCallError, Object> handleCall(Object decodedPayload) {
        return switch (decodedPayload) {
            case Authorize authorize -> {
                // check if provided tag is allowed
                var status = allowedTags.contains(authorize.getIdTag()) ? IdTagInfo.Status.ACCEPTED : IdTagInfo.Status.INVALID;
                yield Either.right(AuthorizeResponse.builder()
                        .withIdTagInfo(IdTagInfo.builder()
                                .withStatus(status)
                                // optionally add an expiry date
                                .build()
                        )
                        .build()
                );
            }
            case BootNotification bn -> {
                // save the bootnotification
                this.bootNotification = bn;

                yield Either.right(
                        BootNotificationResponse.builder()
                                .withStatus(BootNotificationResponse.Status.ACCEPTED)
                                .withInterval(600) // heartbeat interval in seconds (600 = 5min)
                                .withCurrentTime(ZonedDateTime.now())
                                .build()
                );
            }
//            case DataTransfer dt -> {}
//            case DiagnosticsStatusNotification dsn -> {}
            case Heartbeat _ -> // return response with current time
                    Either.right(
                            HeartbeatResponse.builder()
                                    .withCurrentTime(ZonedDateTime.now())
                                    .build()
                    );
//            case FirmwareStatusNotification fsn -> {}
            case MeterValues metervalues -> {

                // only process metervalues with a transaction id
                // add metervalues to transaction in connector status
                // Note: assumes a transaction is active
                metervalues.getTransactionId()
                        .map(TransactionId::of)
                        .ifPresent(txId ->
                                transactions.find(t -> t.id().equals(txId))
                                        .peek(t -> {
                                            var updatedTransaction = t.addMeterValues(List.ofAll(metervalues.getMeterValue()));
                                            transactions = transactions.replace(t, updatedTransaction);
                                        })
                        );

                yield Either.right(
                        MeterValuesResponse.builder().build()
                );
            }
            case StartTransaction startTransaction -> {
                if (startTransaction.getConnectorId() == null) {
                    yield Either.left(new ProcessingCallError(
                            some("No connector provided"),
                            some("Connector is required"),
                            some(JsonObject.of("startTransaction", startTransaction))
                    ));
                }

                if (startTransaction.getIdTag() == null || startTransaction.getIdTag().isBlank()) {
                    yield Either.left(new ProcessingCallError(
                            some("No IdTag provided"),
                            some("IdTag is required"),
                            some(JsonObject.of("startTransaction", startTransaction))
                    ));
                }

                // must verify the validity of the idTag since might have been locally authorized from CP cache
                var status = allowedTags.contains(startTransaction.getIdTag()) ? IdTagInfo__2.Status.ACCEPTED : IdTagInfo__2.Status.INVALID;

                // TODO: verify no transaction is already active. Otherwise return status 'ConcurrentTx'.

                var txId = transactionIdProvider.nextTransactionId();

                // only create and save a transaction when status accepted
                if (IdTagInfo__2.Status.ACCEPTED.equals(status)) {
                    var transaction = Transaction.create(
                            TransactionId.of(txId),
                            Connector.of(startTransaction.getConnectorId()),
                            new IdTag(startTransaction.getIdTag()),
                            new MeterValue(startTransaction.getMeterStart()),
                            startTransaction.getTimestamp()
                    );

                    // add transaction to front of list
                    transactions = transactions.prepend(transaction);
                }

                yield Either.right(StartTransactionResponse.builder()
                        .withIdTagInfo(IdTagInfo__2.builder()
                                .withStatus(status)
                                .build()
                        ).withTransactionId(txId) // must always provide txId regardless of IdTag status
                        .build()
                );
            }
            case StatusNotification sn -> {
                // save the status for the connector
                connectors.compute(Connector.of(sn.getConnectorId()), (_, statusNotifications) -> statusNotifications == null ? List.of(sn) : statusNotifications.prepend(sn));

                yield Either.right(
                        StatusNotificationResponse.builder().build()
                );
            }
            case StopTransaction stoptransaction ->
                    transactions.find(t -> t.id().equals(TransactionId.of(stoptransaction.getTransactionId())))
                            .filter(t -> t.stopReason().isEmpty()) // transaction should not be stopped already
                            .toEither(() -> // error when transaction with id is not found
                                    new ProcessingCallError(
                                            some("Transaction not found with id " + stoptransaction.getTransactionId()),
                                            none(),
                                            some(JsonObject.of("stopTransaction", stoptransaction))
                                    ))
                            .flatMap(tx -> {
                                var stoppedTx = tx.stop(
                                        MeterValue.of(stoptransaction.getMeterStop()),
                                        stoptransaction.getTimestamp(),
                                        Option.ofOptional(stoptransaction.getReason()));
                                transactions = transactions.replace(tx, stoppedTx);

                                var builder = StopTransactionResponse.builder();

                                // add idTagInfo when idTag was provided
                                stoptransaction.getIdTag().ifPresent(idTag ->
                                        builder.withIdTagInfo(IdTagInfo__3.builder()
                                                .withStatus(IdTagInfo__3.Status.ACCEPTED)
                                                .build()
                                        )
                                );
                                return Either.right(builder.build());
                            });

            default -> {
                LOG.warn("Unsupported payload: {}", decodedPayload);

                yield Either.left(new ProcessingCallError(
                        some("Unsupported payload"),
                        none(),
                        some(JsonObject.of("payload", decodedPayload))
                ));
            }
        };
    }

    record ProcessingCallError(Option<String> errorCode,
                               Option<String> errorDescription,
                               Option<JsonObject> errorDetails) {
    }
}
