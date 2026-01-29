package com.argonathsystems.adapter.hytaleadapter.accessor;

import com.argonathsystems.framework.accessorapi.CommandAccessor;

/**
 * <p><b>MIGRATION-001 Status:</b> BLOCKED - Requires Official Hytale SDK</p>
 */
public class HytaleCommandAccessor implements CommandAccessor {
    private final Object server;
    public HytaleCommandAccessor(Object server) { this.server = server; }
    
    @Override
    public void register(String command, CommandExecutor executor) {
        throw new UnsupportedOperationException("HytaleCommandAccessor.register() requires official Hytale SDK CommandManager");
    }
}
