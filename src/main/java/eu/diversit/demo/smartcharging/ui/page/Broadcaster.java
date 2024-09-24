package eu.diversit.demo.smartcharging.ui.page;

import eu.diversit.demo.smartcharging.model.ChargePointState;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Consumer;

/**
 * Allow broadcasting live updates to the frontend
 * by providing a listener/subscription solution.
 */
@ApplicationScoped
public class Broadcaster {

    private final Map<UpdateType, ConcurrentLinkedQueue<Consumer<Object>>> registrars = new ConcurrentHashMap<>();

    public synchronized <T> Registration register(UpdateType key, Consumer<T> consumer) {
        var list = registrars.getOrDefault(key, new ConcurrentLinkedQueue<>());

        @SuppressWarnings("unchecked")
        var uncheckedConsumer = (Consumer<Object>) consumer;

        list.add(uncheckedConsumer);
        registrars.put(key, list);

        return () -> {
            synchronized (Broadcaster.class) {
                var updatedList = registrars.get(key);
                updatedList.remove(uncheckedConsumer);
                registrars.put(key, updatedList);
            }
        };
    }

    public synchronized void broadcast(Update<?> update) {
        var list = registrars.getOrDefault(update.updateType(), new ConcurrentLinkedQueue<>());
        list.forEach(consumer -> consumer.accept(update.payload()));
    }

    enum UpdateType {
        CHARGE_POINT_STATE
    }

    interface Registration {
        void unregister();
    }

    interface Update<T> {
        UpdateType updateType();

        T payload();
    }

    public record ChargePointStateUpdate(ChargePointState payload) implements Update<ChargePointState> {
        @Override
        public UpdateType updateType() {
            return UpdateType.CHARGE_POINT_STATE;
        }
    }
}
