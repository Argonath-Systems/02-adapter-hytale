package com.argonathsystems.adapter.hytaleadapter.accessor;

import com.hytale.api.Server;
import com.argonathsystems.framework.accessorapi.ItemAccessor;
import com.argonathsystems.framework.accessorapi.dto.ItemData;
import com.argonathsystems.framework.accessorapi.dto.ItemDefinitionData;

import java.util.Collection;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;

public class HytaleItemAccessor implements ItemAccessor {
    private final Server server;

    public HytaleItemAccessor(Server server) {
        this.server = server;
    }

    @Override
    public Optional<ItemDefinitionData> getItemDefinition(String itemId) {
        return Optional.empty();
    }

    @Override
    public Collection<ItemDefinitionData> getAllItemDefinitions() {
        return Collections.emptyList();
    }

    @Override
    public Collection<ItemDefinitionData> getItemsByTag(String tag) {
        return Collections.emptyList();
    }

    @Override
    public Set<String> getItemTags(String itemId) {
        return Collections.emptySet();
    }

    @Override
    public ItemData createItem(String itemId, int amount) {
        return null;
    }
}