package com.argonathsystems.adapter.hytaleadapter;

// ✅ HYTALE IMPORTS ALLOWED IN ADAPTER MODULE
import com.hytale.api.Server;

// Accessor API interfaces
import com.argonathsystems.framework.accessorapi.AccessorProvider;
import com.argonathsystems.framework.accessorapi.PlayerAccessor;
import com.argonathsystems.framework.accessorapi.ItemAccessor;
import com.argonathsystems.framework.accessorapi.WorldAccessor;
import com.argonathsystems.framework.accessorapi.EventAccessor;

// Accessor implementations
import com.argonathsystems.adapter.hytaleadapter.accessor.*;

/**
 * Hytale implementation of {@link AccessorProvider}.
 * 
 * <p>Provides all accessor implementations for the Hytale platform.
 * Mods should request accessors from this provider rather than
 * creating them directly.
 * 
 * @author LordOfTheTales Team
 * @version 1.0.0-SNAPSHOT
 */
public class HytaleAdapterAdapterProvider implements AccessorProvider {

    private final Server server;
    
    // Cached accessor instances
    private final HytalePlayerAccessor playerAccessor;
    private final HytaleItemAccessor itemAccessor;
    private final HytaleWorldAccessor worldAccessor;
    private final HytaleEventAccessor eventAccessor;
    private final HytaleEntityAccessor entityAccessor;
    private final HytaleInventoryAccessor inventoryAccessor;
    private final HytaleSchedulerAccessor schedulerAccessor;
    private final HytaleStorageAccessor storageAccessor;
    private final HytaleUIAccessor uiAccessor;
    private final HytaleSoundAccessor soundAccessor;
    private final HytaleNotificationAccessor notificationAccessor;
    private final HytaleHologramAccessor hologramAccessor;
    private final com.argonathsystems.adapter.hytaleadapter.thread.HytaleWorldExecutor worldExecutor;

    public HytaleAdapterAdapterProvider(Server server) {
        this.server = server;
        
        // Initialize all accessors
        this.playerAccessor = new HytalePlayerAccessor(server);
        this.itemAccessor = new HytaleItemAccessor(server);
        this.worldAccessor = new HytaleWorldAccessor(server);
        this.eventAccessor = new HytaleEventAccessor(server);
        this.entityAccessor = new HytaleEntityAccessor(server);
        this.inventoryAccessor = new HytaleInventoryAccessor(server);
        this.schedulerAccessor = new HytaleSchedulerAccessor(server);
        this.storageAccessor = new HytaleStorageAccessor(server);
        this.uiAccessor = new HytaleUIAccessor(server);
        this.soundAccessor = new HytaleSoundAccessor(server);
        this.notificationAccessor = new HytaleNotificationAccessor(server);
        this.hologramAccessor = new HytaleHologramAccessor(server);
        this.worldExecutor = new com.argonathsystems.adapter.hytaleadapter.thread.HytaleWorldExecutor(server);
    }

    @Override
    public PlayerAccessor getPlayerAccessor() { return playerAccessor; }

    @Override
    public ItemAccessor getItemAccessor() { return itemAccessor; }

    @Override
    public WorldAccessor getWorldAccessor() { return worldAccessor; }

    @Override
    public EventAccessor getEventAccessor() { return eventAccessor; }

    @Override
    public com.argonathsystems.framework.accessorapi.EntityAccessor getEntityAccessor() { return entityAccessor; }

    @Override
    public com.argonathsystems.framework.accessorapi.InventoryAccessor getInventoryAccessor() { return inventoryAccessor; }

    @Override
    public com.argonathsystems.framework.accessorapi.SchedulerAccessor getSchedulerAccessor() { return schedulerAccessor; }

    @Override
    public com.argonathsystems.framework.accessorapi.StorageAccessor getStorageAccessor() { return storageAccessor; }

    @Override
    public com.argonathsystems.framework.accessorapi.UIAccessor getUIAccessor() { return uiAccessor; }

    @Override
    public com.argonathsystems.framework.accessorapi.SoundAccessor getSoundAccessor() { return soundAccessor; }

    @Override
    public com.argonathsystems.framework.accessorapi.NotificationAccessor getNotificationAccessor() { return notificationAccessor; }

    @Override
    public com.argonathsystems.framework.accessorapi.HologramAccessor getHologramAccessor() { return hologramAccessor; }

    @Override
    public com.argonathsystems.framework.accessorapi.thread.WorldExecutor getWorldExecutor() { return worldExecutor; }

    @Override
    public String getPlatformId() { return "hytale"; }

    @Override
    public boolean supports(Capability capability) { return true; }
    
    /**
     * Shutdown all accessors and release resources.
     */
    public void shutdown() {
        // Cleanup any resources held by accessors
        eventAccessor.unregisterAll();
    }
}