# Accessor Stub Implementation Plan

> **Module**: `02-adapter-hytale`  
> **Date**: 2026-01-31  
> **Author**: HytaleArchitect Agent  
> **Objective**: Comprehensive review of all stubs, TODOs, and unimplemented methods with SDK-based implementation strategy

---

## 📊 Executive Summary

### Total Issues Identified

| Category | Count | Priority |
|----------|-------|----------|
| `UnsupportedOperationException` throws | 52 | 🔴 Critical |
| `// TODO` comments | 35 | 🟡 High |
| Stub implementations (return null/empty) | ~15 | 🟡 High |
| Deprecated API usage | 4 | 🟢 Low |
| **Total Implementation Gaps** | **~106** | - |

### SDK Coverage Analysis

Based on analysis of the Hytale SDK documentation (7192 classes):

| API Area | SDK Available | Implementation Feasibility |
|----------|---------------|---------------------------|
| Entity/ECS System | ✅ Full | HIGH - Component pattern documented |
| Inventory/Items | ✅ Full | HIGH - `ItemStack`, `Inventory` available |
| World/Chunks | ✅ Full | HIGH - `World`, `WorldChunk` available |
| Particles | ✅ Full | HIGH - `ParticleUtil` implemented |
| Camera Effects | ⚠️ Partial | MEDIUM - `CameraShakeEffect` only |
| Model/Animation | ❌ Limited | LOW - Asset system, no runtime control |
| Hologram | ❌ None | BLOCKED - No hologram entity type |
| Render Preview | ❌ None | BLOCKED - Server-side only |
| Permissions | ❌ None | BLOCKED - Need custom implementation |

---

## 🔴 Phase 1: Critical Implementations (SDK Available)

### 1.1 HytaleHologramAccessor (4 methods)

**Current State**: All 4 methods throw `UnsupportedOperationException`

**File**: [HytaleHologramAccessor.java](src/main/java/com/argonathsystems/adapter/hytaleadapter/accessor/HytaleHologramAccessor.java)

| Method | SDK Approach | Feasibility |
|--------|--------------|-------------|
| `createHologram()` | Use NPC with invisible model + nameplate | 🟡 Workaround |
| `updateHologram()` | Update NPC nameplate text | 🟡 Workaround |
| `moveHologram()` | `TransformComponent.teleportPosition()` | 🟢 Feasible |
| `removeHologram()` | `Entity.remove()` | 🟢 Feasible |

**Implementation Strategy**:
```java
// Create hologram as invisible NPC with nameplate
public HologramHandle createHologram(LocationData location, List<String> lines) {
    World world = Universe.get().getWorld(location.world());
    EntityStore store = world.getEntityStore();
    
    // Spawn invisible entity with nameplate component
    // Use NPCPlugin to spawn with no visible model
    NPCSpawnData spawnData = NPCSpawnData.builder()
        .position(LocationConverter.toVector3d(location))
        .role("hologram") // Define hologram role asset
        .build();
    
    Entity entity = NPCPlugin.spawnNPC(store, spawnData);
    
    // Set nameplate with multi-line text
    NameplateComponent nameplate = store.getComponent(entity.getReference(), NameplateComponent.class);
    nameplate.setText(String.join("\n", lines));
    
    return new HologramHandle(entity.getUuid());
}
```

**Blockers**: Need to verify `NameplateComponent` supports multi-line text

---

### 1.2 HytaleModelAccessor (14 methods)

**Current State**: All 14 methods throw `UnsupportedOperationException`

**File**: [HytaleModelAccessor.java](src/main/java/com/argonathsystems/adapter/hytaleadapter/accessor/HytaleModelAccessor.java)

| Method | SDK Support | Notes |
|--------|-------------|-------|
| `spawnModel()` | ⚠️ Partial | Entity spawn with model role |
| `removeModel()` | ✅ Full | `Entity.remove()` |
| `playAnimation()` | ❌ None | No runtime animation API |
| `stopAnimation()` | ❌ None | No runtime animation API |
| `stopAllAnimations()` | ❌ None | No runtime animation API |
| `setModel()` | ❌ None | Model assigned at spawn only |
| `setSkin()` | ❌ None | No skin swap API |
| `setModelVariant()` | ❌ None | Variant in asset definition |
| `setEquipment()` | ⚠️ Partial | Equipment slots via inventory |
| `clearEquipment()` | ⚠️ Partial | Clear slots via inventory |
| `setScale()` | ❌ None | No transform scale API |
| `setGlowing()` | ❌ None | No visual effects API |
| `setGlowColor()` | ❌ None | No visual effects API |
| `isModelLoaded()` | ⚠️ Partial | Check entity exists |

**Implementation Strategy**:

**Implementable (3 methods)**:
```java
@Override
public void removeModel(UUID modelId) {
    World world = Universe.get().getDefaultWorld();
    Entity entity = world.getEntity(modelId);
    if (entity != null && !entity.wasRemoved()) {
        entity.remove();
        LOGGER.debug("Removed model entity: {}", modelId);
    }
}

@Override
public boolean isModelLoaded(UUID modelId) {
    World world = Universe.get().getDefaultWorld();
    Entity entity = world.getEntity(modelId);
    return entity != null && !entity.wasRemoved();
}

@Override
public Optional<UUID> spawnModel(String modelId, LocationData location) {
    World world = Universe.get().getWorld(location.world());
    if (world == null) return Optional.empty();
    
    // Use entity spawn with model reference
    // Requires model role asset to be defined
    EntityStore store = world.getEntityStore().getStore();
    // ... spawn entity with modelId as role
    return Optional.of(entity.getUuid());
}
```

**Blocked (11 methods)** - Require future SDK extensions for:
- Runtime animation control
- Dynamic model/skin changes
- Transform scale modification
- Visual effects (glow, particles)

**Recommendation**: Create `HIGH_RISK_API_WISHLIST.md` for Hytale SDK feature requests

---

### 1.3 Data Converters (4 methods)

**Files**: 
- [EntityDataConverter.java](src/main/java/com/argonathsystems/adapter/hytaleadapter/converter/EntityDataConverter.java)
- [ItemDataConverter.java](src/main/java/com/argonathsystems/adapter/hytaleadapter/converter/ItemDataConverter.java)

| Converter | Method | SDK Approach |
|-----------|--------|--------------|
| EntityDataConverter | `toHytale(EntityData)` | Build component set for EntityStore |
| EntityDataConverter | `toFramework(Entity)` | Extract from TransformComponent, etc. |
| ItemDataConverter | `toHytale(ItemData)` | Create ItemStack from item ID + meta |
| ItemDataConverter | `toFramework(ItemStack)` | Extract item ID, quantity, metadata |

**Implementation**:
```java
// EntityDataConverter
public Entity toHytale(EntityData data, World world) {
    EntityStore store = world.getEntityStore();
    Store<EntityStore> componentStore = store.getStore();
    
    Vector3d position = new Vector3d(data.x(), data.y(), data.z());
    // Spawn via NPC plugin or entity factory
    return NPCPlugin.spawnNPC(componentStore, position, data.type());
}

public EntityData toFramework(Entity entity) {
    TransformComponent transform = entity.getTransformComponent();
    Vector3d pos = transform.getPosition();
    
    return new EntityData(
        entity.getUuid(),
        entity.getType(), // or role name
        pos.getX(), pos.getY(), pos.getZ(),
        // ... other fields
    );
}

// ItemDataConverter
public ItemStack toHytale(ItemData item) {
    return ItemStack.of(item.id(), item.amount());
}

public ItemData toFramework(ItemStack stack) {
    return new ItemData(
        stack.getItemId(),
        stack.getQuantity(),
        stack.getDurability(),
        extractMetadata(stack.getMetadata())
    );
}
```

---

## 🟡 Phase 2: High Priority TODOs

### 2.1 HytaleCameraAccessor (9 TODOs)

**File**: [HytaleCameraAccessor.java](src/main/java/com/argonathsystems/adapter/hytale/accessor/HytaleCameraAccessor.java)

| TODO Location | Current State | SDK Status |
|---------------|---------------|------------|
| L81: Apply camera state | Internal state only | ❌ No API |
| L97: Set camera position | Internal state only | ❌ No API |
| L108: Set camera rotation | Internal state only | ❌ No API |
| L157: Apply letterbox | Internal state only | ❌ No API |
| L175: Apply DoF | Internal state only | ❌ No API |
| L192: Apply FOV | Internal state only | ❌ No API |
| L217: Detach from player | Internal state only | ❌ No API |
| L240: Reattach to player | Internal state only | ❌ No API |

**Implemented**:
- `applyShake()` ✅ - Uses `CameraShakeEffect` packet

**Recommendation**: 
- Keep internal state tracking for testing
- Document as "stub mode" until camera control API available
- Add feature request to SDK wishlist

---

### 2.2 HytaleBlockAccessor (4 TODOs)

**File**: [HytaleBlockAccessor.java](src/main/java/com/argonathsystems/adapter/hytaleadapter/accessor/HytaleBlockAccessor.java)

| TODO | Line | Implementation Path |
|------|------|---------------------|
| Block type query | L194 | `WorldChunk.getBlockType(x,y,z)` |
| Chunk scanning | L217 | Iterate `WorldChunk` positions |
| Spatial search | L225 | Iterate chunks in radius |
| Solid block detection | L245 | Check block type properties |

**Implementation**:
```java
@Override
public boolean isContainer(String worldId, int x, int y, int z) {
    String blockType = getBlockType(worldId, x, y, z);
    return CONTAINER_TYPES.contains(blockType);
}

@Override
public List<LocationData> findContainersInChunk(String worldId, int chunkX, int chunkZ) {
    World world = getWorld(worldId);
    if (world == null) return Collections.emptyList();
    
    long chunkIndex = packChunkIndex(chunkX, chunkZ);
    WorldChunk chunk = world.getChunk(chunkIndex);
    if (chunk == null) return Collections.emptyList();
    
    List<LocationData> containers = new ArrayList<>();
    for (int localX = 0; localX < 16; localX++) {
        for (int y = 0; y < 256; y++) {
            for (int localZ = 0; localZ < 16; localZ++) {
                BlockType type = chunk.getBlockType(localX, y, localZ);
                if (CONTAINER_TYPES.contains(type.getId())) {
                    int globalX = chunkX * 16 + localX;
                    int globalZ = chunkZ * 16 + localZ;
                    containers.add(new LocationData(worldId, globalX, y, globalZ));
                }
            }
        }
    }
    return containers;
}

@Override
public boolean isSolid(String worldId, int x, int y, int z) {
    String blockType = getBlockType(worldId, x, y, z);
    return !NON_SOLID_BLOCKS.contains(blockType);
}
```

---

### 2.3 HytaleInventoryAccessor (2 TODOs)

**File**: [HytaleInventoryAccessor.java](src/main/java/com/argonathsystems/adapter/hytaleadapter/accessor/HytaleInventoryAccessor.java)

| TODO | Line | Resolution |
|------|------|------------|
| Refresh container | L318 | Use `player.sendInventory()` |
| HyUI ItemGridBuilder | L337 | Requires HyUI API research |

**HyUI Integration**:
```java
@Override
public void openCustomInventory(UUID playerId, String inventoryId, List<ItemData> contents) {
    PlayerRef playerRef = Universe.get().getPlayer(playerId);
    if (playerRef == null) return;
    
    // Create HyUI page with ItemGrid
    HyUIPage page = PageBuilder.create()
        .title(inventoryId)
        .content(container -> {
            ItemGridBuilder grid = container.itemGrid()
                .rows(3)
                .columns(9);
            
            for (int i = 0; i < contents.size(); i++) {
                ItemData item = contents.get(i);
                ItemStack stack = fromItemData(item);
                grid.slot(i, slot -> slot.item(stack));
            }
            
            return grid.build();
        })
        .build();
    
    // Open page for player
    HyUIManager.open(playerRef, page);
}
```

---

### 2.4 HytaleNPCEntityAccessor (1 TODO)

**File**: [HytaleNPCEntityAccessor.java](src/main/java/com/argonathsystems/adapter/hytaleadapter/accessor/HytaleNPCEntityAccessor.java)

| TODO | Line | Resolution |
|------|------|------------|
| Pathfinding API | L421 | Use NPC Brain/GoalSelector pattern |

**SDK Research Required**:
- Check `com.hypixel.hytale.builtin.path` package for pathfinding
- `WorldPathBuilder`, `PatrolPath`, `PrefabPathCollection` classes exist
- May require custom `ActionNavigateTo` implementation

**Proposed Implementation**:
```java
@Override
public void navigateTo(UUID entityId, LocationData target) {
    Entity entity = getEntity(entityId);
    if (entity == null) return;
    
    // Option 1: Use TransientPath for single-target navigation
    TransientPathDefinition path = TransientPathDefinition.builder()
        .addWaypoint(LocationConverter.toVector3d(target))
        .build();
    
    // Option 2: Trigger navigation via NPC action system
    // Requires NPCBrainComponent or similar
    
    LOGGER.debug("Navigate entity {} to {}", entityId, target);
}
```

---

### 2.5 HytaleRenderAccessor (8 TODOs)

**File**: [HytaleRenderAccessor.java](src/main/java/com/argonathsystems/adapter/hytaleadapter/accessor/HytaleRenderAccessor.java)

**Current State**: All methods are stubs with internal state tracking

**SDK Status**: ❌ No server-side 3D render preview API

**Resolution**:
1. Keep stub implementation for API contract
2. Document as "client-side only feature"
3. Consider HyUI-based alternative (2D item/model previews)

---

## 🟡 Phase 3: Quest Format Converter

**File**: [QuestFormatConverter.java](src/main/java/com/argonathsystems/adapter/hytale/integration/QuestFormatConverter.java)

### Methods Throwing UnsupportedOperationException

| Method | Purpose | Implementation Effort |
|--------|---------|----------------------|
| `toFrameworkFormat()` | WebUI → Game | MEDIUM |
| `toHyQuestFormat()` | Game → WebUI | MEDIUM |

### Implementation Strategy

```java
public QuestDefinition toFrameworkFormat(HyQuestDefinition hyQuest) {
    // 1. Build node lookup map
    Map<String, QuestNode> nodeMap = hyQuest.getNodes().stream()
        .collect(Collectors.toMap(QuestNode::getId, Function.identity()));
    
    // 2. Build edge adjacency
    Map<String, List<String>> adjacency = buildAdjacencyMap(hyQuest.getEdges());
    
    // 3. Find root node
    QuestNode root = findRootNode(nodeMap);
    
    // 4. Traverse graph to build linear stages
    List<QuestStage> stages = new ArrayList<>();
    QuestNode current = root;
    while (current != null) {
        stages.add(convertToStage(current, nodeMap, adjacency));
        current = getNextStageNode(current, adjacency, nodeMap);
    }
    
    // 5. Build framework definition
    return QuestDefinition.builder()
        .id(hyQuest.getId())
        .metadata(convertMetadata(hyQuest.getMetadata()))
        .stages(stages)
        .build();
}
```

---

## 🟢 Phase 4: Low Priority / Deferred

### 4.1 Deprecated API Usage

| File | API | Replacement |
|------|-----|-------------|
| HytaleNPCEntityAccessor | `getTransformComponent()` | ECS component lookup |
| HytaleNPCEntityAccessor | `getUuid()` | `UUIDComponent` lookup |
| HytaleNPCEntityAccessor | `getLegacyDisplayName()` | Codec/Nameplate component |
| PlayerConverter | `getUuid()` | `UUIDComponent` lookup |

**Resolution**: Migrate to ECS pattern when component access is stabilized

### 4.2 PlayerRefCache

**File**: [PlayerRefCache.java](src/main/java/com/argonathsystems/adapter/hytale/util/PlayerRefCache.java)

**Current**: Throws `UnsupportedOperationException`

**Implementation**:
```java
public PlayerRef getOrCreate(UUID playerId) {
    return cache.computeIfAbsent(playerId, id -> 
        Universe.get().getPlayer(id)
    );
}
```

---

## 📋 Implementation Priority Matrix

| Priority | Category | Items | Effort | SDK Ready |
|----------|----------|-------|--------|-----------|
| P0 | Data Converters | 4 | LOW | ✅ |
| P0 | Block Operations | 4 | MEDIUM | ✅ |
| P0 | Hologram (workaround) | 4 | MEDIUM | ⚠️ |
| P1 | Inventory HyUI | 2 | MEDIUM | ✅ |
| P1 | Quest Converter | 2 | HIGH | ✅ |
| P1 | NPC Pathfinding | 1 | HIGH | ⚠️ |
| P2 | Camera Effects | 9 | BLOCKED | ❌ |
| P2 | Render Preview | 8 | BLOCKED | ❌ |
| P2 | Model Control | 11 | BLOCKED | ❌ |

---

## 🚀 Recommended Next Steps

### Immediate Actions (Week 1)
1. ✅ Implement `EntityDataConverter` and `ItemDataConverter`
2. ✅ Complete `HytaleBlockAccessor` TODOs
3. ✅ Implement `QuestFormatConverter` with full graph traversal
4. ✅ Add `PlayerRefCache` implementation

### Short-term (Week 2-3)
1. 🔄 Implement hologram via invisible NPC + nameplate
2. 🔄 Add HyUI `ItemGridBuilder` integration
3. 🔄 Research pathfinding API for NPC navigation
4. 🔄 Document blocked APIs in SDK wishlist

### Long-term (SDK Updates Required)
1. ⏳ Camera control API (position, rotation, FOV, DoF)
2. ⏳ Runtime animation control
3. ⏳ Model/skin swapping
4. ⏳ Visual effects (glow, scale)
5. ⏳ 3D render preview for UI

---

## 📚 SDK Reference Index

### Key Packages for Implementation

| Package | Purpose |
|---------|---------|
| `com.hypixel.hytale.server.core.entity` | Entity, EntityStore |
| `com.hypixel.hytale.server.core.inventory` | Inventory, ItemStack, ItemContainer |
| `com.hypixel.hytale.server.core.universe` | Universe, World, PlayerRef |
| `com.hypixel.hytale.server.core.universe.world` | WorldChunk, ParticleUtil |
| `com.hypixel.hytale.server.npc` | NPCPlugin, NPC spawning |
| `com.hypixel.hytale.builtin.path` | Path, WorldPathBuilder |
| `com.hypixel.hytale.builtin.mounts` | MountedComponent, MountedByComponent |
| `com.hypixel.hytale.protocol.packets.camera` | CameraShakeEffect |
| `com.hypixel.hytale.component` | Store, Ref, ComponentAccessor |
| `au.ellie.hyui.builders` | HyUIPage, ItemGridBuilder, ContainerBuilder |

### Documentation Sources
- **Javadoc**: `00-Argonath-External-Docs/javadoc/` (7192 classes)
- **HyUI**: `00-Argonath-External-Docs/HyUI/docs/`
- **SDK Site**: `00-Argonath-External-Docs/hytale-sdk/site/`

---

## 📝 Change Log

| Date | Version | Changes |
|------|---------|---------|
| 2026-01-31 | 1.0.0 | Initial comprehensive audit and plan |

