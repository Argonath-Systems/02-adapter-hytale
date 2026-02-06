package com.argonathsystems.adapter.hytale.ecs;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Supplier;

/**
 * Synchronization service for bridging Argonath POJO data with Hytale ECS components.
 * 
 * <p>This service manages the lifecycle of player data:
 * <ol>
 *   <li><b>Player Join</b>: Load from EntityStore → populate POJOs for business logic</li>
 *   <li><b>During Session</b>: Business logic updates POJOs freely</li>
 *   <li><b>Player Quit</b>: Sync POJO changes back to ECS components → auto-persisted</li>
 * </ol>
 * 
 * <h2>Usage</h2>
 * <pre>{@code
 * // In PlayerReadyEvent handler
 * Ref<EntityStore> ref = event.getPlayerRef();
 * Store<EntityStore> store = ref.getStore();
 * ArgonathComponentSyncService.onPlayerJoin(store, ref, playerRef);
 * 
 * // In PlayerDisconnectEvent handler
 * ArgonathComponentSyncService.onPlayerQuit(playerId);
 * }</pre>
 * 
 * <h2>Specification</h2>
 * <p>SF-ARCHITECTURE-028-ecs-persistence-bridge, Section 4.3
 * 
 * @author Argonath Systems Team
 * @version 1.0.0
 * @since 4.0.0
 */
public final class ArgonathComponentSyncService {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(ArgonathComponentSyncService.class);
    
    // Track active player sessions for cleanup
    private static final ConcurrentMap<UUID, PlayerSession> ACTIVE_SESSIONS = new ConcurrentHashMap<>();
    
    // POJO providers (registered by framework modules)
    private static Supplier<PlayerStatsDataProvider> statsProvider;
    private static Supplier<MountCollectionProvider> mountProvider;
    private static Supplier<NPCRelationshipProvider> npcProvider;
    private static Supplier<CombatStatsProvider> combatProvider;
    private static Supplier<GuildMembershipProvider> guildProvider;
    
    private ArgonathComponentSyncService() {
        // Prevent instantiation
    }

    // ========== Provider Registration ==========
    
    /**
     * Register the player stats data provider (from 04-framework-stats).
     */
    public static void registerStatsProvider(Supplier<PlayerStatsDataProvider> provider) {
        statsProvider = Objects.requireNonNull(provider, "Stats provider cannot be null");
        LOGGER.info("Registered PlayerStatsDataProvider");
    }
    
    /**
     * Register the mount collection provider (from 06-mod-mounts).
     */
    public static void registerMountProvider(Supplier<MountCollectionProvider> provider) {
        mountProvider = Objects.requireNonNull(provider, "Mount provider cannot be null");
        LOGGER.info("Registered MountCollectionProvider");
    }
    
    /**
     * Register the NPC relationship provider (from 04-framework-npc).
     */
    public static void registerNPCProvider(Supplier<NPCRelationshipProvider> provider) {
        npcProvider = Objects.requireNonNull(provider, "NPC provider cannot be null");
        LOGGER.info("Registered NPCRelationshipProvider");
    }
    
    /**
     * Register the combat stats provider (from 06-mod-combat).
     */
    public static void registerCombatProvider(Supplier<CombatStatsProvider> provider) {
        combatProvider = Objects.requireNonNull(provider, "Combat provider cannot be null");
        LOGGER.info("Registered CombatStatsProvider");
    }
    
    /**
     * Register the guild membership provider (from 06-mod-guilds).
     */
    public static void registerGuildProvider(Supplier<GuildMembershipProvider> provider) {
        guildProvider = Objects.requireNonNull(provider, "Guild provider cannot be null");
        LOGGER.info("Registered GuildMembershipProvider");
    }

    // ========== Player Lifecycle ==========
    
    /**
     * Handle player join: load data from ECS components and populate POJOs.
     * 
     * <p>This method should be called from the PlayerReadyEvent handler after
     * obtaining the store and ref from the event.</p>
     * 
     * @param store The Store containing the player's EntityStore
     * @param ref The Ref to the player's EntityStore
     * @param playerRef The PlayerRef for the player
     */
    public static void onPlayerJoin(Store<EntityStore> store, Ref<EntityStore> ref, PlayerRef playerRef) {
        UUID playerId = playerRef.getUuid();
        LOGGER.debug("Player join sync started: {}", playerId);
        
        try {
            // Create session tracker
            PlayerSession session = new PlayerSession(playerId, store, ref, playerRef);
            ACTIVE_SESSIONS.put(playerId, session);
            
            // Load stats component → POJO
            if (statsProvider != null) {
                loadStatsComponent(store, ref, playerId, session);
            }
            
            // Load mount collection component → POJO
            if (mountProvider != null) {
                loadMountComponent(store, ref, playerId, session);
            }
            
            // Load NPC relationship component → POJO
            if (npcProvider != null) {
                loadNPCComponent(store, ref, playerId, session);
            }
            
            // Load combat stats component → POJO
            if (combatProvider != null) {
                loadCombatComponent(store, ref, playerId, session);
            }
            
            // Load guild membership component → POJO
            if (guildProvider != null) {
                loadGuildComponent(store, ref, playerId, session);
            }
            
            LOGGER.info("Player join sync completed: {} ({} components)", playerId, session.loadedComponents);
            
        } catch (Exception e) {
            LOGGER.error("Failed to sync player data on join: {}", playerId, e);
            // Remove partial session
            ACTIVE_SESSIONS.remove(playerId);
        }
    }
    
    /**
     * Handle player quit: sync POJO changes back to ECS components.
     * 
     * @param playerId The UUID of the player
     */
    public static void onPlayerQuit(UUID playerId) {
        LOGGER.debug("Player quit sync started: {}", playerId);
        
        PlayerSession session = ACTIVE_SESSIONS.remove(playerId);
        if (session == null) {
            LOGGER.warn("No active session found for player: {}", playerId);
            return;
        }
        
        try {
            Store<EntityStore> store = session.store;
            Ref<EntityStore> ref = session.ref;
            
            if (!ref.isValid()) {
                LOGGER.warn("Player ref no longer valid: {}", playerId);
                return;
            }
            
            int syncedCount = 0;
            
            // Sync stats POJO → component
            if (statsProvider != null && session.statsLoaded) {
                syncStatsComponent(store, ref, playerId);
                syncedCount++;
            }
            
            // Sync mount collection POJO → component
            if (mountProvider != null && session.mountsLoaded) {
                syncMountComponent(store, ref, playerId);
                syncedCount++;
            }
            
            // Sync NPC relationship POJO → component
            if (npcProvider != null && session.npcLoaded) {
                syncNPCComponent(store, ref, playerId);
                syncedCount++;
            }
            
            // Sync combat stats POJO → component
            if (combatProvider != null && session.combatLoaded) {
                syncCombatComponent(store, ref, playerId);
                syncedCount++;
            }
            
            // Sync guild membership POJO → component
            if (guildProvider != null && session.guildLoaded) {
                syncGuildComponent(store, ref, playerId);
                syncedCount++;
            }
            
            LOGGER.info("Player quit sync completed: {} ({} components)", playerId, syncedCount);
            
        } catch (Exception e) {
            LOGGER.error("Failed to sync player data on quit: {}", playerId, e);
        }
    }
    
    /**
     * Force sync all data for an active player.
     * Useful for periodic saves or before server shutdown.
     * 
     * @param playerId The UUID of the player
     */
    public static void forceSync(UUID playerId) {
        PlayerSession session = ACTIVE_SESSIONS.get(playerId);
        
        if (session == null) {
            LOGGER.debug("No active session for force sync: {}", playerId);
            return;
        }
        
        try {
            Store<EntityStore> store = session.store;
            Ref<EntityStore> ref = session.ref;
            
            if (!ref.isValid()) {
                LOGGER.debug("Ref invalid for force sync: {}", playerId);
                return;
            }
            
            // Sync all loaded components
            if (statsProvider != null && session.statsLoaded) syncStatsComponent(store, ref, playerId);
            if (mountProvider != null && session.mountsLoaded) syncMountComponent(store, ref, playerId);
            if (npcProvider != null && session.npcLoaded) syncNPCComponent(store, ref, playerId);
            if (combatProvider != null && session.combatLoaded) syncCombatComponent(store, ref, playerId);
            if (guildProvider != null && session.guildLoaded) syncGuildComponent(store, ref, playerId);
            
            LOGGER.debug("Force sync completed: {}", playerId);
            
        } catch (Exception e) {
            LOGGER.error("Failed to force sync player data: {}", playerId, e);
        }
    }
    
    /**
     * Force sync all active players. 
     * Call this before server shutdown.
     */
    public static void forceSyncAll() {
        LOGGER.info("Force syncing all {} active player sessions...", ACTIVE_SESSIONS.size());
        ACTIVE_SESSIONS.keySet().forEach(playerId -> {
            try {
                forceSync(playerId);
            } catch (Exception e) {
                LOGGER.error("Failed to sync session: {}", playerId, e);
            }
        });
    }

    // ========== Component Loaders ==========
    
    private static void loadStatsComponent(Store<EntityStore> store, Ref<EntityStore> ref, UUID playerId, PlayerSession session) {
        ArgonathPlayerStatsComponent component = store.ensureAndGetComponent(
            ref, ArgonathComponentRegistry.playerStats()
        );
        
        PlayerStatsDataProvider provider = statsProvider.get();
        if (provider != null) {
            provider.loadFromComponent(playerId, component);
            session.statsLoaded = true;
            session.loadedComponents++;
        }
    }
    
    private static void loadMountComponent(Store<EntityStore> store, Ref<EntityStore> ref, UUID playerId, PlayerSession session) {
        ArgonathMountCollectionComponent component = store.ensureAndGetComponent(
            ref, ArgonathComponentRegistry.mountCollection()
        );
        
        MountCollectionProvider provider = mountProvider.get();
        if (provider != null) {
            provider.loadFromComponent(playerId, component);
            session.mountsLoaded = true;
            session.loadedComponents++;
        }
    }
    
    private static void loadNPCComponent(Store<EntityStore> store, Ref<EntityStore> ref, UUID playerId, PlayerSession session) {
        ArgonathNPCRelationshipComponent component = store.ensureAndGetComponent(
            ref, ArgonathComponentRegistry.npcRelationship()
        );
        
        NPCRelationshipProvider provider = npcProvider.get();
        if (provider != null) {
            provider.loadFromComponent(playerId, component);
            session.npcLoaded = true;
            session.loadedComponents++;
        }
    }
    
    private static void loadCombatComponent(Store<EntityStore> store, Ref<EntityStore> ref, UUID playerId, PlayerSession session) {
        ArgonathCombatStatsComponent component = store.ensureAndGetComponent(
            ref, ArgonathComponentRegistry.combatStats()
        );
        
        CombatStatsProvider provider = combatProvider.get();
        if (provider != null) {
            provider.loadFromComponent(playerId, component);
            session.combatLoaded = true;
            session.loadedComponents++;
        }
    }
    
    private static void loadGuildComponent(Store<EntityStore> store, Ref<EntityStore> ref, UUID playerId, PlayerSession session) {
        ArgonathGuildMembershipComponent component = store.ensureAndGetComponent(
            ref, ArgonathComponentRegistry.guildMembership()
        );
        
        GuildMembershipProvider provider = guildProvider.get();
        if (provider != null) {
            provider.loadFromComponent(playerId, component);
            session.guildLoaded = true;
            session.loadedComponents++;
        }
    }

    // ========== Component Syncers ==========
    
    private static void syncStatsComponent(Store<EntityStore> store, Ref<EntityStore> ref, UUID playerId) {
        ArgonathPlayerStatsComponent component = store.getComponent(
            ref, ArgonathComponentRegistry.playerStats()
        );
        
        if (component != null) {
            PlayerStatsDataProvider provider = statsProvider.get();
            if (provider != null) {
                provider.syncToComponent(playerId, component);
            }
        }
    }
    
    private static void syncMountComponent(Store<EntityStore> store, Ref<EntityStore> ref, UUID playerId) {
        ArgonathMountCollectionComponent component = store.getComponent(
            ref, ArgonathComponentRegistry.mountCollection()
        );
        
        if (component != null) {
            MountCollectionProvider provider = mountProvider.get();
            if (provider != null) {
                provider.syncToComponent(playerId, component);
            }
        }
    }
    
    private static void syncNPCComponent(Store<EntityStore> store, Ref<EntityStore> ref, UUID playerId) {
        ArgonathNPCRelationshipComponent component = store.getComponent(
            ref, ArgonathComponentRegistry.npcRelationship()
        );
        
        if (component != null) {
            NPCRelationshipProvider provider = npcProvider.get();
            if (provider != null) {
                provider.syncToComponent(playerId, component);
            }
        }
    }
    
    private static void syncCombatComponent(Store<EntityStore> store, Ref<EntityStore> ref, UUID playerId) {
        ArgonathCombatStatsComponent component = store.getComponent(
            ref, ArgonathComponentRegistry.combatStats()
        );
        
        if (component != null) {
            CombatStatsProvider provider = combatProvider.get();
            if (provider != null) {
                provider.syncToComponent(playerId, component);
            }
        }
    }
    
    private static void syncGuildComponent(Store<EntityStore> store, Ref<EntityStore> ref, UUID playerId) {
        ArgonathGuildMembershipComponent component = store.getComponent(
            ref, ArgonathComponentRegistry.guildMembership()
        );
        
        if (component != null) {
            GuildMembershipProvider provider = guildProvider.get();
            if (provider != null) {
                provider.syncToComponent(playerId, component);
            }
        }
    }

    // ========== Provider Interfaces ==========
    
    /**
     * Provider interface for player stats data sync.
     * Implement in 04-framework-stats adapter layer.
     */
    public interface PlayerStatsDataProvider {
        void loadFromComponent(UUID playerId, ArgonathPlayerStatsComponent component);
        void syncToComponent(UUID playerId, ArgonathPlayerStatsComponent component);
    }
    
    /**
     * Provider interface for mount collection data sync.
     * Implement in 06-mod-mounts adapter layer.
     */
    public interface MountCollectionProvider {
        void loadFromComponent(UUID playerId, ArgonathMountCollectionComponent component);
        void syncToComponent(UUID playerId, ArgonathMountCollectionComponent component);
    }
    
    /**
     * Provider interface for NPC relationship data sync.
     * Implement in 04-framework-npc adapter layer.
     */
    public interface NPCRelationshipProvider {
        void loadFromComponent(UUID playerId, ArgonathNPCRelationshipComponent component);
        void syncToComponent(UUID playerId, ArgonathNPCRelationshipComponent component);
    }
    
    /**
     * Provider interface for combat stats data sync.
     * Implement in 06-mod-combat adapter layer.
     */
    public interface CombatStatsProvider {
        void loadFromComponent(UUID playerId, ArgonathCombatStatsComponent component);
        void syncToComponent(UUID playerId, ArgonathCombatStatsComponent component);
    }
    
    /**
     * Provider interface for guild membership data sync.
     * Implement in 06-mod-guilds adapter layer.
     */
    public interface GuildMembershipProvider {
        void loadFromComponent(UUID playerId, ArgonathGuildMembershipComponent component);
        void syncToComponent(UUID playerId, ArgonathGuildMembershipComponent component);
    }

    // ========== Session Tracking ==========
    
    private static class PlayerSession {
        final UUID playerId;
        final Store<EntityStore> store;
        final Ref<EntityStore> ref;
        final PlayerRef playerRef;
        int loadedComponents = 0;
        boolean statsLoaded = false;
        boolean mountsLoaded = false;
        boolean npcLoaded = false;
        boolean combatLoaded = false;
        boolean guildLoaded = false;
        
        PlayerSession(UUID playerId, Store<EntityStore> store, Ref<EntityStore> ref, PlayerRef playerRef) {
            this.playerId = playerId;
            this.store = store;
            this.ref = ref;
            this.playerRef = playerRef;
        }
    }
    
    /**
     * Get count of active player sessions.
     */
    public static int getActiveSessionCount() {
        return ACTIVE_SESSIONS.size();
    }
    
    /**
     * Reset the service (for testing only).
     */
    static void reset() {
        ACTIVE_SESSIONS.clear();
        statsProvider = null;
        mountProvider = null;
        npcProvider = null;
        combatProvider = null;
        guildProvider = null;
    }
}
