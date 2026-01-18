package com.argonathsystems.adapter.hytaleadapter.accessor;

import com.argonathsystems.framework.accessorapi.HologramAccessor;
import com.argonathsystems.framework.accessorapi.dto.LocationData;
import com.hytale.api.Server;
import java.util.List;
import java.util.UUID;

public class HytaleHologramAccessor implements HologramAccessor {
    private final Server server;
    public HytaleHologramAccessor(Server server) { this.server = server; }
    
    @Override public UUID createHologram(LocationData location, String... lines) { return UUID.randomUUID(); }
    @Override public void updateHologram(UUID hologramId, String... lines) {}
    @Override public void moveHologram(UUID hologramId, LocationData newLocation) {}
    @Override public void removeHologram(UUID hologramId) {}
}