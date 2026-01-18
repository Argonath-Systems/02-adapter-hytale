package com.argonathsystems.adapter.hytaleadapter.accessor;

import com.argonathsystems.framework.accessorapi.InventoryAccessor;
import com.argonathsystems.framework.accessorapi.dto.ItemData;
import com.hytale.api.Server;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class HytaleInventoryAccessor implements InventoryAccessor {
    private final Server server;

    public HytaleInventoryAccessor(Server server) {
        this.server = server;
    }

    @Override
    public List<ItemData> getInventoryContents(UUID playerId) {
        return Collections.emptyList();
    }

    @Override
    public Optional<ItemData> getItem(UUID playerId, int slot) {
        return Optional.empty();
    }

    @Override
    public void setItem(UUID playerId, int slot, ItemData item) {
    }

    @Override
    public Optional<ItemData> addItem(UUID playerId, ItemData item) {
        return Optional.of(item);
    }

    @Override
    public boolean removeItem(UUID playerId, String itemId, int amount) {
        return false;
    }

    @Override
    public void clearInventory(UUID playerId) {
    }

    @Override
    public boolean hasItem(UUID playerId, String itemId, int amount) {
        return false;
    }

    @Override
    public int countItem(UUID playerId, String itemId) {
        return 0;
    }

    @Override
    public Optional<ItemData> getMainHandItem(UUID playerId) {
        return Optional.empty();
    }

    @Override
    public int getEmptySlots(UUID playerId) {
        return 0;
    }
}