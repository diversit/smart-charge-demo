package eu.diversit.demo.smartcharging.model.json;

import io.vavr.collection.List;
import io.vavr.control.Option;
import io.vavr.control.Try;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.UUID;
import java.util.function.Function;

public sealed interface OcppJsonMessage {

    int MESSAGE_TYPE_POS = 0;
    int UNIQUE_ID_POS = 1;
    int ACTION_POS = 2;
    int CALL_PAYLOAD_POS = 3;
    int CALLRESULT_PAYLOAD_POS = 2;
    int ERROR_CODE_POS = 2;
    int ERROR_DESCRIPTION_POS = 3;
    int CALLERROR_DETAILS = 4;

    /**
     * Transform {@link JsonArray} into a {@link OcppJsonMessage}.
     * <p>
     * Message formats:
     * <p>
     * {@code
     * CALL:       [<MessageType>, "<UniqueId>", "<Action>", {<Payload}]
     * CALLRESULT: [<MessageType>, "<UniqueId>", {<Payload}]
     * CALLERROR:  [<MessageType>, "<UniqueId>", "<ErrorCode>", "<ErrorDescription">, {<ErrorDetails>}]
     * }
     * <p>
     * Note: {@code JsonArray.getXX} method may throw runtime exceptions! Make sure to handle them.
     *
     * @param jsonArray
     * @return
     */
    static Option<OcppJsonMessage> fromJsonArray(JsonArray jsonArray) {

        Function<Integer, Option<JsonObject>> getPayload =
                pos -> Try.of(() -> jsonArray.getJsonObject(pos)).toOption();

        return MessageType.findBy(jsonArray.getInteger(MESSAGE_TYPE_POS))
                .map(messageType -> switch (messageType) {
                    case CALL -> new Call(
                            new MessageId(jsonArray.getString(UNIQUE_ID_POS)),
                            Action.findAction(jsonArray.getString(ACTION_POS)).get(), // may throw exception on .get() !!
                            getPayload.apply(CALL_PAYLOAD_POS)
                    );
                    case CALLRESULT -> new CallResult(
                            new MessageId(jsonArray.getString(UNIQUE_ID_POS)),
                            getPayload.apply(CALLRESULT_PAYLOAD_POS)
                    );
                    case CALLERROR -> new CallError(
                            new MessageId(jsonArray.getString(UNIQUE_ID_POS)),
                            Option.of(jsonArray.getString(ERROR_CODE_POS)),
                            Option.of(jsonArray.getString(ERROR_DESCRIPTION_POS)),
                            getPayload.apply(CALLERROR_DETAILS)
                    );
                });
    }

    MessageType messageType();

    MessageId messageId();

    JsonArray toJsonArray();

    enum MessageType {
        CALL(2),
        CALLRESULT(3),
        CALLERROR(4);

        private final int number;

        MessageType(int number) {
            this.number = number;
        }

        public static Option<MessageType> findBy(int number) {
            return List.of(values())
                    .find(v -> v.number == number);
        }

        public int getNumber() {
            return number;
        }
    }

    record MessageId(String value) {
        public static MessageId createUnique() {
            return new MessageId(UUID.randomUUID().toString());
        }
    }

    record Call(MessageId messageId, Action action, Option<JsonObject> payload) implements OcppJsonMessage {
        @Override
        public MessageType messageType() {
            return MessageType.CALL;
        }

        @Override
        public JsonArray toJsonArray() {
            return JsonArray.of(
                    messageType().getNumber(),
                    messageId.value(),
                    action.getClazz().getSimpleName(),
                    payload.getOrNull()
            );
        }
    }

    record CallResult(MessageId messageId, Option<JsonObject> payload) implements OcppJsonMessage {
        @Override
        public MessageType messageType() {
            return MessageType.CALLRESULT;
        }

        @Override
        public JsonArray toJsonArray() {
            return JsonArray.of(
                    messageType().getNumber(),
                    messageId.value(),
                    payload.getOrNull()
            );
        }
    }

    record CallError(MessageId messageId, Option<String> errorCode, Option<String> errorDescription,
                     Option<JsonObject> errorDetails) implements OcppJsonMessage {
        @Override
        public MessageType messageType() {
            return MessageType.CALLERROR;
        }

        @Override
        public JsonArray toJsonArray() {
            return JsonArray.of(
                    messageType().getNumber(),
                    messageId.value(),
                    errorCode.getOrNull(),
                    errorDescription.getOrNull(),
                    errorDetails.getOrNull()
            );
        }
    }
}
