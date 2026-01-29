package com.argonathsystems.adapter.hytaleadapter.accessor;

import com.argonathsystems.framework.accessorapi.InventoryAccessor;
import com.argonathsystems.framework.accessorapi.dto.ItemData;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * <p><b>MIGRATION-001 Status:</b> BLOCKED - Requires Official Hytale SDK</p>
 */
public class HytaleInventoryAccessor implements InventoryAccessor {
    private final Object server;

    public HytaleInventoryAccessor(Object server) {
        this.server = server;
    }

    @Override
    public List<ItemData> getInventoryContents(UUID playerId) {
        throw new UnsupportedOperationException(
            "HytaleInventoryAccessor.getInventoryContents() requires official Hytale SDK Inventory system"
        );
    }

    @Override
    public Optional<ItemData> getItem(UUID playerId, int slot) {
        throw new UnsupportedOperationException(
            "HytaleInventoryAccessor.getItem() requires official Hytale SDK Inventory system"
        );
    }

    @Override
    public void setItem(UUID playerId, int slot, ItemData item) {
        throw new UnsupportedOperationException(
            "HytaleInventoryAccessor.setItem() requires official Hytale SDK Inventory system"
        );
    }

    @Override
    public Optional<ItemData> addItem(UUID playerId, ItemData item) {
        throw new UnsupportedOperationException(
            "HytaleInventoryAccessor.addItem() requires official Hytale SDK Inventory system"
        );
    }

    @Override
    public boolean removeItem(UUID playerId, String itemId, int amount) {
        throw new UnsupportedOperationException(
            "HytaleInventoryAccessor.removeItem() requires official Hytale SDK Inventory system"
        );
    }

    @Override
    public boolean hasItem(UUID playerId, String itemId, int amount) {
        throw new UnsupportedOperationException(
            "HytaleInventoryAccessor.hasItem() requires official Hytale SDK Inventory system"
        );
    }

    @Override
    public void clearInventory(UUID playerId) {
        throw new UnsupportedOperationException(
            "HytaleInventoryAccessor.clearInventory() requires official Hytale SDK Inventory system"
        );
    }

    @Override
    public int getInventorySize(UUID playerId) {
        throw new UnsupportedOperationException(
            "HytaleInventoryAccessor.getInventorySize() requires official Hytale SDK Inventory system"
        );
    }

    @Override
    public int getFirstEmptySlot(UUID playerId) {
        throw new UnsupportedOperationException(
            "HytaleInventoryAccessor.getFirstEmptySlot() requires official Hytale SDK Inventory system"
        );
    }
}
