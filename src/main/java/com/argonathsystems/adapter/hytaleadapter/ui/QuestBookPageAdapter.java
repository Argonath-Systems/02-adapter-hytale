package com.argonathsystems.adapter.hytaleadapter.ui;

import java.util.UUID;

/**
 * <p><b>MIGRATION-001 Status:</b> BLOCKED - Requires Official Hytale SDK & HyUI</p>
 */
public class QuestBookPageAdapter {
    public void openQuestBook(UUID playerId) {
        throw new UnsupportedOperationException(
            "QuestBookPageAdapter.openQuestBook() requires official Hytale SDK Player and HyUI"
        );
    }
    
    public void closeQuestBook(UUID playerId) {
        throw new UnsupportedOperationException(
            "QuestBookPageAdapter.closeQuestBook() requires official Hytale SDK Player and HyUI"
        );
    }
    
    public void updateQuestBook(UUID playerId, String questId) {
        throw new UnsupportedOperationException(
            "QuestBookPageAdapter.updateQuestBook() requires official Hytale SDK Player and HyUI"
        );
    }
}
