package com.argonathsystems.adapter.hytaleadapter.util;

import java.util.Optional;

/**
 * <p><b>MIGRATION-001 Status:</b> BLOCKED - Requires Official Hytale SDK</p>
 * 
 * <p>Helper for ECS component access.</p>
 * <p>All methods throw UnsupportedOperationException until the SDK is available.</p>
 */
public class ComponentHelper {
    public static <T> Optional<T> getComponent(Object entity, Class<T> componentClass) {
        throw new UnsupportedOperationException(
            "ComponentHelper.getComponent() requires official Hytale SDK: " +
            "Entity.getComponent(componentClass)"
        );
    }
}