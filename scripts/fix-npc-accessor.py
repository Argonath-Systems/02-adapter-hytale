#!/usr/bin/env python3
"""Fix HytaleNPCEntityAccessor with correct interface methods."""
from pathlib import Path

BASE_DIR = Path("/mnt/d/Gaming/Argonath-Systems/02-adapter-hytale/src/main/java")

npc_accessor = BASE_DIR / "com/argonathsystems/adapter/hytaleadapter/accessor/HytaleNPCEntityAccessor.java"
npc_accessor.write_text("""package com.argonathsystems.adapter.hytaleadapter.accessor;

import com.argonathsystems.framework.accessorapi.EntityAccessor;
import com.argonathsystems.framework.accessorapi.data.DataValue;
import com.argonathsystems.framework.accessorapi.dto.EntityData;
import com.argonathsystems.framework.accessorapi.dto.LocationData;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

/**
 * <p><b>MIGRATION-001 Status:</b> BLOCKED - Requires Official Hytale SDK</p>
 * 
 * <p>This accessor implements Entity/NPC operations.</p>
 * <p>Implementation requires the official Hytale SDK (com.hypixel.hytale.*)
 * which is not available in the development environment.</p>
 * 
 * <p>All methods throw UnsupportedOperationException until the SDK is available.</p>
 */
public class HytaleNPCEntityAccessor implements EntityAccessor {
    private final Object server;

    public HytaleNPCEntityAccessor(Object server) {
        this.server = server;
    }

    @Override
    public Optional<EntityData> getEntity(UUID entityId) {
        throw new UnsupportedOperationException(
            "HytaleNPCEntityAccessor.getEntity() requires official Hytale SDK Entity class"
        );
    }

    @Override
    public Collection<EntityData> getEntities(String worldName) {
        throw new UnsupportedOperationException(
            "HytaleNPCEntityAccessor.getEntities() requires official Hytale SDK World/Entity classes"
        );
    }

    @Override
    public Collection<EntityData> getEntitiesNear(LocationData location, double radius) {
        throw new UnsupportedOperationException(
            "HytaleNPCEntityAccessor.getEntitiesNear() requires official Hytale SDK World/Entity classes"
        );
    }

    @Override
    public Optional<EntityData> spawnEntity(String type, LocationData location) {
        throw new UnsupportedOperationException(
            "HytaleNPCEntityAccessor.spawnEntity() requires official Hytale SDK World.spawnEntity() method"
        );
    }

    @Override
    public void removeEntity(UUID entityId) {
        throw new UnsupportedOperationException(
            "HytaleNPCEntityAccessor.removeEntity() requires official Hytale SDK Entity.remove() method"
        );
    }

    @Override
    public void damage(UUID entityId, int amount) {
        throw new UnsupportedOperationException(
            "HytaleNPCEntityAccessor.damage() requires official Hytale SDK Entity damage system"
        );
    }

    @Override
    public void heal(UUID entityId, int amount) {
        throw new UnsupportedOperationException(
            "HytaleNPCEntityAccessor.heal() requires official Hytale SDK Entity health system"
        );
    }

    @Override
    public void navigateTo(UUID entityId, LocationData target) {
        throw new UnsupportedOperationException(
            "HytaleNPCEntityAccessor.navigateTo() requires official Hytale SDK Entity pathfinding/navigation system"
        );
    }

    @Override
    public void teleport(UUID entityId, LocationData target) {
        throw new UnsupportedOperationException(
            "HytaleNPCEntityAccessor.teleport() requires official Hytale SDK Entity.teleport() method"
        );
    }

    @Override
    public void setMetadata(UUID entityId, String key, DataValue value) {
        throw new UnsupportedOperationException(
            "HytaleNPCEntityAccessor.setMetadata() requires official Hytale SDK Entity metadata system"
        );
    }

    @Override
    public Optional<DataValue> getMetadata(UUID entityId, String key) {
        throw new UnsupportedOperationException(
            "HytaleNPCEntityAccessor.getMetadata() requires official Hytale SDK Entity metadata system"
        );
    }
}
""")

print("✓ Fixed: HytaleNPCEntityAccessor.java (complete rewrite with correct interface)")
