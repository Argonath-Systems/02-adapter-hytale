package com.argonathsystems.adapter.hytale.questdesigner;

import com.argonathsystems.mod.questdesigner.accessor.HytaleRegistryAccessor;
import com.argonathsystems.mod.questdesigner.accessor.RegistryEntryDTO;

import java.lang.reflect.Method;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Hytale implementation of {@link HytaleRegistryAccessor}.
 * 
 * <p>Provides access to Hytale's game registries for items, entities, NPCs, and locations.
 * This implementation queries the real Hytale registry APIs and converts results to
 * platform-agnostic {@link RegistryEntryDTO} objects.
 * 
 * <p><b>Note:</b> This class is the ONLY place in quest-designer that imports Hytale registry APIs.
 * 
 * @author Argonath Systems
 * @version 1.0.0
 * @since 1.0.0
 */
public class HytaleRegistryAccessorImpl implements HytaleRegistryAccessor {

    private static final Logger LOGGER = Logger.getLogger(HytaleRegistryAccessorImpl.class.getName());

    // Cache for registry entries (1 minute TTL)
    private volatile List<RegistryEntryDTO> cachedItems = null;
    private volatile List<RegistryEntryDTO> cachedEntities = null;
    private volatile List<RegistryEntryDTO> cachedNPCs = null;
    private volatile List<RegistryEntryDTO> cachedLocations = null;
    private volatile long cacheTimestamp = 0;
    private static final long CACHE_TTL_MS = 60_000; // 1 minute

    /** Whether dynamic SDK registry lookup is available */
    private volatile boolean sdkRegistryAvailable = false;

    /**
     * Creates a new Hytale registry accessor.
     * 
     * <p>Attempts to use dynamic SDK registry lookup via {@code AssetStore.getAssetMap()}.
     * Falls back to stub data if SDK asset store classes are not available at runtime.</p>
     */
    public HytaleRegistryAccessorImpl() {
        // Probe for SDK AssetStore availability at construction time
        sdkRegistryAvailable = probeAssetStoreAvailability();
        if (sdkRegistryAvailable) {
            LOGGER.log(Level.INFO, "Initialized Hytale Registry Accessor (SDK dynamic mode)");
        } else {
            LOGGER.log(Level.INFO, "Initialized Hytale Registry Accessor (stub fallback mode)");
        }
    }

    /**
     * Probes whether the SDK AssetStore API is available at runtime.
     * Uses reflection to avoid hard dependency on asset store classes
     * whose exact package/signature may change between SDK versions.
     */
    private boolean probeAssetStoreAvailability() {
        try {
            // Try to locate the HytaleServer registry entry point
            Class<?> serverClass = Class.forName("com.hypixel.hytale.server.core.HytaleServer");
            Method getInstance = serverClass.getMethod("get");
            Object serverInstance = getInstance.invoke(null);
            if (serverInstance == null) {
                return false;
            }
            // Check if getRegistry() is available
            Method getRegistry = serverClass.getMethod("getRegistry");
            Object registry = getRegistry.invoke(serverInstance);
            return registry != null;
        } catch (Exception e) {
            LOGGER.log(Level.FINE, "SDK AssetStore not available: {0}", e.getMessage());
            return false;
        }
    }

    @Override
    public List<RegistryEntryDTO> getItems() {
        refreshCacheIfNeeded();
        return cachedItems != null ? Collections.unmodifiableList(cachedItems) : loadItems();
    }

    @Override
    public List<RegistryEntryDTO> getEntityTypes() {
        refreshCacheIfNeeded();
        return cachedEntities != null ? Collections.unmodifiableList(cachedEntities) : loadEntities();
    }

    @Override
    public List<RegistryEntryDTO> getNPCs() {
        refreshCacheIfNeeded();
        return cachedNPCs != null ? Collections.unmodifiableList(cachedNPCs) : loadNPCs();
    }

    @Override
    public List<RegistryEntryDTO> getLocations() {
        refreshCacheIfNeeded();
        return cachedLocations != null ? Collections.unmodifiableList(cachedLocations) : loadLocations();
    }

    @Override
    public List<RegistryEntryDTO> search(String query, String type) {
        if (query == null || query.isBlank()) {
            return Collections.emptyList();
        }

        String lowercaseQuery = query.toLowerCase(Locale.ROOT);
        List<RegistryEntryDTO> searchPool;

        // Select pool based on type
        if (type == null || type.equalsIgnoreCase("all")) {
            searchPool = new ArrayList<>();
            searchPool.addAll(getItems());
            searchPool.addAll(getEntityTypes());
            searchPool.addAll(getNPCs());
            searchPool.addAll(getLocations());
        } else {
            searchPool = switch (type.toLowerCase(Locale.ROOT)) {
                case "items" -> getItems();
                case "entities" -> getEntityTypes();
                case "npcs" -> getNPCs();
                case "locations" -> getLocations();
                default -> Collections.emptyList();
            };
        }

        // Filter by query
        return searchPool.stream()
            .filter(entry -> 
                entry.getId().toLowerCase(Locale.ROOT).contains(lowercaseQuery) ||
                entry.getName().toLowerCase(Locale.ROOT).contains(lowercaseQuery) ||
                entry.getDisplayName().toLowerCase(Locale.ROOT).contains(lowercaseQuery)
            )
            .collect(Collectors.toList());
    }

    private void refreshCacheIfNeeded() {
        long now = System.currentTimeMillis();
        if (now - cacheTimestamp > CACHE_TTL_MS) {
            synchronized (this) {
                if (now - cacheTimestamp > CACHE_TTL_MS) {
                    LOGGER.log(Level.FINE, "Refreshing registry cache");
                    cachedItems = loadItems();
                    cachedEntities = loadEntities();
                    cachedNPCs = loadNPCs();
                    cachedLocations = loadLocations();
                    cacheTimestamp = now;
                }
            }
        }
    }

    private List<RegistryEntryDTO> loadItems() {
        if (sdkRegistryAvailable) {
            List<RegistryEntryDTO> dynamicItems = loadFromAssetStore("ItemType", "items");
            if (!dynamicItems.isEmpty()) {
                LOGGER.log(Level.INFO, "Loaded {0} items from SDK AssetStore", dynamicItems.size());
                return dynamicItems;
            }
        }
        
        // Fallback stub data — used when SDK AssetStore is unavailable or returns empty
        List<RegistryEntryDTO> items = new ArrayList<>();
        items.add(new RegistryEntryDTO("hytale:sword_iron", "sword_iron", "Iron Sword", "textures/items/sword_iron.png", "weapons"));
        items.add(new RegistryEntryDTO("hytale:sword_steel", "sword_steel", "Steel Sword", "textures/items/sword_steel.png", "weapons"));
        items.add(new RegistryEntryDTO("hytale:bow_hunter", "bow_hunter", "Hunter's Bow", "textures/items/bow_hunter.png", "weapons"));
        items.add(new RegistryEntryDTO("hytale:staff_arcane", "staff_arcane", "Arcane Staff", "textures/items/staff_arcane.png", "weapons"));
        items.add(new RegistryEntryDTO("hytale:potion_health", "potion_health", "Health Potion", "textures/items/potion_health.png", "consumables"));
        items.add(new RegistryEntryDTO("hytale:potion_mana", "potion_mana", "Mana Potion", "textures/items/potion_mana.png", "consumables"));
        items.add(new RegistryEntryDTO("hytale:ore_iron", "ore_iron", "Iron Ore", "textures/items/ore_iron.png", "materials"));
        items.add(new RegistryEntryDTO("hytale:ore_gold", "ore_gold", "Gold Ore", "textures/items/ore_gold.png", "materials"));
        items.add(new RegistryEntryDTO("hytale:gem_ruby", "gem_ruby", "Ruby", "textures/items/gem_ruby.png", "materials"));
        items.add(new RegistryEntryDTO("hytale:armor_plate", "armor_plate", "Plate Armor", "textures/items/armor_plate.png", "armor"));
        
        LOGGER.log(Level.FINE, "Loaded {0} items from stub registry", items.size());
        return items;
    }

    private List<RegistryEntryDTO> loadEntities() {
        if (sdkRegistryAvailable) {
            List<RegistryEntryDTO> dynamicEntities = loadFromAssetStore("EntityType", "entities");
            if (!dynamicEntities.isEmpty()) {
                LOGGER.log(Level.INFO, "Loaded {0} entities from SDK AssetStore", dynamicEntities.size());
                return dynamicEntities;
            }
        }
        
        // Fallback stub data — used when SDK AssetStore is unavailable or returns empty
        List<RegistryEntryDTO> entities = new ArrayList<>();
        entities.add(new RegistryEntryDTO("hytale:kweebec", "kweebec", "Kweebec", "textures/entities/kweebec_icon.png", "creatures"));
        entities.add(new RegistryEntryDTO("hytale:trork", "trork", "Trork", "textures/entities/trork_icon.png", "hostile"));
        entities.add(new RegistryEntryDTO("hytale:varyn_minion", "varyn_minion", "Varyn Minion", "textures/entities/varyn_minion_icon.png", "hostile"));
        entities.add(new RegistryEntryDTO("hytale:fen_stalker", "fen_stalker", "Fen Stalker", "textures/entities/fen_stalker_icon.png", "hostile"));
        entities.add(new RegistryEntryDTO("hytale:pteropod", "pteropod", "Pteropod", "textures/entities/pteropod_icon.png", "creatures"));
        entities.add(new RegistryEntryDTO("hytale:ramhorn", "ramhorn", "Ramhorn", "textures/entities/ramhorn_icon.png", "creatures"));
        entities.add(new RegistryEntryDTO("hytale:sabretusk", "sabretusk", "Sabretusk", "textures/entities/sabretusk_icon.png", "creatures"));
        entities.add(new RegistryEntryDTO("hytale:undead_skeleton", "undead_skeleton", "Undead Skeleton", "textures/entities/undead_skeleton_icon.png", "undead"));
        
        LOGGER.log(Level.FINE, "Loaded {0} entities from stub registry", entities.size());
        return entities;
    }

    private List<RegistryEntryDTO> loadNPCs() {
        if (sdkRegistryAvailable) {
            // NPCs may be a subset of EntityType filtered by category,
            // or a separate NpcType asset store. Try both patterns.
            List<RegistryEntryDTO> dynamicNpcs = loadFromAssetStore("NpcType", "npcs");
            if (dynamicNpcs.isEmpty()) {
                // Fallback: filter entities by NPC-like categories
                dynamicNpcs = loadEntities().stream()
                    .filter(e -> "npc".equalsIgnoreCase(e.getCategory()) 
                              || "villagers".equalsIgnoreCase(e.getCategory())
                              || "quest".equalsIgnoreCase(e.getCategory()))
                    .collect(Collectors.toList());
            }
            if (!dynamicNpcs.isEmpty()) {
                LOGGER.log(Level.INFO, "Loaded {0} NPCs from SDK", dynamicNpcs.size());
                return dynamicNpcs;
            }
        }
        
        // Fallback stub data — used when SDK NPC asset store is unavailable
        List<RegistryEntryDTO> npcs = new ArrayList<>();
        npcs.add(new RegistryEntryDTO("hytale:villager_blacksmith", "villager_blacksmith", "Village Blacksmith", "textures/npcs/villager_blacksmith.png", "villagers"));
        npcs.add(new RegistryEntryDTO("hytale:villager_merchant", "villager_merchant", "Village Merchant", "textures/npcs/villager_merchant.png", "villagers"));
        npcs.add(new RegistryEntryDTO("hytale:villager_elder", "villager_elder", "Village Elder", "textures/npcs/villager_elder.png", "villagers"));
        npcs.add(new RegistryEntryDTO("hytale:guild_master", "guild_master", "Guild Master", "textures/npcs/guild_master.png", "guild"));
        npcs.add(new RegistryEntryDTO("hytale:quest_giver_main", "quest_giver_main", "Quest Giver", "textures/npcs/quest_giver.png", "quest"));
        npcs.add(new RegistryEntryDTO("hytale:trainer_combat", "trainer_combat", "Combat Trainer", "textures/npcs/trainer_combat.png", "trainers"));
        npcs.add(new RegistryEntryDTO("hytale:trainer_magic", "trainer_magic", "Magic Trainer", "textures/npcs/trainer_magic.png", "trainers"));
        
        LOGGER.log(Level.FINE, "Loaded {0} NPCs from stub registry", npcs.size());
        return npcs;
    }

    private List<RegistryEntryDTO> loadLocations() {
        if (sdkRegistryAvailable) {
            // Locations can come from Biome asset store or zone/world system
            List<RegistryEntryDTO> dynamicLocations = loadFromAssetStore("Biome", "biome");
            if (!dynamicLocations.isEmpty()) {
                LOGGER.log(Level.INFO, "Loaded {0} locations from SDK AssetStore", dynamicLocations.size());
                return dynamicLocations;
            }
            
            // Alternative: Discover worlds from Universe as location entries
            dynamicLocations = loadWorldsAsLocations();
            if (!dynamicLocations.isEmpty()) {
                LOGGER.log(Level.INFO, "Loaded {0} locations from Universe worlds", dynamicLocations.size());
                return dynamicLocations;
            }
        }
        
        // Fallback stub data — used when SDK zone/location system is unavailable
        List<RegistryEntryDTO> locations = new ArrayList<>();
        locations.add(new RegistryEntryDTO("hytale:zone_orbis", "zone_orbis", "Orbis", "textures/locations/zone_orbis.png", "zones"));
        locations.add(new RegistryEntryDTO("hytale:zone_emerald_grove", "zone_emerald_grove", "Emerald Grove", "textures/locations/zone_emerald_grove.png", "zones"));
        locations.add(new RegistryEntryDTO("hytale:zone_howling_sands", "zone_howling_sands", "Howling Sands", "textures/locations/zone_howling_sands.png", "zones"));
        locations.add(new RegistryEntryDTO("hytale:zone_frozen_wastes", "zone_frozen_wastes", "Frozen Wastes", "textures/locations/zone_frozen_wastes.png", "zones"));
        locations.add(new RegistryEntryDTO("hytale:poi_village_start", "poi_village_start", "Starting Village", "textures/locations/poi_village.png", "poi"));
        locations.add(new RegistryEntryDTO("hytale:poi_dungeon_crypt", "poi_dungeon_crypt", "Ancient Crypt", "textures/locations/poi_dungeon.png", "poi"));
        locations.add(new RegistryEntryDTO("hytale:poi_tower_wizard", "poi_tower_wizard", "Wizard's Tower", "textures/locations/poi_tower.png", "poi"));
        
        LOGGER.log(Level.FINE, "Loaded {0} locations from stub registry", locations.size());
        return locations;
    }

    // ============================================================
    // SDK Dynamic Registry Lookup
    // ============================================================

    /**
     * Generic reflection-based loader for SDK AssetStore entries.
     * 
     * <p>Attempts to load registry entries from the Hytale SDK using:
     * {@code Class.forName("...assetType").getMethod("getAssetStore").invoke()}</p>
     * 
     * <p>The SDK AssetStore pattern is: each asset type has a static {@code getAssetStore()}
     * method returning an {@code AssetStore<K, V, ?>} with a {@code getAssetMap()} returning
     * {@code Map<K, V>}. Each entry's key is used as the registry ID, and the asset's
     * toString/getId/getDisplayName methods are probed via reflection.</p>
     * 
     * @param assetTypeName Simple class name of the SDK asset type (e.g. "ItemType", "EntityType")
     * @param category Default category to assign if the asset has no category method
     * @return List of registry entries, or empty list if lookup fails
     */
    @SuppressWarnings("unchecked")
    private List<RegistryEntryDTO> loadFromAssetStore(String assetTypeName, String category) {
        List<RegistryEntryDTO> entries = new ArrayList<>();
        
        // Known SDK package paths to search for asset types
        String[] packagePrefixes = {
            "com.hypixel.hytale.server.core.registry.",
            "com.hypixel.hytale.server.core.inventory.",
            "com.hypixel.hytale.server.core.entity.",
            "com.hypixel.hytale.server.core.modules.entity.",
            "com.hypixel.hytale.server.core.universe.world.",
            "com.hypixel.hytale.server.core.",
            "com.hypixel.hytale.server.npc."
        };
        
        Class<?> assetClass = null;
        for (String prefix : packagePrefixes) {
            try {
                assetClass = Class.forName(prefix + assetTypeName);
                break;
            } catch (ClassNotFoundException ignored) {
                // Try next package prefix
            }
        }
        
        if (assetClass == null) {
            LOGGER.log(Level.FINE, "SDK asset type not found: {0}", assetTypeName);
            return entries;
        }
        
        try {
            // Call static getAssetStore() on the asset type class
            Method getAssetStore = assetClass.getMethod("getAssetStore");
            Object assetStore = getAssetStore.invoke(null);
            if (assetStore == null) {
                return entries;
            }
            
            // Call getAssetMap() on the AssetStore instance
            Method getAssetMap = assetStore.getClass().getMethod("getAssetMap");
            Object assetMap = getAssetMap.invoke(assetStore);
            if (!(assetMap instanceof Map<?, ?> map)) {
                return entries;
            }
            
            // Iterate entries and extract registry DTOs
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                try {
                    String key = entry.getKey().toString();
                    Object asset = entry.getValue();
                    
                    String id = "hytale:" + key;
                    String name = key;
                    String displayName = key;
                    String assetCategory = category;
                    
                    // Probe for getId()
                    try {
                        Method getId = asset.getClass().getMethod("getId");
                        Object idObj = getId.invoke(asset);
                        if (idObj != null) {
                            name = idObj.toString();
                        }
                    } catch (NoSuchMethodException ignored) {}
                    
                    // Probe for getDisplayName()
                    try {
                        Method getDisplayName = asset.getClass().getMethod("getDisplayName");
                        Object dnObj = getDisplayName.invoke(asset);
                        if (dnObj != null) {
                            displayName = dnObj.toString();
                        }
                    } catch (NoSuchMethodException ignored) {}
                    
                    // Probe for getCategory()
                    try {
                        Method getCategory = asset.getClass().getMethod("getCategory");
                        Object catObj = getCategory.invoke(asset);
                        if (catObj != null) {
                            assetCategory = catObj.toString().toLowerCase(Locale.ROOT);
                        }
                    } catch (NoSuchMethodException ignored) {}
                    
                    // Build icon path heuristically
                    String iconPath = "textures/" + category + "/" + key + ".png";
                    
                    entries.add(new RegistryEntryDTO(id, name, displayName, iconPath, assetCategory));
                } catch (Exception e) {
                    LOGGER.log(Level.FINE, "Failed to convert asset entry: {0}", e.getMessage());
                }
            }
            
        } catch (NoSuchMethodException e) {
            LOGGER.log(Level.FINE, "Asset type {0} has no getAssetStore() method: {1}", 
                new Object[]{assetTypeName, e.getMessage()});
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to load from AssetStore {0}: {1}",
                new Object[]{assetTypeName, e.getMessage()});
        }
        
        return entries;
    }

    /**
     * Loads worlds from the Hytale Universe as location entries.
     * This provides dynamically discovered worlds/realms as searchable locations.
     */
    private List<RegistryEntryDTO> loadWorldsAsLocations() {
        List<RegistryEntryDTO> locations = new ArrayList<>();
        try {
            Class<?> universeClass = Class.forName("com.hypixel.hytale.server.core.universe.Universe");
            Method getMethod = universeClass.getMethod("get");
            Object universe = getMethod.invoke(null);
            if (universe == null) {
                return locations;
            }
            
            Method getWorlds = universeClass.getMethod("getWorlds");
            Object worldsObj = getWorlds.invoke(universe);
            if (worldsObj instanceof Map<?, ?> worldsMap) {
                for (Map.Entry<?, ?> entry : worldsMap.entrySet()) {
                    String worldName = entry.getKey().toString();
                    String id = "hytale:world_" + worldName;
                    locations.add(new RegistryEntryDTO(
                        id, worldName, worldName,
                        "textures/locations/world_" + worldName + ".png",
                        "worlds"
                    ));
                }
            }
        } catch (Exception e) {
            LOGGER.log(Level.FINE, "Failed to load worlds as locations: {0}", e.getMessage());
        }
        return locations;
    }

    /**
     * Forces a cache refresh.
     */
    public void invalidateCache() {
        synchronized (this) {
            cacheTimestamp = 0;
        }
    }
}
