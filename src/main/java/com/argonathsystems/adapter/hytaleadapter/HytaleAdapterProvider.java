package com.argonathsystems.adapter.hytaleadapter;

import com.argonathsystems.adapter.hytaleadapter.accessor.*;
import com.argonathsystems.framework.accessorapi.*;
import com.argonathsystems.framework.accessorapi.thread.WorldExecutor;

/**
 * <p><b>MIGRATION-001 Status:</b> BLOCKED - Requires Official Hytale SDK</p>
 */
public class HytaleAdapterProvider implements AccessorProvider {
    private final Object server;
    
    public HytaleAdapterProvider(Object server) {
        this.server = server;
    }
    
    @Override
    public PlayerAccessor getPlayerAccessor() {
        return new HytalePlayerAccessor(server);
    }
    
    @Override
    public WorldAccessor getWorldAccessor() {
        return new HytaleWorldAccessor(server);
    }
    
    @Override
    public WorldManagementAccessor getWorldManagementAccessor() {
        throw new UnsupportedOperationException(
            "WorldManagementAccessor not yet implemented"
        );
    }
    
    @Override
    public UIAccessor getUIAccessor() {
        return new HytaleUIAccessor(server);
    }
    
    @Override
    public EventAccessor getEventAccessor() {
        return new HytaleEventAccessor(server);
    }
    
    @Override
    public SchedulerAccessor getSchedulerAccessor() {
        return new HytaleSchedulerAccessor(server);
    }
    
    @Override
    public CommandAccessor getCommandAccessor() {
        return new HytaleCommandAccessor(server);
    }
    
    @Override
    public ConfigAccessor getConfigAccessor() {
        return new HytaleConfigAccessor();
    }
    
    @Override
    public StorageAccessor getStorageAccessor() {
        return new HytaleStorageAccessor(server);
    }
    
    @Override
    public ItemAccessor getItemAccessor() {
        return new HytaleItemAccessor(server);
    }
    
    @Override
    public InventoryAccessor getInventoryAccessor() {
        return new HytaleInventoryAccessor(server);
    }
    
    @Override
    public EntityAccessor getEntityAccessor() {
        return new HytaleNPCEntityAccessor(server);
    }
    
    @Override
    public NotificationAccessor getNotificationAccessor() {
        return new HytaleNotificationAccessor(server);
    }
    
    @Override
    public SoundAccessor getSoundAccessor() {
        return new HytaleSoundAccessor(server);
    }
    
    @Override
    public HologramAccessor getHologramAccessor() {
        return new HytaleHologramAccessor(server);
    }
    
    @Override
    public AssetAccessor getAssetAccessor() {
        throw new UnsupportedOperationException(
            "AssetAccessor not yet implemented"
        );
    }
    
    @Override
    public GuildAccessor getGuildAccessor() {
        throw new UnsupportedOperationException(
            "GuildAccessor not yet implemented"
        );
    }
    
    @Override
    public WorldExecutor getWorldExecutor() {
        throw new UnsupportedOperationException(
            "WorldExecutor not yet implemented"
        );
    }
    
    @Override
    public String getPlatformId() {
        return "hytale";
    }
    
    @Override
    public boolean supports(Capability capability) {
        // All capabilities not supported until SDK is integrated
        return false;
    }
}
