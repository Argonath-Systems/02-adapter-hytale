package com.argonathsystems.adapter.hytaleadapter;

// ✅ HYTALE IMPORTS ALLOWED IN ADAPTER MODULE
import com.hytale.api.Server;

// Accessor API interfaces
import com.lordofthetales.framework.accessorapi.AccessorProvider;
import com.lordofthetales.framework.accessorapi.PlayerAccessor;
import com.lordofthetales.framework.accessorapi.ItemAccessor;
import com.lordofthetales.framework.accessorapi.WorldAccessor;
import com.lordofthetales.framework.accessorapi.EventAccessor;

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

    public HytaleAdapterAdapterProvider(Server server) {
        this.server = server;
        
        // Initialize all accessors
        this.playerAccessor = new HytalePlayerAccessor(server);
        this.itemAccessor = new HytaleItemAccessor(server);
        this.worldAccessor = new HytaleWorldAccessor(server);
        this.eventAccessor = new HytaleEventAccessor(server);
    }

    @Override
    public PlayerAccessor getPlayerAccessor() {
        return playerAccessor;
    }

    @Override
    public ItemAccessor getItemAccessor() {
        return itemAccessor;
    }

    @Override
    public WorldAccessor getWorldAccessor() {
        return worldAccessor;
    }

    @Override
    public EventAccessor getEventAccessor() {
        return eventAccessor;
    }
    
    /**
     * Shutdown all accessors and release resources.
     */
    public void shutdown() {
        // Cleanup any resources held by accessors
        eventAccessor.unregisterAll();
    }
}