package com.argonathsystems.adapter.hytaleadapter.converter;

import com.argonathsystems.framework.accessorapi.dto.ItemData;
import com.hytale.api.inventory.ItemStack;
import java.util.Collections;
import java.util.UUID;

public class ItemDataConverter {
    public static ItemData toDTO(ItemStack hytaleItemStack) {
        if (hytaleItemStack == null) return null;
        return new ItemData(
            UUID.randomUUID(), // Hytale items might not have UUIDs, generate one
            hytaleItemStack.getType(),
            hytaleItemStack.getAmount(),
            0, // modelData
            64, // maxStackSize
            Collections.emptyMap() // meta
        );
    }

    public static ItemStack fromDTO(ItemData dto, com.hytale.api.Server server) {
        if (dto == null) return null;
        return server.createItemStack(dto.itemId(), dto.amount());
    }
}