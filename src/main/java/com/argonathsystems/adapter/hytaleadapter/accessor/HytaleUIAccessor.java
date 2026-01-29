package com.argonathsystems.adapter.hytaleadapter.accessor;

import com.argonathsystems.framework.accessorapi.UIAccessor;
import com.argonathsystems.framework.accessorapi.ui.HudLayoutData;
import com.argonathsystems.framework.accessorapi.ui.UIContext;
import com.argonathsystems.framework.accessorapi.ui.UIUpdateData;

import java.util.UUID;

/**
 * <p><b>MIGRATION-001 Status:</b> BLOCKED - Requires Official Hytale SDK & HyUI</p>
 * 
 * <p>This accessor implements UI/HUD operations.</p>
 * <p>Implementation requires the official Hytale SDK (com.hypixel.hytale.*)
 * and HyUI integration which are not available in the development environment.</p>
 * 
 * <p>All methods throw UnsupportedOperationException until dependencies are available.</p>
 */
public class HytaleUIAccessor implements UIAccessor {
    private final Object server;

    public HytaleUIAccessor(Object server) {
        this.server = server;
    }

    @Override
    public void registerUI(String uiId, String uiDef) {
        throw new UnsupportedOperationException(
            "HytaleUIAccessor.registerUI() requires HyUI HyUIML template system"
        );
    }

    @Override
    public void openUI(UUID playerId, String uiId, UIContext context) {
        throw new UnsupportedOperationException(
            "HytaleUIAccessor.openUI() requires official Hytale SDK Player and HyUI UI system"
        );
    }

    @Override
    public void closeUI(UUID playerId) {
        throw new UnsupportedOperationException(
            "HytaleUIAccessor.closeUI() requires official Hytale SDK Player and HyUI UI system"
        );
    }

    @Override
    public boolean hasUIOpen(UUID playerId, String uiId) {
        throw new UnsupportedOperationException(
            "HytaleUIAccessor.hasUIOpen() requires official Hytale SDK Player and HyUI UI tracking"
        );
    }

    @Override
    public void sendUIUpdate(UUID playerId, String elementId, UIUpdateData data) {
        throw new UnsupportedOperationException(
            "HytaleUIAccessor.sendUIUpdate() requires HyUI element update system"
        );
    }

    @Override
    public void addHud(UUID playerId, String hudId, HudLayoutData layout) {
        throw new UnsupportedOperationException(
            "HytaleUIAccessor.addHud() requires HyUI HUD system"
        );
    }

    @Override
    public void removeHud(UUID playerId, String hudId) {
        throw new UnsupportedOperationException(
            "HytaleUIAccessor.removeHud() requires HyUI HUD system"
        );
    }

    @Override
    public void updateHud(UUID playerId, String hudId, String elementId, UIUpdateData data) {
        throw new UnsupportedOperationException(
            "HytaleUIAccessor.updateHud() requires HyUI HUD update system"
        );
    }
}
