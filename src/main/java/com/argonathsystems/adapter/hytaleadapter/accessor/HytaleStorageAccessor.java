package com.argonathsystems.adapter.hytaleadapter.accessor;

import com.argonathsystems.framework.accessorapi.StorageAccessor;
import com.argonathsystems.framework.accessorapi.data.DataValue;
import java.util.Optional;
import java.util.Set;

/**
 * <p><b>MIGRATION-001 Status:</b> BLOCKED - Requires Official Hytale SDK</p>
 */
public class HytaleStorageAccessor implements StorageAccessor {
    private final Object server;
    public HytaleStorageAccessor(Object server) { this.server = server; }
    
    @Override
    public void setString(String namespace, String key, String value) {
        throw new UnsupportedOperationException("Requires official Hytale SDK storage");
    }
    @Override
    public Optional<String> getString(String namespace, String key) {
        throw new UnsupportedOperationException("Requires official Hytale SDK storage");
    }
    @Override
    public void setInt(String namespace, String key, int value) {
        throw new UnsupportedOperationException("Requires official Hytale SDK storage");
    }
    @Override
    public Optional<Integer> getInt(String namespace, String key) {
        throw new UnsupportedOperationException("Requires official Hytale SDK storage");
    }
    @Override
    public void remove(String namespace, String key) {
        throw new UnsupportedOperationException("Requires official Hytale SDK storage");
    }
    @Override
    public boolean has(String namespace, String key) {
        throw new UnsupportedOperationException("Requires official Hytale SDK storage");
    }
    @Override
    public Set<String> getKeys(String namespace) {
        throw new UnsupportedOperationException("Requires official Hytale SDK storage");
    }
    @Override
    public void flush() {
        throw new UnsupportedOperationException("Requires official Hytale SDK storage");
    }
}
