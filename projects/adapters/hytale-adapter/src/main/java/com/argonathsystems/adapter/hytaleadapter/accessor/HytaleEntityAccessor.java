package com.argonathsystems.adapter.hytaleadapter.accessor;

import com.argonathsystems.framework.accessorapi.EntityAccessor;
import com.argonathsystems.framework.accessorapi.dto.EntityData;
import com.argonathsystems.framework.accessorapi.dto.LocationData;
import com.hytale.api.Server;

import java.util.Collection;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

public class HytaleEntityAccessor implements EntityAccessor {
    private final Server server;

    public HytaleEntityAccessor(Server server) {
        this.server = server;
    }

    @Override
    public Optional<EntityData> getEntity(UUID entityId) {
        return Optional.empty();
    }

    @Override
    public Collection<EntityData> getEntities(String worldName) {
        return Collections.emptyList();
    }

    @Override
    public Collection<EntityData> getEntitiesNear(LocationData location, double radius) {
        return Collections.emptyList();
    }

    @Override
    public Optional<EntityData> spawnEntity(String type, LocationData location) {
        return Optional.empty();
    }

    @Override
    public void removeEntity(UUID entityId) {
    }

    @Override
    public void damage(UUID entityId, int amount) {
    }

    @Override
    public void heal(UUID entityId, int amount) {
    }
}