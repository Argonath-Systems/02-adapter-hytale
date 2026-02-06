package com.argonathsystems.adapter.hytale.ecs;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentRegistryProxy;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Central registry for Argonath ECS components.
 * 
 * <p>This registry manages all Argonath player data components that wrap
 * platform-agnostic POJOs for persistence through Hytale's EntityStore system.
 * 
 * <h2>Usage</h2>
 * <p>Initialize in your plugin's {@code setup()} method:
 * <pre>{@code
 * @Override
 * protected void setup() {
 *     ArgonathComponentRegistry.initializeAll(getEntityStoreRegistry());
 * }
 * }</pre>
 * 
 * <p>Then access component types via static accessors:
 * <pre>{@code
 * ArgonathPlayerStatsComponent stats = store.ensureAndGetComponent(
 *     ref, ArgonathComponentRegistry.playerStats()
 * );
 * }</pre>
 * 
 * <h2>Specification</h2>
 * <p>SF-ARCHITECTURE-028-ecs-persistence-bridge, Section 4.2
 * 
 * @author Argonath Systems Team
 * @version 1.0.0
 * @since 4.0.0
 */
public final class ArgonathComponentRegistry {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(ArgonathComponentRegistry.class);
    
    // Component types storage
    private static final Map<Class<?>, ComponentType<EntityStore, ?>> COMPONENT_TYPES = 
        new ConcurrentHashMap<>();
    
    // Pre-defined component types for static access
    private static ComponentType<EntityStore, ArgonathPlayerStatsComponent> PLAYER_STATS;
    private static ComponentType<EntityStore, ArgonathMountCollectionComponent> MOUNT_COLLECTION;
    private static ComponentType<EntityStore, ArgonathNPCRelationshipComponent> NPC_RELATIONSHIP;
    private static ComponentType<EntityStore, ArgonathCombatStatsComponent> COMBAT_STATS;
    private static ComponentType<EntityStore, ArgonathGuildMembershipComponent> GUILD_MEMBERSHIP;
    
    // Initialization flag
    private static volatile boolean initialized = false;
    
    private ArgonathComponentRegistry() {
        // Prevent instantiation
    }

    // ========== Registration Methods ==========
    
    /**
     * Register a component type with Hytale's ComponentRegistryProxy.
     * 
     * @param registry The ComponentRegistryProxy from the plugin
     * @param name Unique name for the component (e.g., "ArgonathPlayerStats")
     * @param componentClass The component class
     * @param codec BuilderCodec for BSON serialization
     * @param <T> Component type
     * @return The registered ComponentType
     */
    public static <T extends Component<EntityStore>> ComponentType<EntityStore, T> register(
            ComponentRegistryProxy<EntityStore> registry,
            String name,
            Class<T> componentClass,
            BuilderCodec<T> codec
    ) {
        ComponentType<EntityStore, T> type = registry.registerComponent(
            componentClass,
            name,
            codec
        );
        COMPONENT_TYPES.put(componentClass, type);
        LOGGER.info("Registered Argonath component: {} → {}", name, componentClass.getSimpleName());
        return type;
    }
    
    /**
     * Get a component type by class.
     * 
     * @param componentClass The component class
     * @param <T> Component type
     * @return The ComponentType, or null if not registered
     */
    @SuppressWarnings("unchecked")
    public static <T extends Component<EntityStore>> ComponentType<EntityStore, T> get(Class<T> componentClass) {
        return (ComponentType<EntityStore, T>) COMPONENT_TYPES.get(componentClass);
    }

    // ========== Initialization ==========
    
    /**
     * Initialize all Argonath ECS components.
     * 
     * <p>Call this from your plugin's {@code setup()} method:
     * <pre>{@code
     * ArgonathComponentRegistry.initializeAll(getEntityStoreRegistry());
     * }</pre>
     * 
     * @param registry The ComponentRegistryProxy from JavaPlugin
     */
    public static synchronized void initializeAll(ComponentRegistryProxy<EntityStore> registry) {
        if (initialized) {
            LOGGER.warn("ArgonathComponentRegistry already initialized - skipping");
            return;
        }
        
        LOGGER.info("Initializing Argonath ECS components...");
        
        // Player Stats
        PLAYER_STATS = register(
            registry, 
            "ArgonathPlayerStats", 
            ArgonathPlayerStatsComponent.class, 
            ArgonathPlayerStatsComponent.CODEC
        );
        
        // Mount Collection
        MOUNT_COLLECTION = register(
            registry,
            "ArgonathMountCollection",
            ArgonathMountCollectionComponent.class,
            ArgonathMountCollectionComponent.CODEC
        );
        
        // NPC Relationships
        NPC_RELATIONSHIP = register(
            registry,
            "ArgonathNPCRelationship",
            ArgonathNPCRelationshipComponent.class,
            ArgonathNPCRelationshipComponent.CODEC
        );
        
        // Combat Stats
        COMBAT_STATS = register(
            registry,
            "ArgonathCombatStats",
            ArgonathCombatStatsComponent.class,
            ArgonathCombatStatsComponent.CODEC
        );
        
        // Guild Membership
        GUILD_MEMBERSHIP = register(
            registry,
            "ArgonathGuildMembership",
            ArgonathGuildMembershipComponent.class,
            ArgonathGuildMembershipComponent.CODEC
        );
        
        initialized = true;
        LOGGER.info("Argonath ECS components initialized: {} types registered", COMPONENT_TYPES.size());
    }
    
    /**
     * Check if registry has been initialized.
     */
    public static boolean isInitialized() {
        return initialized;
    }

    // ========== Static Component Type Accessors ==========
    
    /**
     * Get the PlayerStats component type.
     * 
     * @return ComponentType for player statistics
     * @throws IllegalStateException if registry not initialized
     */
    public static ComponentType<EntityStore, ArgonathPlayerStatsComponent> playerStats() {
        ensureInitialized();
        return PLAYER_STATS;
    }
    
    /**
     * Get the MountCollection component type.
     * 
     * @return ComponentType for mount collection
     * @throws IllegalStateException if registry not initialized
     */
    public static ComponentType<EntityStore, ArgonathMountCollectionComponent> mountCollection() {
        ensureInitialized();
        return MOUNT_COLLECTION;
    }
    
    /**
     * Get the NPCRelationship component type.
     * 
     * @return ComponentType for NPC relationships
     * @throws IllegalStateException if registry not initialized
     */
    public static ComponentType<EntityStore, ArgonathNPCRelationshipComponent> npcRelationship() {
        ensureInitialized();
        return NPC_RELATIONSHIP;
    }
    
    /**
     * Get the CombatStats component type.
     * 
     * @return ComponentType for combat statistics
     * @throws IllegalStateException if registry not initialized
     */
    public static ComponentType<EntityStore, ArgonathCombatStatsComponent> combatStats() {
        ensureInitialized();
        return COMBAT_STATS;
    }
    
    /**
     * Get the GuildMembership component type.
     * 
     * @return ComponentType for guild membership
     * @throws IllegalStateException if registry not initialized
     */
    public static ComponentType<EntityStore, ArgonathGuildMembershipComponent> guildMembership() {
        ensureInitialized();
        return GUILD_MEMBERSHIP;
    }

    // ========== Internal Helpers ==========
    
    private static void ensureInitialized() {
        if (!initialized) {
            throw new IllegalStateException(
                "ArgonathComponentRegistry not initialized. " +
                "Call ArgonathComponentRegistry.initializeAll(registry) in your plugin's setup() method."
            );
        }
    }
    
    /**
     * Reset the registry (for testing only).
     */
    static void reset() {
        COMPONENT_TYPES.clear();
        PLAYER_STATS = null;
        MOUNT_COLLECTION = null;
        NPC_RELATIONSHIP = null;
        COMBAT_STATS = null;
        GUILD_MEMBERSHIP = null;
        initialized = false;
        LOGGER.debug("ArgonathComponentRegistry reset");
    }
}
