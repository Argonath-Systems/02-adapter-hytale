package com.argonathsystems.adapter.hytaleadapter.accessor;

import com.argonathsystems.framework.accessorapi.SoundAccessor;
import com.argonathsystems.framework.accessorapi.dto.LocationData;
import com.hytale.api.Server;
import java.util.UUID;

public class HytaleSoundAccessor implements SoundAccessor {
    private final Server server;
    public HytaleSoundAccessor(Server server) { this.server = server; }
    
    @Override public void playSound(String soundId, LocationData location, float volume, float pitch) {}
    @Override public void playSoundTo(UUID playerId, String soundId, float volume, float pitch) {}
    @Override public void stopSound(UUID playerId, String soundId) {}
}