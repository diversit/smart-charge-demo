package eu.diversit.demo.smartcharging.model;

import eu.diversit.demo.smartcharging.TransactionIdProvider;
import eu.diversit.demo.smartcharging.model.json.Action;
import eu.diversit.demo.smartcharging.model.json.OcppJsonMessage;
import eu.diversit.demo.smartcharging.model.json.ocpp.*;
import io.vavr.collection.HashMap;
import io.vavr.collection.List;
import io.vavr.control.Either;
import io.vavr.control.Option;
import io.vertx.core.json.JsonObject;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.ZonedDateTime;

import static io.vavr.control.Option.none;
import static io.vavr.control.Option.some;

@ApplicationScoped
public class ChargePoint {
    private static final Logger LOG = LoggerFactory.getLogger(ChargePoint.class);

    private java.util.Map<Connector, List<StatusNotification>> connectors = new java.util.HashMap<>();
    private List<Transaction> transactions = List.empty();
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
                HashMap.ofAll(connectors),
                transactions
        );
    }

    public void clearState() {
        connectors.clear();
        transactions = List.empty();
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
                LOG.warn("Handling of CallResult not implemented yet");
                yield null; // no reply to a CallResult
            }
            case OcppJsonMessage.CallError(var messageId, var errorCode, var errorDescription, var errorDetails) -> {
                LOG.warn("Handling of CallError not implemented yet");
                yield null; // no reply to a CallError
            }
        };
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
                                            var updatedTransaction = t.addMeterValues(metervalues.getMeterValue());
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
