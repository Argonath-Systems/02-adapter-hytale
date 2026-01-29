package com.argonathsystems.adapter.hytaleadapter.ui;

import java.util.UUID;

/**
 * <p><b>MIGRATION-001 Status:</b> BLOCKED - Requires Official Hytale SDK & HyUI</p>
 */
public class DialoguePageAdapter {
    public void showDialogue(UUID playerId, String dialogueId) {
        throw new UnsupportedOperationException(
            "DialoguePageAdapter.showDialogue() requires official Hytale SDK Player and HyUI"
        );
    }
    
    public void closeDialogue(UUID playerId) {
        throw new UnsupportedOperationException(
            "DialoguePageAdapter.closeDialogue() requires official Hytale SDK Player and HyUI"
        );
    }
}
