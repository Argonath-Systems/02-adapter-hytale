package com.argonathsystems.adapter.hytaleadapter.accessor;

import com.argonathsystems.framework.accessorapi.UIAccessor;
import com.hytale.api.Server;
import java.util.UUID;

public class HytaleUIAccessor implements UIAccessor {
    private final Server server;
    public HytaleUIAccessor(Server server) { this.server = server; }
    
    @Override public void openUI(UUID playerId, String uiId, Object context) {}
    @Override public void closeUI(UUID playerId) {}
    @Override public boolean hasUIOpen(UUID playerId, String uiId) { return false; }
    @Override public void sendUIUpdate(UUID playerId, String elementId, Object data) {}
}