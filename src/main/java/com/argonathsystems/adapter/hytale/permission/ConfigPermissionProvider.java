package com.argonathsystems.adapter.hytale.permission;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Configuration-based permission provider.
 *
 * <p>Default implementation of {@link PermissionProvider} that uses an in-memory
 * role-to-permission mapping. Roles and permissions can be loaded from config
 * files or set programmatically.</p>
 *
 * <h2>Default Roles:</h2>
 * <ul>
 *   <li>{@code admin} — All permissions (wildcard)</li>
 *   <li>{@code moderator} — Moderation permissions</li>
 *   <li>{@code builder} — Build permissions</li>
 *   <li>{@code default} — Basic player permissions</li>
 * </ul>
 *
 * <h2>Permission Nodes:</h2>
 * <p>Supports dot-separated permission nodes with wildcard matching:</p>
 * <ul>
 *   <li>{@code argonath.*} — Matches all argonath permissions</li>
 *   <li>{@code argonath.quest.create} — Matches exactly</li>
 *   <li>{@code *} — Matches everything</li>
 * </ul>
 *
 * @author Argonath Systems Team
 * @version 1.0.0
 * @since 2.1.0
 */
public class ConfigPermissionProvider implements PermissionProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConfigPermissionProvider.class);

    /** Role → Set of permission nodes */
    private final Map<String, Set<String>> rolePermissions = new ConcurrentHashMap<>();

    /** Player UUID → Set of roles */
    private final Map<UUID, Set<String>> playerRoles = new ConcurrentHashMap<>();

    /** Player UUID → Set of individual permission overrides */
    private final Map<UUID, Set<String>> playerPermissions = new ConcurrentHashMap<>();

    public ConfigPermissionProvider() {
        initializeDefaultRoles();
        LOGGER.info("ConfigPermissionProvider initialized with default role hierarchy");
    }

    /**
     * Initialize default role-permission mappings.
     */
    private void initializeDefaultRoles() {
        // Admin: full access
        addRolePermission("admin", "*");

        // Moderator: moderation + read permissions
        addRolePermission("moderator", "argonath.npc.animation.read");
        addRolePermission("moderator", "argonath.npc.animation.trigger");
        addRolePermission("moderator", "argonath.quest.*");
        addRolePermission("moderator", "argonath.world.*");
        addRolePermission("moderator", "argonath.player.teleport");
        addRolePermission("moderator", "argonath.player.info");

        // Builder: build permissions
        addRolePermission("builder", "argonath.world.create");
        addRolePermission("builder", "argonath.world.edit");
        addRolePermission("builder", "argonath.npc.animation.read");

        // Default: basic permissions for all players
        addRolePermission("default", "argonath.quest.accept");
        addRolePermission("default", "argonath.quest.abandon");
        addRolePermission("default", "argonath.quest.view");
        addRolePermission("default", "argonath.player.info.self");
    }

    @Override
    public boolean hasPermission(UUID playerId, String permission) {
        if (playerId == null || permission == null || permission.isBlank()) {
            return false;
        }

        // Check individual player permission overrides first
        Set<String> playerPerms = playerPermissions.get(playerId);
        if (playerPerms != null && matchesAny(playerPerms, permission)) {
            return true;
        }

        // Check role-based permissions
        Set<String> roles = playerRoles.getOrDefault(playerId, Set.of("default"));
        for (String role : roles) {
            Set<String> rolePerms = rolePermissions.get(role);
            if (rolePerms != null && matchesAny(rolePerms, permission)) {
                return true;
            }
        }

        // Default role fallback (everyone has "default" role)
        Set<String> defaultPerms = rolePermissions.get("default");
        if (defaultPerms != null && matchesAny(defaultPerms, permission)) {
            return true;
        }

        LOGGER.debug("Permission denied: player={}, permission={}", playerId, permission);
        return false;
    }

    @Override
    public boolean hasRole(UUID playerId, String role) {
        if (playerId == null || role == null) {
            return false;
        }
        Set<String> roles = playerRoles.get(playerId);
        return roles != null && roles.contains(role.toLowerCase(Locale.ROOT));
    }

    @Override
    public String getPrimaryRole(UUID playerId) {
        if (playerId == null) {
            return "default";
        }
        Set<String> roles = playerRoles.get(playerId);
        if (roles == null || roles.isEmpty()) {
            return "default";
        }
        // Priority order: admin > moderator > builder > default
        if (roles.contains("admin")) return "admin";
        if (roles.contains("moderator")) return "moderator";
        if (roles.contains("builder")) return "builder";
        return roles.iterator().next();
    }

    // ========================================================================
    // Configuration API
    // ========================================================================

    /**
     * Add a permission to a role.
     *
     * @param role The role name (will be lowercased)
     * @param permission The permission node
     */
    public void addRolePermission(String role, String permission) {
        rolePermissions
            .computeIfAbsent(role.toLowerCase(Locale.ROOT), k -> ConcurrentHashMap.newKeySet())
            .add(permission.toLowerCase(Locale.ROOT));
    }

    /**
     * Assign a role to a player.
     *
     * @param playerId The player's UUID
     * @param role The role to assign
     */
    public void addPlayerRole(UUID playerId, String role) {
        playerRoles
            .computeIfAbsent(playerId, k -> ConcurrentHashMap.newKeySet())
            .add(role.toLowerCase(Locale.ROOT));
        LOGGER.debug("Assigned role '{}' to player {}", role, playerId);
    }

    /**
     * Remove a role from a player.
     *
     * @param playerId The player's UUID
     * @param role The role to remove
     */
    public void removePlayerRole(UUID playerId, String role) {
        Set<String> roles = playerRoles.get(playerId);
        if (roles != null) {
            roles.remove(role.toLowerCase(Locale.ROOT));
        }
    }

    /**
     * Grant an individual permission to a player (override).
     *
     * @param playerId The player's UUID
     * @param permission The permission node
     */
    public void grantPermission(UUID playerId, String permission) {
        playerPermissions
            .computeIfAbsent(playerId, k -> ConcurrentHashMap.newKeySet())
            .add(permission.toLowerCase(Locale.ROOT));
    }

    /**
     * Revoke an individual permission from a player.
     *
     * @param playerId The player's UUID
     * @param permission The permission to revoke
     */
    public void revokePermission(UUID playerId, String permission) {
        Set<String> perms = playerPermissions.get(playerId);
        if (perms != null) {
            perms.remove(permission.toLowerCase(Locale.ROOT));
        }
    }

    /**
     * Clean up all data for a player (call on disconnect).
     *
     * @param playerId The player's UUID
     */
    public void cleanupPlayer(UUID playerId) {
        // Don't remove roles — they persist across sessions
        // Only remove transient permission overrides if desired
    }

    // ========================================================================
    // Wildcard Matching
    // ========================================================================

    /**
     * Check if any permission in the set matches the requested permission.
     * Supports wildcard matching with '*'.
     */
    private boolean matchesAny(Set<String> grantedPermissions, String requested) {
        String requestedLower = requested.toLowerCase(Locale.ROOT);
        for (String granted : grantedPermissions) {
            if (matchesPermission(granted, requestedLower)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Check if a granted permission matches a requested permission.
     *
     * <p>Rules:</p>
     * <ul>
     *   <li>{@code *} matches everything</li>
     *   <li>{@code argonath.*} matches {@code argonath.quest.create}</li>
     *   <li>{@code argonath.quest.*} matches {@code argonath.quest.create} but not {@code argonath.npc.read}</li>
     *   <li>Exact match: {@code argonath.quest.create} matches {@code argonath.quest.create}</li>
     * </ul>
     */
    private boolean matchesPermission(String granted, String requested) {
        if ("*".equals(granted)) {
            return true;
        }
        if (granted.equals(requested)) {
            return true;
        }
        if (granted.endsWith(".*")) {
            String prefix = granted.substring(0, granted.length() - 1); // "argonath.quest."
            return requested.startsWith(prefix);
        }
        return false;
    }
}
