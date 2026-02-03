# Hytale Adapter - Exhaustive Stub Implementation Plan

**Document Version**: 1.0.0  
**Created**: 2026-02-03  
**Module**: `02-adapter-hytale`  
**Estimated Total Effort**: ~120 hours across 9 batches  
**Auditor**: HytaleArchitect

---

## Table of Contents

1. [Executive Summary](#executive-summary)
2. [SDK Reference Index](#sdk-reference-index)
3. [Batch 1: NPC Entity Accessor](#batch-1-npc-entity-accessor)
4. [Batch 2: World Accessor](#batch-2-world-accessor)
5. [Batch 3: UI Accessor](#batch-3-ui-accessor)
6. [Batch 4: Mount System](#batch-4-mount-system)
7. [Batch 5: Storage Accessor](#batch-5-storage-accessor)
8. [Batch 6: Model & Animation Accessor](#batch-6-model--animation-accessor)
9. [Batch 7: Hologram Accessor](#batch-7-hologram-accessor)
10. [Batch 8: Guild Accessor](#batch-8-guild-accessor)
11. [Batch 9: Registry & Quest Designer](#batch-9-registry--quest-designer)
12. [Implementation Checklist](#implementation-checklist)
13. [Risk Assessment](#risk-assessment)

---

## Executive Summary

This document provides exhaustive implementation plans for all remaining stubs and unimplemented methods in the `02-adapter-hytale` module. Each batch includes:

- **SDK Class References** with exact package paths
- **Method Signatures** from SDK javadocs
- **Implementation Patterns** with code samples
- **Verification Criteria** checklists

### Stub Categories Summary

| Batch | Category | Stubs | Effort | Priority |
|-------|----------|-------|--------|----------|
| 1 | NPC Entity Accessor | 12 | 16h | 🔴 Critical |
| 2 | World Accessor | 8 | 14h | 🔴 Critical |
| 3 | UI Accessor | 19 | 20h | 🟡 Medium |
| 4 | Mount System | 6 | 8h | 🟠 High |
| 5 | Storage Accessor | 7 | 10h | 🔴 Critical |
| 6 | Model & Animation | 14 | 16h | 🟡 Medium |
| 7 | Hologram Accessor | 4 | 8h | 🟢 Low |
| 8 | Guild Accessor | 9 | 12h | 🟡 Medium |
| 9 | Registry & Quest Designer | 6 | 6h | 🟢 Low |
| **Total** | | **85** | **~110h** | |

---

## SDK Reference Index

### Core ECS System

| Class | Package | Purpose | Key Methods |
|-------|---------|---------|-------------|
| `World` | `com.hypixel.hytale.server.core.universe.world` | World container | `spawnEntity()`, `getEntity()`, `getEntityRef()`, `getEntityStore()`, `getChunkStore()` |
| `EntityStore` | `com.hypixel.hytale.server.core.universe.world.storage` | Entity ECS store | ECS iteration patterns |
| `ChunkStore` | `com.hypixel.hytale.server.core.universe.world.storage` | Chunk ECS store | Block entity access |
| `Ref<T>` | `com.hypixel.hytale.component` | Entity reference | `get()`, `isValid()` |
| `ComponentAccessor<T>` | `com.hypixel.hytale.component` | Component access | `get()`, `has()`, `add()`, `remove()` |
| `Store<T>` | `com.hypixel.hytale.component` | ECS store | `getResource()`, iteration methods |

### Entity System

| Class | Package | Purpose | Key Methods |
|-------|---------|---------|-------------|
| `Entity` | `com.hypixel.hytale.server.core.entity` | Base entity | `getWorld()`, `remove()`, `getTransformComponent()` |
| `NPCEntity` | `com.hypixel.hytale.server.npc.entities` | NPC entity | NPC-specific behavior |
| `TransformComponent` | `com.hypixel.hytale.server.core.modules.entity.component` | Position/rotation | `teleportPosition()`, `getPosition()`, `getRotation()` |
| `EntityStatMap` | `com.hypixel.hytale.server.core.modules.entitystats` | Entity stats | `get()`, `set()`, stat indices |
| `EntityStatValue` | `com.hypixel.hytale.server.core.modules.entitystats` | Stat value | `get()`, `set()` |

### NPC Plugin

| Class | Package | Purpose | Key Methods |
|-------|---------|---------|-------------|
| `NPCPlugin` | `com.hypixel.hytale.server.npc` | NPC singleton | `get()`, `spawnNPC()`, `spawnEntity()`, `getBuilderManager()` |
| `Role` | `com.hypixel.hytale.server.npc.role` | NPC role | AI behavior definition |
| `Blackboard` | `com.hypixel.hytale.server.npc.blackboard` | NPC state | AI memory/state |

### Mount System

| Class | Package | Purpose | Key Methods |
|-------|---------|---------|-------------|
| `MountPlugin` | `com.hypixel.hytale.builtin.mounts` | Mount singleton | `getInstance()`, `getMountedComponentType()`, `getMountedByComponentType()` |
| `MountedComponent` | `com.hypixel.hytale.builtin.mounts` | Rider component | `getMountedToEntity()`, `getControllerType()`, `getAttachmentOffset()` |
| `MountedByComponent` | `com.hypixel.hytale.builtin.mounts` | Mount component | `getPassengers()`, `addPassenger()`, `removePassenger()` |
| `MountController` | `com.hypixel.hytale.protocol` | Control type | Enum: passenger/driver modes |

### Block System

| Class | Package | Purpose | Key Methods |
|-------|---------|---------|-------------|
| `BlockModule` | `com.hypixel.hytale.server.core.modules.block` | Block singleton | `get()`, `getBlockEntity()`, `ensureBlockEntity()` |
| `WorldChunk` | `com.hypixel.hytale.server.core.universe.world.chunk` | Chunk data | `getBlockType()`, `setBlockType()` |
| `ChunkLightingManager` | `com.hypixel.hytale.server.core.universe.world.lighting` | Lighting | Light level access |

### Pathfinding (Complex - Deferred)

| Class | Package | Purpose | Key Methods |
|-------|---------|---------|-------------|
| `AStarWithTarget` | `com.hypixel.hytale.server.npc.navigation` | A* pathfinder | `initComputePath()`, `computePath()`, `getPath()` |
| `PathFollower` | `com.hypixel.hytale.server.npc.navigation` | Path execution | `setPath()`, `executePath()`, `clearPath()` |
| `IWaypoint` | `com.hypixel.hytale.server.npc.navigation` | Path point | `getPosition()`, `next()`, `getLength()` |
| `MotionController` | `com.hypixel.hytale.server.npc.movement.controllers` | Movement type | `MotionControllerWalk`, `MotionControllerFly` |

### Weather System

| Class | Package | Purpose | Key Methods |
|-------|---------|---------|-------------|
| `WeatherPlugin` | `com.hypixel.hytale.builtin.weather` | Weather singleton | `get()`, `getWeatherTrackerComponentType()`, `getWeatherResourceType()` |
| `WeatherTracker` | `com.hypixel.hytale.builtin.weather.components` | Per-entity weather | `getWeatherIndex()`, `setWeatherIndex()` |
| `WeatherResource` | `com.hypixel.hytale.builtin.weather.resources` | World weather | `setForcedWeather()`, `getWeatherIndexForEnvironment()` |

---

## Batch 1: NPC Entity Accessor

**Effort**: 16 hours  
**Priority**: 🔴 Critical  
**File**: `HytaleNPCEntityAccessor.java`

### SDK Classes Required

```java
// Entity system
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.entity.Entity;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.ComponentAccessor;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;

// NPC plugin
import com.hypixel.hytale.server.npc.NPCPlugin;
import com.hypixel.hytale.server.npc.entities.NPCEntity;

// Stats system
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatValue;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMapComponent;
```

### 1.1 getEntities(worldName)

**Current State**: Returns empty list or stub

**SDK Pattern**:
```java
@Override
public List<EntityData> getEntities(String worldName) {
    World world = getWorld(worldName);
    if (world == null) {
        return Collections.emptyList();
    }
    
    List<EntityData> entities = new ArrayList<>();
    
    // Execute on world thread
    world.execute(() -> {
        Store<EntityStore> store = world.getEntityStore();
        
        // Iterate all entities using ECS pattern
        store.forEach((Ref<EntityStore> ref, ComponentAccessor<EntityStore> accessor) -> {
            Entity entity = accessor.get(Entity.getComponentType());
            if (entity != null) {
                EntityData data = entityToData(entity, ref, accessor);
                entities.add(data);
            }
        });
    });
    
    return entities;
}
```

**Key SDK Methods**:
- `World.getEntityStore()` → `Store<EntityStore>`
- `Store.forEach(BiConsumer<Ref, ComponentAccessor>)` → iteration
- `ComponentAccessor.get(ComponentType)` → component access

### 1.2 getEntitiesNear(location, radius)

**Current State**: `throw new UnsupportedOperationException`

**SDK Pattern**:
```java
@Override
public List<EntityData> getEntitiesNear(LocationData location, double radius) {
    World world = getWorld(location.world());
    if (world == null) {
        return Collections.emptyList();
    }
    
    List<EntityData> nearbyEntities = new ArrayList<>();
    double radiusSquared = radius * radius;
    Vector3d center = new Vector3d(location.x(), location.y(), location.z());
    
    world.execute(() -> {
        Store<EntityStore> store = world.getEntityStore();
        
        // Option 1: Use SpatialResource if available (NPCPlugin.getNpcSpatialResource())
        // Option 2: Manual iteration with distance check
        
        store.forEach((ref, accessor) -> {
            TransformComponent transform = accessor.get(TransformComponent.getComponentType());
            if (transform != null) {
                Vector3d pos = transform.getPosition();
                double distSq = pos.distanceSquared(center);
                if (distSq <= radiusSquared) {
                    Entity entity = accessor.get(Entity.getComponentType());
                    if (entity != null) {
                        nearbyEntities.add(entityToData(entity, ref, accessor));
                    }
                }
            }
        });
    });
    
    return nearbyEntities;
}
```

**SDK Spatial Query Alternative**:
```java
// More efficient using NPCPlugin's spatial resource
SpatialResource<Ref<EntityStore>, EntityStore> spatial = 
    store.getResource(NPCPlugin.get().getNpcSpatialResource());
    
// Query entities in sphere
List<Ref<EntityStore>> refs = spatial.queryRadius(center, radius);
```

### 1.3 spawnEntity(type, location)

**Current State**: `throw new UnsupportedOperationException`

**SDK Pattern**:
```java
@Override
public Optional<EntityData> spawnEntity(String entityType, LocationData location) {
    World world = getWorld(location.world());
    if (world == null) {
        return Optional.empty();
    }
    
    AtomicReference<EntityData> result = new AtomicReference<>();
    
    world.execute(() -> {
        Store<EntityStore> store = world.getEntityStore();
        Vector3d pos = new Vector3d(location.x(), location.y(), location.z());
        Vector3f rot = new Vector3f(location.pitch(), location.yaw(), 0f);
        
        // Use NPCPlugin for NPC entities
        if (isNpcType(entityType)) {
            var pair = NPCPlugin.get().spawnNPC(
                store,
                entityType,      // Role template name
                null,            // Group name (optional)
                pos,
                rot
            );
            
            if (pair != null) {
                Ref<EntityStore> ref = pair.left();
                INonPlayerCharacter npc = pair.right();
                result.set(npcToData(npc, ref));
            }
        } else {
            // Use World.spawnEntity for generic entities
            // Requires entity factory lookup - may need asset registry
            LOGGER.warn("Non-NPC entity spawning requires asset lookup: {}", entityType);
        }
    });
    
    return Optional.ofNullable(result.get());
}
```

**NPCPlugin.spawnNPC Signature**:
```java
public Pair<Ref<EntityStore>, INonPlayerCharacter> spawnNPC(
    Store<EntityStore> store,
    String roleName,           // e.g., "trork_warrior"
    String groupName,          // e.g., "hostile_group" or null
    Vector3d position,
    Vector3f rotation
);
```

### 1.4 damageEntity(entityId, amount)

**Current State**: Stub with TODO

**SDK Pattern**:
```java
@Override
public boolean damageEntity(UUID entityId, double amount) {
    Entity entity = getEntityByUUID(entityId);
    if (entity == null) {
        return false;
    }
    
    World world = entity.getWorld();
    AtomicBoolean success = new AtomicBoolean(false);
    
    world.execute(() -> {
        Ref<EntityStore> ref = world.getEntityRef(entityId);
        if (ref == null || !ref.isValid()) {
            return;
        }
        
        Store<EntityStore> store = world.getEntityStore();
        ComponentAccessor<EntityStore> accessor = store.getAccessor(ref);
        
        // Get EntityStatMapComponent for health modification
        EntityStatMapComponent statMapComponent = accessor.get(
            EntityStatMapComponent.getComponentType()
        );
        
        if (statMapComponent != null) {
            EntityStatMap statMap = statMapComponent.getStatMap();
            
            // Health is typically index 0 or use named lookup
            // SDK uses EntityStatIndex enum or integer indices
            int healthIndex = getHealthStatIndex(); // Implementation TBD
            
            EntityStatValue currentHealth = statMap.get(healthIndex);
            double newHealth = Math.max(0, currentHealth.get() - amount);
            currentHealth.set(newHealth);
            
            success.set(true);
            
            LOGGER.debug("Damaged entity {} by {} (health now: {})", 
                entityId, amount, newHealth);
        }
    });
    
    return success.get();
}
```

**SDK Research Required**:
- Exact stat index for Health (may be `EntityStatIndex.HEALTH` or integer)
- Whether damage events should be fired (`DamageEvent` if exists)
- DamageSource system for attribution

### 1.5 healEntity(entityId, amount)

**Current State**: Stub with TODO

**SDK Pattern**:
```java
@Override
public boolean healEntity(UUID entityId, double amount) {
    Entity entity = getEntityByUUID(entityId);
    if (entity == null) {
        return false;
    }
    
    World world = entity.getWorld();
    AtomicBoolean success = new AtomicBoolean(false);
    
    world.execute(() -> {
        Ref<EntityStore> ref = world.getEntityRef(entityId);
        Store<EntityStore> store = world.getEntityStore();
        ComponentAccessor<EntityStore> accessor = store.getAccessor(ref);
        
        EntityStatMapComponent statMapComponent = accessor.get(
            EntityStatMapComponent.getComponentType()
        );
        
        if (statMapComponent != null) {
            EntityStatMap statMap = statMapComponent.getStatMap();
            
            int healthIndex = getHealthStatIndex();
            int maxHealthIndex = getMaxHealthStatIndex();
            
            EntityStatValue currentHealth = statMap.get(healthIndex);
            EntityStatValue maxHealth = statMap.get(maxHealthIndex);
            
            double newHealth = Math.min(maxHealth.get(), currentHealth.get() + amount);
            currentHealth.set(newHealth);
            
            success.set(true);
        }
    });
    
    return success.get();
}
```

### 1.6 moveToLocation(entityId, target) - DEFERRED

**Current State**: `throw new UnsupportedOperationException`

**Complexity**: Very High - requires A* pathfinding integration

**SDK Pattern (Simplified)**:
```java
@Override
public void moveToLocation(UUID entityId, LocationData target) {
    // This requires full A* pathfinding integration
    // See HIGH_RISK_API_RESEARCH.md for detailed pattern
    
    Entity entity = getEntityByUUID(entityId);
    if (entity == null) {
        LOGGER.warn("Cannot move non-existent entity: {}", entityId);
        return;
    }
    
    World world = entity.getWorld();
    
    world.execute(() -> {
        Ref<EntityStore> ref = world.getEntityRef(entityId);
        Store<EntityStore> store = world.getEntityStore();
        ComponentAccessor<EntityStore> accessor = store.getAccessor(ref);
        
        // Get Role from RoleComponent
        RoleComponent roleComp = accessor.get(RoleComponent.getComponentType());
        if (roleComp == null) {
            LOGGER.warn("Entity {} has no RoleComponent - cannot navigate", entityId);
            return;
        }
        
        Role role = roleComp.getRole();
        MotionController controller = role.getMotionController();
        
        // Get or create PathFollower
        PathFollower pathFollower = getOrCreatePathFollower(ref, accessor);
        
        // Get A* node pool from resource
        AStarNodePoolProviderSimple nodePool = store.getResource(
            NPCPlugin.get().getAStarNodePoolProviderSimpleResourceType()
        );
        
        // Create pathfinder
        AStarWithTarget pathfinder = new AStarWithTarget();
        
        Vector3d startPos = getEntityPosition(ref, accessor);
        Vector3d targetPos = new Vector3d(target.x(), target.y(), target.z());
        
        // Init path computation (async)
        Progress progress = pathfinder.initComputePath(
            ref,
            startPos,
            targetPos,
            AStarEvaluator.DEFAULT,
            controller,
            new ProbeMoveData(),
            nodePool,
            accessor
        );
        
        // Schedule continued path computation
        // This may span multiple ticks
        schedulePathComputation(ref, pathfinder, pathFollower, controller, accessor);
    });
}
```

**Recommendation**: Defer this implementation until NPC quest navigation is specifically required.

### 1.7 setMetadata / getMetadata

**Current State**: `throw new UnsupportedOperationException`

**SDK Limitation**: Hytale uses ECS components, not arbitrary key-value metadata.

**Workaround Pattern**:
```java
// Option 1: Use BsonDocument in entity's existing metadata component
// Option 2: Register custom MetadataComponent

// Accessor interface for metadata
private static final Map<UUID, Map<String, Object>> entityMetadataCache = 
    new ConcurrentHashMap<>();

@Override
public void setMetadata(UUID entityId, String key, Object value) {
    entityMetadataCache
        .computeIfAbsent(entityId, k -> new ConcurrentHashMap<>())
        .put(key, value);
    
    // Optionally persist to entity's BsonDocument if available
    persistMetadataToEntity(entityId, key, value);
}

@Override
public Optional<Object> getMetadata(UUID entityId, String key) {
    Map<String, Object> metadata = entityMetadataCache.get(entityId);
    if (metadata == null) {
        // Try loading from entity's BsonDocument
        metadata = loadMetadataFromEntity(entityId);
        if (metadata != null) {
            entityMetadataCache.put(entityId, metadata);
        }
    }
    return Optional.ofNullable(metadata != null ? metadata.get(key) : null);
}
```

### Verification Criteria - Batch 1

- [ ] `getEntities()` returns all entities in world
- [ ] `getEntitiesNear()` filters by radius correctly
- [ ] `spawnEntity()` creates NPC entities
- [ ] `damageEntity()` reduces health stat
- [ ] `healEntity()` increases health stat (capped at max)
- [ ] `setMetadata()/getMetadata()` store/retrieve values
- [ ] All methods handle null world gracefully
- [ ] All methods execute on correct thread

---

## Batch 2: World Accessor

**Effort**: 14 hours  
**Priority**: 🔴 Critical  
**File**: `HytaleWorldAccessor.java`

### SDK Classes Required

```java
// World system
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.hypixel.hytale.server.core.modules.block.BlockModule;

// Biome/Zone (implemented in previous session)
import com.hypixel.hytale.server.worldgen.cache.ChunkGeneratorCache;
import com.hypixel.hytale.server.worldgen.chunk.ZoneBiomeResult;
import com.hypixel.hytale.server.worldgen.biome.Biome;
import com.hypixel.hytale.server.worldgen.zone.Zone;
```

### 2.1 getBlockType(location)

**Current State**: Stub returning null

**SDK Pattern**:
```java
@Override
public String getBlockType(LocationData location) {
    World world = getWorld(location.world());
    if (world == null) {
        return "air";
    }
    
    int x = (int) Math.floor(location.x());
    int y = (int) Math.floor(location.y());
    int z = (int) Math.floor(location.z());
    
    // Convert world coords to chunk coords
    long chunkKey = ChunkUtils.positionToKey(x >> 4, z >> 4);
    
    WorldChunk chunk = world.getChunkIfLoaded(chunkKey);
    if (chunk == null) {
        return "air"; // Unloaded chunk
    }
    
    // Get local coords within chunk
    int localX = x & 15;
    int localY = y;
    int localZ = z & 15;
    
    // Get block type ID
    int blockTypeId = chunk.getBlockType(localX, localY, localZ);
    
    // Resolve ID to name via AssetRegistry
    return resolveBlockTypeName(blockTypeId);
}
```

### 2.2 setBlock(location, type)

**Current State**: `throw new UnsupportedOperationException`

**SDK Pattern**:
```java
@Override
public boolean setBlock(LocationData location, String blockType) {
    World world = getWorld(location.world());
    if (world == null) {
        return false;
    }
    
    int blockTypeId = resolveBlockTypeId(blockType);
    if (blockTypeId < 0) {
        LOGGER.warn("Unknown block type: {}", blockType);
        return false;
    }
    
    int x = (int) Math.floor(location.x());
    int y = (int) Math.floor(location.y());
    int z = (int) Math.floor(location.z());
    
    AtomicBoolean success = new AtomicBoolean(false);
    
    world.execute(() -> {
        long chunkKey = ChunkUtils.positionToKey(x >> 4, z >> 4);
        WorldChunk chunk = world.getChunkIfLoaded(chunkKey);
        
        if (chunk != null) {
            int localX = x & 15;
            int localY = y;
            int localZ = z & 15;
            
            // SetBlockSettings for physics/lighting updates
            SetBlockSettings settings = new SetBlockSettings()
                .withUpdateLighting(true)
                .withNotifyNeighbors(true);
            
            chunk.setBlockType(localX, localY, localZ, blockTypeId, settings);
            success.set(true);
        }
    });
    
    return success.get();
}
```

### 2.3 isChunkLoaded(worldName, chunkX, chunkZ)

**Current State**: Returns false

**SDK Pattern**:
```java
@Override
public boolean isChunkLoaded(String worldName, int chunkX, int chunkZ) {
    World world = getWorld(worldName);
    if (world == null) {
        return false;
    }
    
    long chunkKey = ChunkUtils.positionToKey(chunkX, chunkZ);
    WorldChunk chunk = world.getChunkIfLoaded(chunkKey);
    
    return chunk != null;
}
```

### 2.4 loadChunk(worldName, chunkX, chunkZ)

**Current State**: `throw new UnsupportedOperationException`

**SDK Pattern**:
```java
@Override
public CompletableFuture<Boolean> loadChunk(String worldName, int chunkX, int chunkZ) {
    World world = getWorld(worldName);
    if (world == null) {
        return CompletableFuture.completedFuture(false);
    }
    
    long chunkKey = ChunkUtils.positionToKey(chunkX, chunkZ);
    
    return world.getChunkAsync(chunkKey)
        .thenApply(chunk -> chunk != null)
        .exceptionally(ex -> {
            LOGGER.error("Failed to load chunk ({}, {}): {}", chunkX, chunkZ, ex.getMessage());
            return false;
        });
}
```

### 2.5 getHighestBlock(worldName, x, z)

**Current State**: Returns y=64 (stub)

**SDK Pattern**:
```java
@Override
public int getHighestBlock(String worldName, int x, int z) {
    World world = getWorld(worldName);
    if (world == null) {
        return 64; // Fallback
    }
    
    int chunkX = x >> 4;
    int chunkZ = z >> 4;
    long chunkKey = ChunkUtils.positionToKey(chunkX, chunkZ);
    
    WorldChunk chunk = world.getChunkIfLoaded(chunkKey);
    if (chunk == null) {
        return 64; // Unloaded - can't determine
    }
    
    int localX = x & 15;
    int localZ = z & 15;
    
    // Raycast from top down to find first solid block
    int maxHeight = chunk.getMaxHeight();
    for (int y = maxHeight - 1; y >= 0; y--) {
        int blockType = chunk.getBlockType(localX, y, localZ);
        if (blockType != 0) { // 0 = air
            return y;
        }
    }
    
    return 0; // All air
}
```

### 2.6 getWeather(worldName) - ✅ IMPLEMENTED

**Current State**: Implemented in previous session using WeatherPlugin

### 2.7 getBiome(location) - ✅ IMPLEMENTED

**Current State**: Implemented using ChunkGenerator.getZoneBiomeResultAt()

### 2.8 getZone(location) - ✅ IMPLEMENTED

**Current State**: Implemented using ChunkGenerator.getZoneBiomeResultAt()

### Verification Criteria - Batch 2

- [ ] `getBlockType()` returns correct block name
- [ ] `setBlock()` changes block with lighting/neighbor updates
- [ ] `isChunkLoaded()` correctly detects loaded chunks
- [ ] `loadChunk()` loads chunks asynchronously
- [ ] `getHighestBlock()` finds first solid block from top
- [ ] ✅ `getWeather()` - already implemented
- [ ] ✅ `getBiome()` - already implemented
- [ ] ✅ `getZone()` - already implemented

---

## Batch 3: UI Accessor

**Effort**: 20 hours  
**Priority**: 🟡 Medium  
**File**: `HytaleUIAccessor.java`

### HyUI Classes Required

```java
// HyUI builders
import hyui.builder.ToastBuilder;
import hyui.builder.TooltipBuilder;
import hyui.builder.OverlayBuilder;
import hyui.builder.SidebarBuilder;
import hyui.animation.AnimationController;
import hyui.input.InputCapture;
```

### 3.1 Toast Methods

**Methods**: `showToast()`, `hideToast()`, `updateToast()`

**HyUI Pattern**:
```java
private final Map<UUID, Map<String, Object>> activeToasts = new ConcurrentHashMap<>();

@Override
public void showToast(UUID playerId, String toastId, ToastData data) {
    ToastBuilder.toast(playerId)
        .id(toastId)
        .title(data.title())
        .message(data.message())
        .icon(data.iconPath())
        .duration(data.durationMs())
        .position(mapToastPosition(data.position()))
        .onClose(() -> activeToasts.getOrDefault(playerId, Map.of()).remove(toastId))
        .show();
    
    activeToasts
        .computeIfAbsent(playerId, k -> new ConcurrentHashMap<>())
        .put(toastId, data);
}

@Override
public void hideToast(UUID playerId, String toastId) {
    ToastBuilder.toast(playerId)
        .id(toastId)
        .hide();
    
    var playerToasts = activeToasts.get(playerId);
    if (playerToasts != null) {
        playerToasts.remove(toastId);
    }
}

@Override
public void updateToast(UUID playerId, String toastId, ToastData data) {
    ToastBuilder.toast(playerId)
        .id(toastId)
        .title(data.title())
        .message(data.message())
        .update();
}
```

### 3.2 Tooltip Methods

**Methods**: `showTooltip()`, `hideTooltip()`, `updateTooltip()`

**HyUI Pattern**:
```java
@Override
public void showTooltip(UUID playerId, String tooltipId, TooltipData data) {
    TooltipBuilder.tooltip(playerId)
        .id(tooltipId)
        .content(data.content())
        .position(data.x(), data.y())
        .anchor(mapAnchor(data.anchor()))
        .width(data.width())
        .show();
}

@Override
public void hideTooltip(UUID playerId, String tooltipId) {
    TooltipBuilder.tooltip(playerId)
        .id(tooltipId)
        .hide();
}
```

### 3.3 Overlay Methods

**Methods**: `showOverlay()`, `hideOverlay()`, `updateOverlay()`, `hasOverlay()`

**HyUI Pattern**:
```java
private final Map<UUID, Set<String>> activeOverlays = new ConcurrentHashMap<>();

@Override
public void showOverlay(UUID playerId, String overlayId, String htmlContent) {
    OverlayBuilder.overlay(playerId)
        .id(overlayId)
        .fromHtml(htmlContent)
        .withClickPassthrough(false)
        .show();
    
    activeOverlays
        .computeIfAbsent(playerId, k -> ConcurrentHashMap.newKeySet())
        .add(overlayId);
}

@Override
public void hideOverlay(UUID playerId, String overlayId) {
    OverlayBuilder.overlay(playerId)
        .id(overlayId)
        .hide();
    
    var playerOverlays = activeOverlays.get(playerId);
    if (playerOverlays != null) {
        playerOverlays.remove(overlayId);
    }
}

@Override
public boolean hasOverlay(UUID playerId, String overlayId) {
    var playerOverlays = activeOverlays.get(playerId);
    return playerOverlays != null && playerOverlays.contains(overlayId);
}
```

### 3.4 Sidebar Methods

**Methods**: `showSidebar()`, `hideSidebar()`, `updateSidebar()`, `hasSidebar()`

**HyUI Pattern**:
```java
@Override
public void showSidebar(UUID playerId, String sidebarId, SidebarData data) {
    StringBuilder html = new StringBuilder();
    html.append("<div class=\"sidebar\">");
    html.append("<h2>").append(data.title()).append("</h2>");
    html.append("<ul>");
    for (String line : data.lines()) {
        html.append("<li>").append(line).append("</li>");
    }
    html.append("</ul></div>");
    
    SidebarBuilder.sidebar(playerId)
        .id(sidebarId)
        .fromHtml(html.toString())
        .position(data.position()) // LEFT, RIGHT
        .width(data.width())
        .show();
}
```

### 3.5 Animation Methods

**Methods**: `playUIAnimation()`, `stopUIAnimation()`, `getAnimationState()`

**HyUI Pattern**:
```java
private final Map<String, AnimationController> animationControllers = new ConcurrentHashMap<>();

@Override
public void playUIAnimation(UUID playerId, String elementId, AnimationData animation) {
    String key = playerId + ":" + elementId;
    
    AnimationController controller = AnimationController.create()
        .target(playerId, elementId)
        .type(mapAnimationType(animation.type()))
        .duration(animation.durationMs())
        .easing(mapEasing(animation.easing()))
        .onComplete(() -> animationControllers.remove(key))
        .play();
    
    animationControllers.put(key, controller);
}

@Override
public void stopUIAnimation(UUID playerId, String elementId) {
    String key = playerId + ":" + elementId;
    AnimationController controller = animationControllers.remove(key);
    if (controller != null) {
        controller.stop();
    }
}

@Override
public String getAnimationState(UUID playerId, String elementId) {
    String key = playerId + ":" + elementId;
    AnimationController controller = animationControllers.get(key);
    if (controller == null) {
        return "NONE";
    }
    return controller.isPlaying() ? "PLAYING" : "STOPPED";
}
```

### 3.6 Input Capture Methods

**Methods**: `captureInput()`, `releaseInput()`

**HyUI Pattern**:
```java
private final Map<UUID, InputCapture> inputCaptures = new ConcurrentHashMap<>();

@Override
public void captureInput(UUID playerId, InputCaptureConfig config) {
    InputCapture capture = InputCapture.create(playerId)
        .captureKeyboard(config.keyboard())
        .captureMouse(config.mouse())
        .onKey(config.keyHandler())
        .onMouse(config.mouseHandler())
        .start();
    
    inputCaptures.put(playerId, capture);
}

@Override
public void releaseInput(UUID playerId) {
    InputCapture capture = inputCaptures.remove(playerId);
    if (capture != null) {
        capture.release();
    }
}
```

### Verification Criteria - Batch 3

- [ ] Toast shows/hides/updates correctly
- [ ] Tooltip follows mouse or fixed position
- [ ] Overlay renders custom HTML
- [ ] Sidebar displays on left/right
- [ ] Animation plays with correct easing
- [ ] Input capture intercepts keyboard/mouse
- [ ] All UI elements clean up on player disconnect

---

## Batch 4: Mount System

**Effort**: 8 hours  
**Priority**: 🟠 High  
**File**: `HytaleEntityAccessor.java` or `HytaleMountAccessor.java`

### SDK Classes Required

```java
import com.hypixel.hytale.builtin.mounts.MountPlugin;
import com.hypixel.hytale.builtin.mounts.MountedComponent;
import com.hypixel.hytale.builtin.mounts.MountedByComponent;
import com.hypixel.hytale.builtin.mounts.NPCMountComponent;
import com.hypixel.hytale.protocol.MountController;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.math.vector.Vector3f;
```

### 4.1 mountEntity(riderId, mountId)

**SDK Pattern**:
```java
@Override
public boolean mountEntity(UUID riderId, UUID mountId) {
    Entity rider = getEntityByUUID(riderId);
    Entity mount = getEntityByUUID(mountId);
    
    if (rider == null || mount == null) {
        return false;
    }
    
    World world = rider.getWorld();
    if (!world.equals(mount.getWorld())) {
        return false; // Different worlds
    }
    
    AtomicBoolean success = new AtomicBoolean(false);
    
    world.execute(() -> {
        Store<EntityStore> store = world.getEntityStore();
        
        Ref<EntityStore> riderRef = world.getEntityRef(riderId);
        Ref<EntityStore> mountRef = world.getEntityRef(mountId);
        
        if (riderRef == null || mountRef == null) {
            return;
        }
        
        ComponentAccessor<EntityStore> riderAccessor = store.getAccessor(riderRef);
        ComponentAccessor<EntityStore> mountAccessor = store.getAccessor(mountRef);
        
        // Check if mount can accept passengers
        NPCMountComponent mountComponent = mountAccessor.get(
            MountPlugin.getInstance().getMountComponentType()
        );
        
        if (mountComponent == null) {
            LOGGER.warn("Entity {} is not a mount", mountId);
            return;
        }
        
        // Create MountedComponent for rider
        MountedComponent mountedComp = new MountedComponent(
            mountRef,
            new Vector3f(0, 0, 0),  // Attachment offset
            MountController.DRIVER   // or PASSENGER
        );
        
        // Add component to rider
        riderAccessor.add(
            MountPlugin.getInstance().getMountedComponentType(),
            mountedComp
        );
        
        // Add rider to mount's passenger list
        MountedByComponent mountedBy = mountAccessor.get(
            MountPlugin.getInstance().getMountedByComponentType()
        );
        
        if (mountedBy == null) {
            mountedBy = new MountedByComponent();
            mountAccessor.add(
                MountPlugin.getInstance().getMountedByComponentType(),
                mountedBy
            );
        }
        
        mountedBy.addPassenger(riderRef);
        
        success.set(true);
    });
    
    return success.get();
}
```

### 4.2 unmountEntity(riderId)

**SDK Pattern**:
```java
@Override
public boolean unmountEntity(UUID riderId) {
    Entity rider = getEntityByUUID(riderId);
    if (rider == null) {
        return false;
    }
    
    World world = rider.getWorld();
    AtomicBoolean success = new AtomicBoolean(false);
    
    world.execute(() -> {
        Ref<EntityStore> riderRef = world.getEntityRef(riderId);
        Store<EntityStore> store = world.getEntityStore();
        ComponentAccessor<EntityStore> riderAccessor = store.getAccessor(riderRef);
        
        // Get current mount
        MountedComponent mounted = riderAccessor.get(
            MountPlugin.getInstance().getMountedComponentType()
        );
        
        if (mounted == null) {
            return; // Not mounted
        }
        
        Ref<EntityStore> mountRef = mounted.getMountedToEntity();
        
        // Remove MountedComponent from rider
        riderAccessor.remove(MountPlugin.getInstance().getMountedComponentType());
        
        // Remove rider from mount's passenger list
        if (mountRef != null && mountRef.isValid()) {
            ComponentAccessor<EntityStore> mountAccessor = store.getAccessor(mountRef);
            MountedByComponent mountedBy = mountAccessor.get(
                MountPlugin.getInstance().getMountedByComponentType()
            );
            
            if (mountedBy != null) {
                mountedBy.removePassenger(riderRef);
            }
        }
        
        success.set(true);
    });
    
    return success.get();
}
```

### 4.3 getMountedEntity(riderId)

**SDK Pattern**:
```java
@Override
public Optional<UUID> getMountedEntity(UUID riderId) {
    Entity rider = getEntityByUUID(riderId);
    if (rider == null) {
        return Optional.empty();
    }
    
    World world = rider.getWorld();
    AtomicReference<UUID> result = new AtomicReference<>();
    
    world.execute(() -> {
        Ref<EntityStore> riderRef = world.getEntityRef(riderId);
        Store<EntityStore> store = world.getEntityStore();
        ComponentAccessor<EntityStore> accessor = store.getAccessor(riderRef);
        
        MountedComponent mounted = accessor.get(
            MountPlugin.getInstance().getMountedComponentType()
        );
        
        if (mounted != null) {
            Ref<EntityStore> mountRef = mounted.getMountedToEntity();
            if (mountRef != null && mountRef.isValid()) {
                // Get UUID from mount entity
                Entity mount = accessor.get(Entity.getComponentType());
                if (mount != null) {
                    result.set(mount.getUuid());
                }
            }
        }
    });
    
    return Optional.ofNullable(result.get());
}
```

### 4.4 getPassengers(mountId)

**SDK Pattern**:
```java
@Override
public List<UUID> getPassengers(UUID mountId) {
    Entity mount = getEntityByUUID(mountId);
    if (mount == null) {
        return Collections.emptyList();
    }
    
    World world = mount.getWorld();
    List<UUID> passengers = new ArrayList<>();
    
    world.execute(() -> {
        Ref<EntityStore> mountRef = world.getEntityRef(mountId);
        Store<EntityStore> store = world.getEntityStore();
        ComponentAccessor<EntityStore> accessor = store.getAccessor(mountRef);
        
        MountedByComponent mountedBy = accessor.get(
            MountPlugin.getInstance().getMountedByComponentType()
        );
        
        if (mountedBy != null) {
            for (Ref<EntityStore> passengerRef : mountedBy.getPassengers()) {
                if (passengerRef.isValid()) {
                    ComponentAccessor<EntityStore> passengerAccessor = 
                        store.getAccessor(passengerRef);
                    Entity passenger = passengerAccessor.get(Entity.getComponentType());
                    if (passenger != null) {
                        passengers.add(passenger.getUuid());
                    }
                }
            }
        }
    });
    
    return passengers;
}
```

### Verification Criteria - Batch 4

- [ ] `mountEntity()` attaches rider to mount
- [ ] `unmountEntity()` detaches rider from mount
- [ ] `getMountedEntity()` returns current mount UUID
- [ ] `getPassengers()` returns all passenger UUIDs
- [ ] Mount/unmount works for players and NPCs
- [ ] Multi-passenger mounts are supported

---

## Batch 5: Storage Accessor

**Effort**: 10 hours  
**Priority**: 🔴 Critical  
**File**: `HytaleStorageAccessor.java`

### Implementation Approach

The Hytale SDK does not provide a direct key-value storage API. Use the `03-framework-storage` abstraction with a file-based or SQLite backend.

### 5.1 Storage Implementation Pattern

```java
public class HytaleStorageAccessor implements StorageAccessor {
    
    private final Path dataDirectory;
    private final Gson gson;
    
    // Player data storage
    private final Map<UUID, Map<String, Object>> playerDataCache = new ConcurrentHashMap<>();
    
    // Global data storage
    private final Map<String, Map<String, Object>> globalDataCache = new ConcurrentHashMap<>();
    
    public HytaleStorageAccessor(Path dataDirectory) {
        this.dataDirectory = dataDirectory;
        this.gson = new GsonBuilder().setPrettyPrinting().create();
        
        // Create directories
        try {
            Files.createDirectories(dataDirectory.resolve("players"));
            Files.createDirectories(dataDirectory.resolve("global"));
        } catch (IOException e) {
            LOGGER.error("Failed to create storage directories", e);
        }
    }
    
    @Override
    public CompletableFuture<Void> savePlayerData(UUID playerId, String key, Object data) {
        return CompletableFuture.runAsync(() -> {
            try {
                // Update cache
                playerDataCache
                    .computeIfAbsent(playerId, k -> new ConcurrentHashMap<>())
                    .put(key, data);
                
                // Persist to file
                Path playerFile = getPlayerDataPath(playerId);
                Map<String, Object> allData = playerDataCache.get(playerId);
                
                String json = gson.toJson(allData);
                Files.writeString(playerFile, json, StandardOpenOption.CREATE, 
                    StandardOpenOption.TRUNCATE_EXISTING);
                    
            } catch (IOException e) {
                throw new CompletionException(e);
            }
        });
    }
    
    @Override
    public CompletableFuture<Optional<Object>> loadPlayerData(UUID playerId, String key) {
        return CompletableFuture.supplyAsync(() -> {
            // Check cache
            Map<String, Object> cached = playerDataCache.get(playerId);
            if (cached != null && cached.containsKey(key)) {
                return Optional.of(cached.get(key));
            }
            
            // Load from file
            Path playerFile = getPlayerDataPath(playerId);
            if (!Files.exists(playerFile)) {
                return Optional.empty();
            }
            
            try {
                String json = Files.readString(playerFile);
                Map<String, Object> data = gson.fromJson(json, 
                    new TypeToken<Map<String, Object>>(){}.getType());
                
                // Update cache
                playerDataCache.put(playerId, new ConcurrentHashMap<>(data));
                
                return Optional.ofNullable(data.get(key));
                
            } catch (IOException e) {
                LOGGER.error("Failed to load player data for {}", playerId, e);
                return Optional.empty();
            }
        });
    }
    
    @Override
    public CompletableFuture<Void> deletePlayerData(UUID playerId) {
        return CompletableFuture.runAsync(() -> {
            playerDataCache.remove(playerId);
            
            Path playerFile = getPlayerDataPath(playerId);
            try {
                Files.deleteIfExists(playerFile);
            } catch (IOException e) {
                LOGGER.error("Failed to delete player data for {}", playerId, e);
            }
        });
    }
    
    @Override
    public CompletableFuture<List<UUID>> listPlayerData() {
        return CompletableFuture.supplyAsync(() -> {
            try (Stream<Path> files = Files.list(dataDirectory.resolve("players"))) {
                return files
                    .filter(p -> p.toString().endsWith(".json"))
                    .map(p -> {
                        String fileName = p.getFileName().toString();
                        String uuidStr = fileName.replace(".json", "");
                        return UUID.fromString(uuidStr);
                    })
                    .collect(Collectors.toList());
            } catch (IOException e) {
                LOGGER.error("Failed to list player data", e);
                return Collections.emptyList();
            }
        });
    }
    
    @Override
    public CompletableFuture<Boolean> hasPlayerData(UUID playerId) {
        return CompletableFuture.supplyAsync(() -> {
            if (playerDataCache.containsKey(playerId)) {
                return true;
            }
            Path playerFile = getPlayerDataPath(playerId);
            return Files.exists(playerFile);
        });
    }
    
    private Path getPlayerDataPath(UUID playerId) {
        return dataDirectory.resolve("players").resolve(playerId + ".json");
    }
}
```

### Verification Criteria - Batch 5

- [ ] `savePlayerData()` persists data to file
- [ ] `loadPlayerData()` retrieves persisted data
- [ ] `deletePlayerData()` removes player file
- [ ] `listPlayerData()` returns all player UUIDs with data
- [ ] `hasPlayerData()` checks existence
- [ ] Global data methods work similarly
- [ ] Thread-safe caching implemented

---

## Batch 6: Model & Animation Accessor

**Effort**: 16 hours  
**Priority**: 🟡 Medium  
**File**: `HytaleModelAnimationAccessor.java`

### SDK Limitation Warning

**⚠️ Animation playback API may not be fully exposed in current SDK.**

Many model/animation methods may need to remain as stubs with logging until the SDK provides direct animation control APIs.

### 6.1 Available Methods (Asset Enumeration)

```java
@Override
public List<String> getModelAssets() {
    // Use AssetRegistry to list models
    AssetRegistry registry = AssetRegistry.get();
    AssetStore<String, ModelAsset> modelStore = registry.getAssetStore("models");
    
    return modelStore.getAssetMap().keySet().stream()
        .collect(Collectors.toList());
}

@Override
public Optional<ModelInfo> getModel(String modelId) {
    AssetRegistry registry = AssetRegistry.get();
    ModelAsset asset = registry.getAsset("models", modelId);
    
    if (asset == null) {
        return Optional.empty();
    }
    
    Model model = asset.getModel();
    return Optional.of(new ModelInfo(
        modelId,
        model.getName(),
        model.getBounds(),
        getAnimationSets(modelId)
    ));
}
```

### 6.2 Animation Enumeration

```java
@Override
public List<String> getAnimationSets(String modelId) {
    // Model.getAnimations() may return animation set names
    AssetRegistry registry = AssetRegistry.get();
    ModelAsset asset = registry.getAsset("models", modelId);
    
    if (asset == null) {
        return Collections.emptyList();
    }
    
    Model model = asset.getModel();
    // SDK may expose: model.getAnimationSets() or similar
    // If not available, return empty and log warning
    
    LOGGER.debug("Animation enumeration for model {} - SDK pattern TBD", modelId);
    return Collections.emptyList(); // Stub until SDK pattern confirmed
}
```

### 6.3 Animation Playback (Deferred)

```java
@Override
public void playAnimation(UUID entityId, String animationName) {
    // DEFERRED: Animation playback API not confirmed in SDK
    // May require AnimationControllerComponent or protocol packet
    
    LOGGER.warn("playAnimation() not implemented - SDK animation API research required");
    throw new UnsupportedOperationException(
        "Animation playback requires further SDK research. " +
        "See HIGH_RISK_API_RESEARCH.md section 5.3"
    );
}

@Override
public void stopAnimation(UUID entityId) {
    LOGGER.warn("stopAnimation() not implemented - SDK animation API research required");
    throw new UnsupportedOperationException(
        "Animation stop requires further SDK research"
    );
}
```

### Verification Criteria - Batch 6

- [ ] `getModelAssets()` lists available models
- [ ] `getModel()` returns model info if exists
- [ ] `getAnimationSets()` lists animations for model
- [ ] Animation playback documented as deferred
- [ ] Proper logging for unimplemented methods

---

## Batch 7: Hologram Accessor

**Effort**: 8 hours  
**Priority**: 🟢 Low  
**File**: `HytaleHologramAccessor.java`

### Implementation Options

1. **Floating Text Entities** (if SDK supports)
2. **World-space HyUI Elements** (preferred if available)
3. **Invisible NPC with Custom Display Name** (fallback)

### 7.1 HyUI World-Space Implementation

```java
public class HytaleHologramAccessor implements HologramAccessor {
    
    private final Map<String, HologramData> holograms = new ConcurrentHashMap<>();
    
    @Override
    public String createHologram(LocationData location, List<String> lines) {
        String hologramId = UUID.randomUUID().toString();
        
        // Build HTML for hologram
        StringBuilder html = new StringBuilder();
        html.append("<div class=\"hologram\">");
        for (String line : lines) {
            html.append("<div class=\"hologram-line\">").append(line).append("</div>");
        }
        html.append("</div>");
        
        // Create world-space HyUI element
        // Note: HyUI world-space API may vary
        WorldSpaceUI.create()
            .id(hologramId)
            .position(location.x(), location.y(), location.z())
            .world(location.world())
            .fromHtml(html.toString())
            .billboard(true) // Always face player
            .show();
        
        holograms.put(hologramId, new HologramData(hologramId, location, lines));
        
        return hologramId;
    }
    
    @Override
    public void deleteHologram(String hologramId) {
        WorldSpaceUI.get(hologramId).hide();
        holograms.remove(hologramId);
    }
    
    @Override
    public void updateHologram(String hologramId, List<String> lines) {
        HologramData data = holograms.get(hologramId);
        if (data == null) {
            return;
        }
        
        StringBuilder html = new StringBuilder();
        html.append("<div class=\"hologram\">");
        for (String line : lines) {
            html.append("<div class=\"hologram-line\">").append(line).append("</div>");
        }
        html.append("</div>");
        
        WorldSpaceUI.get(hologramId)
            .fromHtml(html.toString())
            .update();
        
        data.lines = lines;
    }
    
    @Override
    public void moveHologram(String hologramId, LocationData newLocation) {
        HologramData data = holograms.get(hologramId);
        if (data == null) {
            return;
        }
        
        WorldSpaceUI.get(hologramId)
            .position(newLocation.x(), newLocation.y(), newLocation.z())
            .update();
        
        data.location = newLocation;
    }
}
```

### 7.2 NPC Fallback Implementation

If world-space UI is not available:

```java
@Override
public String createHologram(LocationData location, List<String> lines) {
    // Spawn invisible NPC with custom name tag
    String hologramId = UUID.randomUUID().toString();
    
    // Combine lines into single display name (may have length limit)
    String displayName = String.join("\n", lines);
    
    // Spawn NPC with role that makes it invisible but shows name
    var result = NPCPlugin.get().spawnNPC(
        store,
        "hologram_template",  // Custom role with invisible model
        null,
        new Vector3d(location.x(), location.y(), location.z()),
        new Vector3f(0, 0, 0)
    );
    
    if (result != null) {
        // Set custom display name
        // SDK pattern TBD
    }
    
    return hologramId;
}
```

### Verification Criteria - Batch 7

- [ ] `createHologram()` displays floating text
- [ ] `deleteHologram()` removes display
- [ ] `updateHologram()` changes text content
- [ ] `moveHologram()` repositions display
- [ ] Holograms always face player (billboard)
- [ ] Multi-line support works

---

## Batch 8: Guild Accessor

**Effort**: 12 hours  
**Priority**: 🟡 Medium  
**File**: `HytaleGuildAccessor.java`

### Implementation Approach

Guild system is entirely custom - no SDK dependency. Uses storage accessor for persistence.

### 8.1 Guild Storage Pattern

```java
public class HytaleGuildAccessor implements GuildAccessor {
    
    private final StorageAccessor storage;
    private final Map<String, GuildData> guildCache = new ConcurrentHashMap<>();
    private final Map<UUID, String> playerGuildMap = new ConcurrentHashMap<>();
    
    private static final String GUILD_STORAGE_KEY = "guilds";
    
    @Override
    public CompletableFuture<GuildData> createGuild(String name, UUID ownerId) {
        return CompletableFuture.supplyAsync(() -> {
            String guildId = UUID.randomUUID().toString();
            
            GuildData guild = new GuildData(
                guildId,
                name,
                ownerId,
                new ArrayList<>(List.of(ownerId)),  // Members
                new HashMap<>(),                      // Ranks
                null,                                 // MOTD
                null,                                 // Description
                RecruitmentStatus.OPEN,
                null                                  // Emblem
            );
            
            // Set owner as leader
            guild.ranks().put(ownerId, GuildRank.LEADER);
            
            // Cache
            guildCache.put(guildId, guild);
            playerGuildMap.put(ownerId, guildId);
            
            // Persist
            persistGuild(guild);
            
            return guild;
        });
    }
    
    @Override
    public CompletableFuture<Boolean> deleteGuild(String guildId) {
        return CompletableFuture.supplyAsync(() -> {
            GuildData guild = guildCache.remove(guildId);
            if (guild == null) {
                return false;
            }
            
            // Remove player mappings
            for (UUID member : guild.members()) {
                playerGuildMap.remove(member);
            }
            
            // Delete from storage
            return storage.deleteGlobalData(GUILD_STORAGE_KEY, guildId).join();
        });
    }
    
    @Override
    public CompletableFuture<Boolean> inviteMember(String guildId, UUID playerId) {
        return CompletableFuture.supplyAsync(() -> {
            GuildData guild = guildCache.get(guildId);
            if (guild == null) {
                return false;
            }
            
            if (guild.members().contains(playerId)) {
                return false; // Already member
            }
            
            guild.members().add(playerId);
            guild.ranks().put(playerId, GuildRank.MEMBER);
            playerGuildMap.put(playerId, guildId);
            
            persistGuild(guild);
            return true;
        });
    }
    
    @Override
    public CompletableFuture<Boolean> removeMember(String guildId, UUID playerId) {
        return CompletableFuture.supplyAsync(() -> {
            GuildData guild = guildCache.get(guildId);
            if (guild == null) {
                return false;
            }
            
            if (playerId.equals(guild.ownerId())) {
                return false; // Can't remove owner
            }
            
            guild.members().remove(playerId);
            guild.ranks().remove(playerId);
            playerGuildMap.remove(playerId);
            
            persistGuild(guild);
            return true;
        });
    }
    
    @Override
    public CompletableFuture<Boolean> setRank(String guildId, UUID playerId, GuildRank rank) {
        return CompletableFuture.supplyAsync(() -> {
            GuildData guild = guildCache.get(guildId);
            if (guild == null || !guild.members().contains(playerId)) {
                return false;
            }
            
            guild.ranks().put(playerId, rank);
            persistGuild(guild);
            return true;
        });
    }
    
    // ... similar implementations for setGuildMotd, setGuildDescription, 
    //     setRecruitmentStatus, setGuildEmblem
    
    private void persistGuild(GuildData guild) {
        storage.saveGlobalData(GUILD_STORAGE_KEY, guild.id(), guild);
    }
}
```

### Verification Criteria - Batch 8

- [ ] `createGuild()` creates with owner as leader
- [ ] `deleteGuild()` removes guild and member mappings
- [ ] `inviteMember()` adds player to guild
- [ ] `removeMember()` removes (except owner)
- [ ] `setRank()` updates player rank
- [ ] MOTD/Description/Emblem updates persist
- [ ] Recruitment status affects invite behavior

---

## Batch 9: Registry & Quest Designer

**Effort**: 6 hours  
**Priority**: 🟢 Low  
**File**: `HytaleRegistryAccessorImpl.java`, `HytaleAssetAccessorImpl.java`

### 9.1 Registry Implementation

```java
public class HytaleRegistryAccessorImpl implements RegistryAccessor {
    
    @Override
    public List<String> getItemIds() {
        AssetRegistry registry = AssetRegistry.get();
        AssetStore<String, Item> itemStore = registry.getAssetStore("items");
        
        if (itemStore == null) {
            LOGGER.warn("Item asset store not available");
            return Collections.emptyList();
        }
        
        return new ArrayList<>(itemStore.getAssetMap().keySet());
    }
    
    @Override
    public List<String> getEntityIds() {
        // Entity configs are in asset registry
        AssetRegistry registry = AssetRegistry.get();
        AssetStore<String, EntityConfig> entityStore = 
            registry.getAssetStore("entities");
        
        if (entityStore == null) {
            return Collections.emptyList();
        }
        
        return new ArrayList<>(entityStore.getAssetMap().keySet());
    }
    
    @Override
    public List<String> getNpcRoles() {
        return NPCPlugin.get().getRoleTemplateNames(true);
    }
    
    @Override
    public List<String> getZoneIds() {
        // Zone enumeration via world generator
        // May need world-specific lookup
        LOGGER.debug("Zone enumeration requires world context");
        return Collections.emptyList();
    }
}
```

### 9.2 Asset Accessor

```java
public class HytaleAssetAccessorImpl implements AssetAccessor {
    
    @Override
    public Optional<byte[]> loadAsset(String assetPath) {
        // Try classpath first
        try (InputStream is = getClass().getResourceAsStream("/" + assetPath)) {
            if (is != null) {
                return Optional.of(is.readAllBytes());
            }
        } catch (IOException e) {
            LOGGER.debug("Asset not on classpath: {}", assetPath);
        }
        
        // Try asset registry
        AssetRegistry registry = AssetRegistry.get();
        // SDK pattern for raw asset loading TBD
        
        return Optional.empty();
    }
    
    @Override
    public Optional<String> getAssetPath(String assetId, AssetType type) {
        // Resolve asset ID to path
        return switch (type) {
            case TEXTURE -> Optional.of("textures/" + assetId + ".png");
            case MODEL -> Optional.of("models/" + assetId + ".json");
            case SOUND -> Optional.of("sounds/" + assetId + ".ogg");
            default -> Optional.empty();
        };
    }
}
```

### Verification Criteria - Batch 9

- [ ] `getItemIds()` returns item registry
- [ ] `getEntityIds()` returns entity types
- [ ] `getNpcRoles()` returns NPC templates
- [ ] `loadAsset()` loads from classpath
- [ ] Quest Designer can browse registries

---

## Implementation Checklist

### Batch 1: NPC Entity Accessor (16h) ⬜
- [ ] `getEntities()` - ECS iteration pattern
- [ ] `getEntitiesNear()` - spatial query
- [ ] `spawnEntity()` - NPCPlugin.spawnNPC()
- [ ] `damageEntity()` - EntityStatMap
- [ ] `healEntity()` - EntityStatMap
- [ ] `moveToLocation()` - DEFERRED (pathfinding)
- [ ] `setMetadata()` - cache implementation
- [ ] `getMetadata()` - cache implementation

### Batch 2: World Accessor (14h) ⬜
- [ ] `getBlockType()` - WorldChunk.getBlockType()
- [ ] `setBlock()` - WorldChunk.setBlockType()
- [ ] `isChunkLoaded()` - World.getChunkIfLoaded()
- [ ] `loadChunk()` - World.getChunkAsync()
- [ ] `getHighestBlock()` - raycast pattern
- [ ] ✅ `getWeather()` - implemented
- [ ] ✅ `getBiome()` - implemented
- [ ] ✅ `getZone()` - implemented

### Batch 3: UI Accessor (20h) ⬜
- [ ] Toast methods (3)
- [ ] Tooltip methods (3)
- [ ] Overlay methods (4)
- [ ] Sidebar methods (4)
- [ ] Animation methods (3)
- [ ] Input capture methods (2)

### Batch 4: Mount System (8h) ⬜
- [ ] `mountEntity()` - MountedComponent
- [ ] `unmountEntity()` - remove component
- [ ] `getMountedEntity()` - query component
- [ ] `getPassengers()` - MountedByComponent

### Batch 5: Storage Accessor (10h) ⬜
- [ ] File-based storage implementation
- [ ] Player data CRUD
- [ ] Global data CRUD
- [ ] Thread-safe caching

### Batch 6: Model & Animation (16h) ⬜
- [ ] `getModelAssets()` - AssetRegistry
- [ ] `getModel()` - model info
- [ ] `getAnimationSets()` - TBD
- [ ] Animation playback - DEFERRED

### Batch 7: Hologram Accessor (8h) ⬜
- [ ] World-space HyUI or NPC fallback
- [ ] Create/delete/update/move

### Batch 8: Guild Accessor (12h) ⬜
- [ ] Custom persistence layer
- [ ] All guild CRUD operations

### Batch 9: Registry & Quest Designer (6h) ⬜
- [ ] Registry enumeration
- [ ] Asset loading

---

## Risk Assessment

| Risk | Probability | Impact | Mitigation |
|------|-------------|--------|------------|
| Animation API not exposed | High | Medium | Document as deferred, log warnings |
| Pathfinding complexity | High | High | Defer until specifically needed |
| WorldChunk API changes | Low | High | Abstract behind accessor interface |
| HyUI API differences | Medium | Medium | Test with actual HyUI library |
| Guild persistence conflicts | Low | Medium | Use atomic file operations |
| Entity metadata limitations | Medium | Medium | Use in-memory cache with file backup |

---

## Document History

- **2026-02-03**: Initial creation - Exhaustive implementation plan for all adapter stubs
- **Based on**: STUB_IMPLEMENTATION_PLAN.md, HIGH_RISK_API_RESEARCH.md, SDK javadocs

---

**Last Updated**: 2026-02-03  
**Author**: HytaleArchitect  
**Status**: 📋 Planning Complete - Ready for Implementation
