package com.argonathsystems.adapter.hytale.questdesigner;

import com.argonathsystems.mod.questdesigner.accessor.HytaleRegistryAccessor;
import com.argonathsystems.mod.questdesigner.accessor.RegistryEntryDTO;

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

    /**
     * Creates a new Hytale registry accessor.
     * 
     * <p><b>STUB Implementation:</b> Registry lookups use stub data until Hytale SDK
     * provides stable ItemRegistry/EntityRegistry APIs (not currently available).</p>
     */
    public HytaleRegistryAccessorImpl() {
        LOGGER.log(Level.INFO, "Initialized Hytale Registry Accessor (stub mode)");
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
        List<RegistryEntryDTO> items = new ArrayList<>();
        try {
            // TODO: Replace with actual Hytale ItemRegistry API calls when available
            // itemRegistry.getAllItems().forEach(item -> {
            //     items.add(new RegistryEntryDTO(
            //         item.getId().toString(),
            //         item.getId().getPath(),
            //         item.getDisplayName(),
            //         "textures/items/" + item.getId().getPath() + ".png",
            //         item.getCategory()
            //     ));
            // });
            
            // Stub data until Hytale API is finalized
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
            
            LOGGER.log(Level.FINE, "Loaded {0} items from registry", items.size());
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to load items from registry", e);
        }
        return items;
    }

    private List<RegistryEntryDTO> loadEntities() {
        List<RegistryEntryDTO> entities = new ArrayList<>();
        try {
            // TODO: Replace with actual Hytale EntityRegistry API calls when available
            // entityRegistry.getAllEntityTypes().forEach(entityType -> {
            //     entities.add(new RegistryEntryDTO(
            //         entityType.getId().toString(),
            //         entityType.getId().getPath(),
            //         entityType.getDisplayName(),
            //         "textures/entities/" + entityType.getId().getPath() + "_icon.png",
            //         entityType.getCategory()
            //     ));
            // });
            
            // Stub data until Hytale API is finalized
            entities.add(new RegistryEntryDTO("hytale:kweebec", "kweebec", "Kweebec", "textures/entities/kweebec_icon.png", "creatures"));
            entities.add(new RegistryEntryDTO("hytale:trork", "trork", "Trork", "textures/entities/trork_icon.png", "hostile"));
            entities.add(new RegistryEntryDTO("hytale:varyn_minion", "varyn_minion", "Varyn Minion", "textures/entities/varyn_minion_icon.png", "hostile"));
            entities.add(new RegistryEntryDTO("hytale:fen_stalker", "fen_stalker", "Fen Stalker", "textures/entities/fen_stalker_icon.png", "hostile"));
            entities.add(new RegistryEntryDTO("hytale:pteropod", "pteropod", "Pteropod", "textures/entities/pteropod_icon.png", "creatures"));
            entities.add(new RegistryEntryDTO("hytale:ramhorn", "ramhorn", "Ramhorn", "textures/entities/ramhorn_icon.png", "creatures"));
            entities.add(new RegistryEntryDTO("hytale:sabretusk", "sabretusk", "Sabretusk", "textures/entities/sabretusk_icon.png", "creatures"));
            entities.add(new RegistryEntryDTO("hytale:undead_skeleton", "undead_skeleton", "Undead Skeleton", "textures/entities/undead_skeleton_icon.png", "undead"));
            
            LOGGER.log(Level.FINE, "Loaded {0} entities from registry", entities.size());
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to load entities from registry", e);
        }
        return entities;
    }

    private List<RegistryEntryDTO> loadNPCs() {
        List<RegistryEntryDTO> npcs = new ArrayList<>();
        try {
            // TODO: Replace with actual Hytale NPC registry API calls when available
            // For now, NPCs might be a subset of entities or a separate system
            
            // Stub data until Hytale API is finalized
            npcs.add(new RegistryEntryDTO("hytale:villager_blacksmith", "villager_blacksmith", "Village Blacksmith", "textures/npcs/villager_blacksmith.png", "villagers"));
            npcs.add(new RegistryEntryDTO("hytale:villager_merchant", "villager_merchant", "Village Merchant", "textures/npcs/villager_merchant.png", "villagers"));
            npcs.add(new RegistryEntryDTO("hytale:villager_elder", "villager_elder", "Village Elder", "textures/npcs/villager_elder.png", "villagers"));
            npcs.add(new RegistryEntryDTO("hytale:guild_master", "guild_master", "Guild Master", "textures/npcs/guild_master.png", "guild"));
            npcs.add(new RegistryEntryDTO("hytale:quest_giver_main", "quest_giver_main", "Quest Giver", "textures/npcs/quest_giver.png", "quest"));
            npcs.add(new RegistryEntryDTO("hytale:trainer_combat", "trainer_combat", "Combat Trainer", "textures/npcs/trainer_combat.png", "trainers"));
            npcs.add(new RegistryEntryDTO("hytale:trainer_magic", "trainer_magic", "Magic Trainer", "textures/npcs/trainer_magic.png", "trainers"));
            
            LOGGER.log(Level.FINE, "Loaded {0} NPCs from registry", npcs.size());
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to load NPCs from registry", e);
        }
        return npcs;
    }

    private List<RegistryEntryDTO> loadLocations() {
        List<RegistryEntryDTO> locations = new ArrayList<>();
        try {
            // TODO: Replace with actual Hytale location/zone registry API calls when available
            
            // Stub data until Hytale API is finalized
            locations.add(new RegistryEntryDTO("hytale:zone_orbis", "zone_orbis", "Orbis", "textures/locations/zone_orbis.png", "zones"));
            locations.add(new RegistryEntryDTO("hytale:zone_emerald_grove", "zone_emerald_grove", "Emerald Grove", "textures/locations/zone_emerald_grove.png", "zones"));
            locations.add(new RegistryEntryDTO("hytale:zone_howling_sands", "zone_howling_sands", "Howling Sands", "textures/locations/zone_howling_sands.png", "zones"));
            locations.add(new RegistryEntryDTO("hytale:zone_frozen_wastes", "zone_frozen_wastes", "Frozen Wastes", "textures/locations/zone_frozen_wastes.png", "zones"));
            locations.add(new RegistryEntryDTO("hytale:poi_village_start", "poi_village_start", "Starting Village", "textures/locations/poi_village.png", "poi"));
            locations.add(new RegistryEntryDTO("hytale:poi_dungeon_crypt", "poi_dungeon_crypt", "Ancient Crypt", "textures/locations/poi_dungeon.png", "poi"));
            locations.add(new RegistryEntryDTO("hytale:poi_tower_wizard", "poi_tower_wizard", "Wizard's Tower", "textures/locations/poi_tower.png", "poi"));
            
            LOGGER.log(Level.FINE, "Loaded {0} locations from registry", locations.size());
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to load locations from registry", e);
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
