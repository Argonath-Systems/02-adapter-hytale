package com.argonathsystems.adapter.hytaleadapter.ui;

import java.util.UUID;

/**
 * <p><b>MIGRATION-001 Status:</b> BLOCKED - Requires Official Hytale SDK & HyUI</p>
 */
public class CombatFramesAdapter {
    public void showCombatFrame(UUID playerId) {
        throw new UnsupportedOperationException(
            "CombatFramesAdapter.showCombatFrame() requires official Hytale SDK Player and HyUI"
        );
    }
    
    public void hideCombatFrame(UUID playerId) {
        throw new UnsupportedOperationException(
            "CombatFramesAdapter.hideCombatFrame() requires official Hytale SDK Player and HyUI"
        );
    }
}
