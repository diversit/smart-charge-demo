package eu.diversit.demo.smartcharging.websocket;

import eu.diversit.demo.smartcharging.model.json.OcppJsonMessage;
import io.quarkus.websockets.next.TextMessageCodec;
import io.vertx.core.json.JsonArray;
import jakarta.inject.Singleton;

import java.lang.reflect.Type;

// Do NOT use @ApplicationScope since then instance is a Proxy
// and then 'codec.getClass().equals(codecBeanClass)' comparison in
// Codecs.textDecode does not match anymore
@Singleton
public class OcppJsonMessageCodec implements TextMessageCodec<OcppJsonMessage> {

    /**
     * Supports decoding {@link OcppJsonMessage} instances
     * and encoding any {@link OcppJsonMessage} implementations.
     *
     * @param type the type to handle, must not be {@code null}
     * @return True when type is {@link OcppJsonMessage} or implementation of {@link OcppJsonMessage}.
     */
    @Override
    public boolean supports(Type type) {
        if (type instanceof Class classType) {
            return OcppJsonMessage.class.isAssignableFrom(classType);
        }
        return false;
    }

    @Override
    public String encode(OcppJsonMessage message) {
        return message.toJsonArray().encode();
    }

    @Override
    public OcppJsonMessage decode(Type type, String json) {
        return OcppJsonMessage.fromJsonArray(new JsonArray(json)).get();
    }
}
