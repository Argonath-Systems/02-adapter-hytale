package com.argonathsystems.adapter.hytaleadapter.accessor;

import com.argonathsystems.framework.accessorapi.ConfigAccessor;
import com.hytale.api.Server;
import java.util.Optional;

public class HytaleConfigAccessor implements ConfigAccessor {
    private final Server server;

    public HytaleConfigAccessor(Server server) {
        this.server = server;
    }

    @Override
    public <T> Optional<T> load(String name, Class<T> type) {
        // TODO: Implement config loading (JSON/TOML)
        return Optional.empty();
    }

    @Override
    public <T> void save(String name, T config) {
        // TODO: Implement config saving
    }
}
