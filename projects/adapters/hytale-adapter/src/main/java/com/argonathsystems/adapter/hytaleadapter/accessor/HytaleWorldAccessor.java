package com.argonathsystems.adapter.hytaleadapter.accessor;

import com.hytale.api.Server;
import com.argonathsystems.framework.accessorapi.WorldAccessor;
import com.argonathsystems.framework.accessorapi.dto.LocationData;

import java.util.Optional;

public class HytaleWorldAccessor implements WorldAccessor {
    private final Server server;

    public HytaleWorldAccessor(Server server) {
        this.server = server;
    }

    @Override
    public String getWorldName() {
        return "";
    }

    @Override
    public String getBiome(LocationData location) {
        return "";
    }

    @Override
    public Optional<String> getZone(LocationData location) {
        return Optional.empty();
    }

    @Override
    public long getWorldTime() {
        return 0;
    }

    @Override
    public boolean isDaytime() {
        return false;
    }

    @Override
    public boolean hasWeather() {
        return false;
    }

    @Override
    public String getBlockType(LocationData location) {
        return "";
    }

    @Override
    public Optional<LocationData> findSafeLocation(LocationData near, int radius) {
        return Optional.empty();
    }
}