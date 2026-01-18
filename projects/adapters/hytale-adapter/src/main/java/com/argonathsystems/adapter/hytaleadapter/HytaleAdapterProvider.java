package com.argonathsystems.adapter.hytaleadapter;

import com.argonathsystems.adapter.hytaleadapter.accessor.*;
import com.argonathsystems.adapter.hytaleadapter.thread.HytaleWorldExecutor;
import com.argonathsystems.framework.accessorapi.*;
import com.hytale.api.Server;

public class HytaleAdapterProvider implements AccessorProvider {
    private final Server server;
    // Cached accessors
    private final HytalePlayerAccessor playerAccessor;
    private final HytaleEntityAccessor entityAccessor;
    private final HytaleItemAccessor itemAccessor;
    private final HytaleInventoryAccessor inventoryAccessor;
    private final HytaleWorldAccessor worldAccessor;
    private final HytaleEventAccessor eventAccessor;
    private final HytaleSchedulerAccessor schedulerAccessor;
    private final HytaleStorageAccessor storageAccessor;
    private final HytaleUIAccessor uiAccessor;
    private final HytaleSoundAccessor soundAccessor;
    private final HytaleNotificationAccessor notificationAccessor;
    private final HytaleHologramAccessor hologramAccessor;
    
    // Internal
    private final HytaleWorldExecutor worldExecutor;

    public HytaleAdapterProvider(Server server) {
        this.server = server;
        this.worldExecutor = new HytaleWorldExecutor(server);
        
        // Initialize accessors
        // Note: Some might require worldExecutor or other deps if improved later.
        this.playerAccessor = new HytalePlayerAccessor(server);
        this.entityAccessor = new HytaleEntityAccessor(server);
        this.itemAccessor = new HytaleItemAccessor(server); 
        this.inventoryAccessor = new HytaleInventoryAccessor(server);
        this.worldAccessor = new HytaleWorldAccessor(server);
        this.eventAccessor = new HytaleEventAccessor(server);
        this.schedulerAccessor = new HytaleSchedulerAccessor(server);
        this.storageAccessor = new HytaleStorageAccessor(server);
        this.uiAccessor = new HytaleUIAccessor(server);
        this.soundAccessor = new HytaleSoundAccessor(server);
        this.notificationAccessor = new HytaleNotificationAccessor(server);
        this.hologramAccessor = new HytaleHologramAccessor(server);
    }
    
    @Override public PlayerAccessor getPlayerAccessor() { return playerAccessor; }
    @Override public EntityAccessor getEntityAccessor() { return entityAccessor; }
    @Override public ItemAccessor getItemAccessor() { return itemAccessor; }
    @Override public InventoryAccessor getInventoryAccessor() { return inventoryAccessor; }
    @Override public WorldAccessor getWorldAccessor() { return worldAccessor; }
    @Override public EventAccessor getEventAccessor() { return eventAccessor; }
    @Override public SchedulerAccessor getSchedulerAccessor() { return schedulerAccessor; }
    @Override public StorageAccessor getStorageAccessor() { return storageAccessor; }
    @Override public UIAccessor getUIAccessor() { return uiAccessor; }
    @Override public SoundAccessor getSoundAccessor() { return soundAccessor; }
    @Override public NotificationAccessor getNotificationAccessor() { return notificationAccessor; }
    @Override public HologramAccessor getHologramAccessor() { return hologramAccessor; }
    
    @Override 
    public com.argonathsystems.framework.accessorapi.thread.WorldExecutor getWorldExecutor() { 
        return worldExecutor; 
    }

    @Override
    public String getPlatformId() {
        return "hytale";
    }

    @Override
    public boolean supports(Capability capability) {
        switch (capability) {
            case CUSTOM_UI:
            case CUSTOM_SOUNDS:
            case NOTIFICATIONS:
            case HOLOGRAMS:
                return true;
            // Add others as implemented
            default:
                return false;
        }
    }
}
