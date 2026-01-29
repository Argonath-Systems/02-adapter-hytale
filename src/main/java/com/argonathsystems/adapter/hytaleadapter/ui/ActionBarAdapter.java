package com.argonathsystems.adapter.hytaleadapter.ui;

import java.util.UUID;

/**
 * <p><b>MIGRATION-001 Status:</b> BLOCKED - Requires Official Hytale SDK & HyUI</p>
 */
public class ActionBarAdapter {
    public void showActionBar(UUID playerId, String message) {
        throw new UnsupportedOperationException(
            "ActionBarAdapter.showActionBar() requires official Hytale SDK Player and HyUI"
        );
    }
    
    public void hideActionBar(UUID playerId) {
        throw new UnsupportedOperationException(
            "ActionBarAdapter.hideActionBar() requires official Hytale SDK Player and HyUI"
        );
    }
    
    public void updateActionBar(UUID playerId, String message) {
        throw new UnsupportedOperationException(
            "ActionBarAdapter.updateActionBar() requires official Hytale SDK Player and HyUI"
        );
    }
}
