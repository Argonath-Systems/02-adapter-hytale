package com.argonathsystems.adapter.hytaleadapter.ui;

import java.util.UUID;

/**
 * <p><b>MIGRATION-001 Status:</b> BLOCKED - Requires Official Hytale SDK & HyUI</p>
 */
public class VendorPageAdapter {
    public void openVendorPage(UUID playerId, String vendorId) {
        throw new UnsupportedOperationException(
            "VendorPageAdapter.openVendorPage() requires official Hytale SDK Player and HyUI"
        );
    }
    
    public void closeVendorPage(UUID playerId) {
        throw new UnsupportedOperationException(
            "VendorPageAdapter.closeVendorPage() requires official Hytale SDK Player and HyUI"
        );
    }
}
