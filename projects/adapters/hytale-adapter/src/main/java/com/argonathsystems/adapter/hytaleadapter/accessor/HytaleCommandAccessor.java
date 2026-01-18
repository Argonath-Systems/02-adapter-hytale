package com.argonathsystems.adapter.hytaleadapter.accessor;

import com.argonathsystems.framework.accessorapi.CommandAccessor;
import com.hytale.api.Server;

public class HytaleCommandAccessor implements CommandAccessor {
    private final Server server;

    public HytaleCommandAccessor(Server server) {
        this.server = server;
    }

    @Override
    public void register(String command, CommandExecutor executor) {
        // TODO: Implement Hytale command registration when API is available
        // server.getCommandManager().register(command, ...);
        server.getLogger().info("Registered command: " + command);
    }
}
