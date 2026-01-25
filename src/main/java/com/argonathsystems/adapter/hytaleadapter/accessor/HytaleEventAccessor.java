package com.argonathsystems.adapter.hytaleadapter.accessor;

import com.hytale.api.Server;
import com.argonathsystems.framework.accessorapi.EventAccessor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public class HytaleEventAccessor implements EventAccessor {
    private final Server server;
    private final Map<Class<?>, List<Consumer<?>>> listeners = new ConcurrentHashMap<>();

    public HytaleEventAccessor(Server server) {
        this.server = server;
    }

    @Override
    public <T> EventRegistration register(Class<T> eventType, Consumer<T> listener) {
        return register(eventType, listener, 0);
    }

    @Override
    public <T> EventRegistration register(Class<T> eventType, Consumer<T> listener, int priority) {
        listeners.computeIfAbsent(eventType, k -> new ArrayList<>()).add(listener);
        return new EventRegistration() {
            @Override
            public Class<?> getEventType() {
                return eventType;
            }

            @Override
            public void unregister() {
                List<Consumer<?>> list = listeners.get(eventType);
                if (list != null) {
                    list.remove(listener);
                }
            }
        };
    }

    @Override
    public void unregister(EventRegistration registration) {
        registration.unregister();
    }

    @Override
    public void unregisterAll() {
        listeners.clear();
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T emit(T event) {
        List<Consumer<?>> list = listeners.get(event.getClass());
        if (list != null) {
            // Create a copy to avoid concurrent modification issues during iteration
            for (Consumer<?> consumer : new ArrayList<>(list)) {
                try {
                    ((Consumer<T>) consumer).accept(event);
                } catch (Exception e) {
                    server.getLogger().error("Error dispatching event " + event.getClass().getSimpleName(), e);
                }
            }
        }
        return event;
    }
}