package com.argonathsystems.adapter.hytale.permission;

import java.util.UUID;

/**
 * Permission provider interface for the Hytale adapter layer.
 *
 * <p>Since the Hytale SDK does not expose a native permission API, this
 * interface provides a custom permission system that can be backed by
 * configuration files, a database, or any other storage mechanism.</p>
 *
 * <h2>Usage Pattern:</h2>
 * <pre>{@code
 * PermissionProvider provider = PermissionProvider.getInstance();
 * if (provider.hasPermission(playerId, "argonath.quest.create")) {
 *     // Allow action
 * }
 * }</pre>
 *
 * <h2>Specification Reference</h2>
 * <ul>
 *   <li>SA-ADAPTER-001 (HA-004): Permissions via PermissionProvider singleton</li>
 *   <li>SF-ACCESSOR-API-010 (AA-020): PlayerAccessor.hasPermission</li>
 * </ul>
 *
 * @author Argonath Systems Team
 * @version 1.0.0
 * @since 2.1.0
 */
public interface PermissionProvider {

    /**
     * Singleton instance holder.
     */
    class Holder {
        private static volatile PermissionProvider instance;
    }

    /**
     * Get the global PermissionProvider instance.
     *
     * @return The PermissionProvider, or a default fail-open instance if none registered
     */
    static PermissionProvider getInstance() {
        if (Holder.instance == null) {
            Holder.instance = new ConfigPermissionProvider();
        }
        return Holder.instance;
    }

    /**
     * Register a PermissionProvider implementation.
     *
     * @param provider The provider to register
     */
    static void setInstance(PermissionProvider provider) {
        Holder.instance = provider;
    }

    /**
     * Check if a player has a specific permission.
     *
     * @param playerId The player's UUID
     * @param permission The permission node (e.g., "argonath.quest.create")
     * @return true if the player has the permission
     */
    boolean hasPermission(UUID playerId, String permission);

    /**
     * Check if a player has a specific role.
     *
     * @param playerId The player's UUID
     * @param role The role name (e.g., "admin", "moderator", "builder")
     * @return true if the player has the role
     */
    boolean hasRole(UUID playerId, String role);

    /**
     * Get the primary role of a player.
     *
     * @param playerId The player's UUID
     * @return The primary role name, or "default" if none assigned
     */
    String getPrimaryRole(UUID playerId);
}
