package com.argonathsystems.adapter.hytaleadapter;

import com.argonathsystems.adapter.hytaleadapter.accessor.*;

/**
 * <p><b>MIGRATION-001 Status:</b> BLOCKED - Requires Official Hytale SDK</p>
 */
public class HytaleAdapterProvider {
    private final Object server;
    
    public HytaleAdapterProvider(Object server) {
        this.server = server;
    }
    
    public HytalePlayerAccessor getPlayerAccessor() {
        return new HytalePlayerAccessor(server);
    }
    
    public HytaleWorldAccessor getWorldAccessor() {
        return new HytaleWorldAccessor(server);
    }
    
    public HytaleUIAccessor getUIAccessor() {
        return new HytaleUIAccessor(server);
    }
    
    public HytaleEventAccessor getEventAccessor() {
        return new HytaleEventAccessor(server);
    }
    
    public HytaleSchedulerAccessor getSchedulerAccessor() {
        return new HytaleSchedulerAccessor(server);
    }
    
    public HytaleCommandAccessor getCommandAccessor() {
        return new HytaleCommandAccessor(server);
    }
    
    public HytaleStorageAccessor getStorageAccessor() {
        return new HytaleStorageAccessor(server);
    }
    
    public HytaleItemAccessor getItemAccessor() {
        return new HytaleItemAccessor(server);
    }
    
    public HytaleInventoryAccessor getInventoryAccessor() {
        return new HytaleInventoryAccessor(server);
    }
    
    public HytaleNPCEntityAccessor getEntityAccessor() {
        return new HytaleNPCEntityAccessor(server);
    }
}
