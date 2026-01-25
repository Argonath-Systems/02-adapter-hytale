package com.argonathsystems.adapter.hytaleadapter.accessor;

import com.argonathsystems.adapter.hytaleadapter.converter.ItemDataConverter;
import com.argonathsystems.adapter.hytaleadapter.util.PlayerRefCache;
import com.argonathsystems.framework.accessorapi.InventoryAccessor;
import com.argonathsystems.framework.accessorapi.dto.ItemData;
import com.hytale.api.Server;
import com.hytale.api.entity.Player;
import com.hytale.api.inventory.Inventory;
import com.hytale.api.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class HytaleInventoryAccessor implements InventoryAccessor {
    private final Server server;

    public HytaleInventoryAccessor(Server server) {
        this.server = server;
    }

    private Player getPlayer(UUID playerId) {
        Player player = PlayerRefCache.get(playerId);
        if (player != null) return player;
        
        // Fallback search
        for (com.hytale.api.world.World world : server.getWorlds()) {
            for (Player p : world.getPlayers()) {
                if (p.getUniqueId().equals(playerId)) {
                    PlayerRefCache.add(p);
                    return p;
                }
            }
        }
        return null;
    }

    @Override
    public List<ItemData> getInventoryContents(UUID playerId) {
        Player player = getPlayer(playerId);
        if (player == null) return Collections.emptyList();

        Inventory inv = player.getInventory();
        List<ItemData> items = new ArrayList<>();
        for (int i = 0; i < inv.getSize(); i++) {
            ItemStack is = inv.getItem(i);
            if (is != null) {
                items.add(ItemDataConverter.toDTO(is));
            }
        }
        return items;
    }

    @Override
    public Optional<ItemData> getItem(UUID playerId, int slot) {
        Player player = getPlayer(playerId);
        if (player == null) return Optional.empty();

        ItemStack is = player.getInventory().getItem(slot);
        return Optional.ofNullable(ItemDataConverter.toDTO(is));
    }

    @Override
    public void setItem(UUID playerId, int slot, ItemData item) {
        Player player = getPlayer(playerId);
        if (player == null) return;

        player.getInventory().setItem(slot, ItemDataConverter.fromDTO(item, server));
    }

    @Override
    public Optional<ItemData> addItem(UUID playerId, ItemData item) {
        Player player = getPlayer(playerId);
        if (player == null) return Optional.of(item); 

        player.getInventory().addItem(ItemDataConverter.fromDTO(item, server));
        return Optional.empty(); 
    }

    @Override
    public boolean removeItem(UUID playerId, String itemId, int amount) {
        // Implementation omitted for brevity
        return false;
    }

    @Override
    public void clearInventory(UUID playerId) {
        Player player = getPlayer(playerId);
        if (player != null) {
            player.getInventory().clear();
        }
    }

    @Override
    public boolean hasItem(UUID playerId, String itemId, int amount) {
        Player player = getPlayer(playerId);
        if (player == null) return false;
        
        int found = 0;
        Inventory inv = player.getInventory();
        for (int i = 0; i < inv.getSize(); i++) {
            ItemStack is = inv.getItem(i);
            if (is != null && is.getType().equals(itemId)) {
                found += is.getAmount();
            }
        }
        return found >= amount;
    }
    
    @Override
    public int countItem(UUID playerId, String itemId) {
        return 0; // TODO impl
    }

    @Override
    public Optional<ItemData> getMainHandItem(UUID playerId) {
        return Optional.empty(); // TODO impl
    }

    @Override
    public int getEmptySlots(UUID playerId) {
        return 0; // TODO impl
    }
}