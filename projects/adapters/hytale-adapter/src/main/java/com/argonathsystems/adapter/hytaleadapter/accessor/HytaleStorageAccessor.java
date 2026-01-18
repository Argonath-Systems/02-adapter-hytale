package com.argonathsystems.adapter.hytaleadapter.accessor;

import com.argonathsystems.framework.accessorapi.StorageAccessor;
import com.hytale.api.Server;
import java.util.Optional;

public class HytaleStorageAccessor implements StorageAccessor {
    private final Server server;
    public HytaleStorageAccessor(Server server) { this.server = server; }
    
    @Override public void setString(String namespace, String key, String value) {}
    @Override public Optional<String> getString(String namespace, String key) { return Optional.empty(); }
    @Override public void setInt(String namespace, String key, int value) {}
    @Override public Optional<Integer> getInt(String namespace, String key) { return Optional.empty(); }
    @Override public void remove(String namespace, String key) {}
    @Override public boolean has(String namespace, String key) { return false; }
    @Override public java.util.Set<String> getKeys(String namespace) { return java.util.Collections.emptySet(); }
}