package com.argonathsystems.adapter.hytaleadapter;

/**
 * <p><b>MIGRATION-001 Status:</b> BLOCKED - Requires Official Hytale SDK</p>
 */
public class HytaleAdapterEventListener {
    private final HytaleAdapterProvider adapters;
    
    public HytaleAdapterEventListener(HytaleAdapterProvider adapters) {
        this.adapters = adapters;
    }
    
    public void onPlayerJoin(Object event) {
        throw new UnsupportedOperationException(
            "HytaleAdapterEventListener.onPlayerJoin() requires official Hytale SDK Event system"
        );
    }
    
    public void onPlayerQuit(Object event) {
        throw new UnsupportedOperationException(
            "HytaleAdapterEventListener.onPlayerQuit() requires official Hytale SDK Event system"
        );
    }
}
