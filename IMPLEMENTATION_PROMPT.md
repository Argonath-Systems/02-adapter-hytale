# Hytale Adapter Implementation Prompt

**Purpose**: This document contains the prompt to be used with the HytaleModder agent to implement the remaining stubs in the `02-adapter-hytale` module.

---

## Master Implementation Prompt

```
You are implementing the remaining stubs and unimplemented methods in the Hytale Adapter layer (`02-adapter-hytale`). This is a critical module that bridges the platform-agnostic accessor interfaces with the actual Hytale SDK APIs.

## Context

- **Module**: `02-adapter-hytale`
- **Reference Document**: `02-adapter-hytale/EXHAUSTIVE_STUB_IMPLEMENTATION_PLAN.md`
- **SDK Documentation**: `00-Argonath-External-Docs/hytale-sdk/extract/md/`
- **HyUI Documentation**: `00-Argonath-External-Docs/HyUI/docs/`
- **Architecture Pattern**: Accessor interfaces in `02-framework-accessor`, implementations in `02-adapter-hytale`

## Implementation Priorities

Implement in this order:

### Priority 1: Critical Path (Batches 1, 2, 5)

1. **NPC Entity Accessor** (`HytaleNPCEntityAccessor.java`)
   - `getEntities(worldName)` - Iterate EntityStore using ECS pattern
   - `getEntitiesNear(location, radius)` - Spatial query with distance filter
   - `spawnEntity(type, location)` - Use `NPCPlugin.get().spawnNPC()`
   - `damageEntity(entityId, amount)` - Modify EntityStatMap health
   - `healEntity(entityId, amount)` - Modify EntityStatMap health (capped at max)
   - `setMetadata(entityId, key, value)` - Use ConcurrentHashMap cache
   - `getMetadata(entityId, key)` - Retrieve from cache

2. **World Accessor** (`HytaleWorldAccessor.java`)
   - `getBlockType(location)` - Use `WorldChunk.getBlockType()`
   - `setBlock(location, type)` - Use `WorldChunk.setBlockType()` with SetBlockSettings
   - `isChunkLoaded(worldName, x, z)` - Check `World.getChunkIfLoaded()`
   - `loadChunk(worldName, x, z)` - Use `World.getChunkAsync()`
   - `getHighestBlock(worldName, x, z)` - Raycast from top down

3. **Storage Accessor** (`HytaleStorageAccessor.java`)
   - Implement file-based JSON persistence
   - `savePlayerData(uuid, key, data)` - Write to `data/players/{uuid}.json`
   - `loadPlayerData(uuid, key)` - Read from player JSON
   - `deletePlayerData(uuid)` - Remove player file
   - `listPlayerData()` - Enumerate player directory
   - `hasPlayerData(uuid)` - Check file existence
   - Global data methods similarly

### Priority 2: High Priority (Batch 4)

4. **Mount System** (in `HytaleEntityAccessor.java` or new `HytaleMountAccessor.java`)
   - `mountEntity(riderId, mountId)` - Add MountedComponent to rider, update MountedByComponent on mount
   - `unmountEntity(riderId)` - Remove MountedComponent, update MountedByComponent
   - `getMountedEntity(riderId)` - Query MountedComponent.getMountedToEntity()
   - `getPassengers(mountId)` - Query MountedByComponent.getPassengers()

### Priority 3: Medium Priority (Batches 3, 6, 8)

5. **UI Accessor** (`HytaleUIAccessor.java`)
   - Toast methods: `showToast()`, `hideToast()`, `updateToast()`
   - Tooltip methods: `showTooltip()`, `hideTooltip()`, `updateTooltip()`
   - Overlay methods: `showOverlay()`, `hideOverlay()`, `updateOverlay()`, `hasOverlay()`
   - Sidebar methods: `showSidebar()`, `hideSidebar()`, `updateSidebar()`, `hasSidebar()`
   - Animation methods: `playUIAnimation()`, `stopUIAnimation()`, `getAnimationState()`
   - Input methods: `captureInput()`, `releaseInput()`

6. **Model & Animation Accessor** (`HytaleModelAnimationAccessor.java`)
   - `getModelAssets()` - Enumerate via AssetRegistry
   - `getModel(modelId)` - Lookup model info
   - `getAnimationSets(modelId)` - List animations (if SDK exposes)
   - Animation playback methods - Log warning and throw UnsupportedOperationException (deferred)

7. **Guild Accessor** (`HytaleGuildAccessor.java`)
   - Implement all guild CRUD using StorageAccessor
   - Member management, rank updates, MOTD/description/emblem

### Priority 4: Low Priority (Batches 7, 9)

8. **Hologram Accessor** (`HytaleHologramAccessor.java`)
   - Try world-space HyUI first, fallback to invisible NPC with display name

9. **Registry Accessor** (`HytaleRegistryAccessorImpl.java`)
   - `getItemIds()`, `getEntityIds()`, `getNpcRoles()` - Use AssetRegistry and NPCPlugin

## SDK Classes Reference

### Entity System
```java
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.entity.Entity;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.ComponentAccessor;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMapComponent;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
```

### NPC System
```java
import com.hypixel.hytale.server.npc.NPCPlugin;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import com.hypixel.hytale.server.core.universe.world.npc.INonPlayerCharacter;
import it.unimi.dsi.fastutil.Pair;
```

### Mount System
```java
import com.hypixel.hytale.builtin.mounts.MountPlugin;
import com.hypixel.hytale.builtin.mounts.MountedComponent;
import com.hypixel.hytale.builtin.mounts.MountedByComponent;
import com.hypixel.hytale.builtin.mounts.NPCMountComponent;
import com.hypixel.hytale.protocol.MountController;
```

### Block System
```java
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.modules.block.BlockModule;
import com.hypixel.hytale.server.core.modules.block.SetBlockSettings;
```

## Implementation Patterns

### ECS Entity Iteration
```java
world.execute(() -> {
    Store<EntityStore> store = world.getEntityStore();
    store.forEach((Ref<EntityStore> ref, ComponentAccessor<EntityStore> accessor) -> {
        Entity entity = accessor.get(Entity.getComponentType());
        if (entity != null) {
            // Process entity
        }
    });
});
```

### NPC Spawning
```java
Pair<Ref<EntityStore>, INonPlayerCharacter> result = NPCPlugin.get().spawnNPC(
    store,           // Store<EntityStore>
    "role_name",     // Role template name
    null,            // Group name (optional)
    position,        // Vector3d
    rotation         // Vector3f
);
```

### Component Access
```java
MountedComponent mounted = accessor.get(MountPlugin.getInstance().getMountedComponentType());
if (mounted != null) {
    Ref<EntityStore> mountRef = mounted.getMountedToEntity();
}
```

### World Thread Execution
```java
world.execute(() -> {
    // All SDK calls must be on world thread
});
```

## Constraints

1. **Thread Safety**: All SDK calls must execute on the world thread via `world.execute()`
2. **Null Checks**: Always check for null world, null entity, null component
3. **Accessor Pattern**: No `hytale.*` imports outside the adapter layer
4. **Logging**: Use SLF4J Logger for debug, warn, error messages
5. **Graceful Degradation**: Return empty collections/Optional.empty() on failure, don't throw

## Deferred Items (Do NOT Implement)

- `moveToLocation()` - Pathfinding too complex, throw UnsupportedOperationException with message
- `playAnimation()` / `stopAnimation()` - SDK API not confirmed, throw with message
- `stopSound()` - SDK doesn't support, return false with log

## Testing

After implementing each batch:
1. Run `just build` to verify compilation
2. Check for any new deprecation warnings
3. Update `IMPLEMENTATION_TRACKING.md` with completed items

## Deliverables

For each implemented method, provide:
1. The complete implementation code
2. Any new imports required
3. Any helper methods created
4. Update to IMPLEMENTATION_TRACKING.md status
```

---

## Batch-Specific Prompts

### Prompt: Batch 1 - NPC Entity Accessor

```
Implement the NPC Entity Accessor methods in `02-adapter-hytale`.

**Target File**: Find and update `HytaleNPCEntityAccessor.java` or `HytaleEntityAccessor.java`

**Methods to Implement**:
1. `getEntities(String worldName)` → `List<EntityData>`
2. `getEntitiesNear(LocationData location, double radius)` → `List<EntityData>`
3. `spawnEntity(String entityType, LocationData location)` → `Optional<EntityData>`
4. `damageEntity(UUID entityId, double amount)` → `boolean`
5. `healEntity(UUID entityId, double amount)` → `boolean`
6. `setMetadata(UUID entityId, String key, Object value)` → `void`
7. `getMetadata(UUID entityId, String key)` → `Optional<Object>`

**SDK Reference**:
- Read `00-Argonath-External-Docs/hytale-sdk/extract/md/com/hypixel/hytale/server/npc/NPCPlugin.md`
- Read `00-Argonath-External-Docs/hytale-sdk/extract/md/com/hypixel/hytale/server/core/universe/world/World.md`
- Read `00-Argonath-External-Docs/hytale-sdk/extract/md/com/hypixel/hytale/server/core/modules/entitystats/EntityStatMap.md`

**Implementation Plan Reference**: See EXHAUSTIVE_STUB_IMPLEMENTATION_PLAN.md Batch 1

**After Implementation**:
- Run `just build` in 02-adapter-hytale
- Update IMPLEMENTATION_TRACKING.md
```

### Prompt: Batch 2 - World Accessor

```
Implement the World Accessor block/chunk methods in `02-adapter-hytale`.

**Target File**: `HytaleWorldAccessor.java`

**Methods to Implement**:
1. `getBlockType(LocationData location)` → `String`
2. `setBlock(LocationData location, String blockType)` → `boolean`
3. `isChunkLoaded(String worldName, int chunkX, int chunkZ)` → `boolean`
4. `loadChunk(String worldName, int chunkX, int chunkZ)` → `CompletableFuture<Boolean>`
5. `getHighestBlock(String worldName, int x, int z)` → `int`

**SDK Reference**:
- Read `00-Argonath-External-Docs/hytale-sdk/extract/md/com/hypixel/hytale/server/core/universe/world/chunk/WorldChunk.md`
- Read `00-Argonath-External-Docs/hytale-sdk/extract/md/com/hypixel/hytale/server/core/modules/block/BlockModule.md`

**Note**: `getBiome()`, `getZone()`, `hasWeather()` are already implemented.

**Implementation Plan Reference**: See EXHAUSTIVE_STUB_IMPLEMENTATION_PLAN.md Batch 2
```

### Prompt: Batch 3 - UI Accessor

```
Implement the UI Accessor HyUI methods in `02-adapter-hytale`.

**Target File**: `HytaleUIAccessor.java`

**Methods to Implement**:
- Toast: `showToast()`, `hideToast()`, `updateToast()`
- Tooltip: `showTooltip()`, `hideTooltip()`, `updateTooltip()`
- Overlay: `showOverlay()`, `hideOverlay()`, `updateOverlay()`, `hasOverlay()`
- Sidebar: `showSidebar()`, `hideSidebar()`, `updateSidebar()`, `hasSidebar()`
- Animation: `playUIAnimation()`, `stopUIAnimation()`, `getAnimationState()`
- Input: `captureInput()`, `releaseInput()`

**HyUI Reference**:
- Read `00-Argonath-External-Docs/HyUI/docs/`
- Fetch https://hyui.gitbook.io/docs/ for latest API patterns

**Pattern**: Use HyUI builder APIs (ToastBuilder, OverlayBuilder, etc.)

**State Tracking**: Maintain Maps for active toasts/overlays/sidebars per player UUID

**Implementation Plan Reference**: See EXHAUSTIVE_STUB_IMPLEMENTATION_PLAN.md Batch 3
```

### Prompt: Batch 4 - Mount System

```
Implement the Mount System methods in `02-adapter-hytale`.

**Target File**: Create `HytaleMountAccessor.java` or add to `HytaleEntityAccessor.java`

**Methods to Implement**:
1. `mountEntity(UUID riderId, UUID mountId)` → `boolean`
2. `unmountEntity(UUID riderId)` → `boolean`
3. `getMountedEntity(UUID riderId)` → `Optional<UUID>`
4. `getPassengers(UUID mountId)` → `List<UUID>`

**SDK Reference**:
- Read `00-Argonath-External-Docs/hytale-sdk/extract/md/com/hypixel/hytale/builtin/mounts/MountPlugin.md`
- Read `00-Argonath-External-Docs/hytale-sdk/extract/md/com/hypixel/hytale/builtin/mounts/MountedComponent.md`
- Read `00-Argonath-External-Docs/hytale-sdk/extract/md/com/hypixel/hytale/builtin/mounts/MountedByComponent.md`

**Key Pattern**:
- Rider gets MountedComponent with reference to mount
- Mount gets MountedByComponent with passenger list
- Use MountPlugin.getInstance() for ComponentTypes

**Implementation Plan Reference**: See EXHAUSTIVE_STUB_IMPLEMENTATION_PLAN.md Batch 4
```

### Prompt: Batch 5 - Storage Accessor

```
Implement the Storage Accessor with file-based persistence in `02-adapter-hytale`.

**Target File**: `HytaleStorageAccessor.java`

**Methods to Implement**:
1. `savePlayerData(UUID playerId, String key, Object data)` → `CompletableFuture<Void>`
2. `loadPlayerData(UUID playerId, String key)` → `CompletableFuture<Optional<Object>>`
3. `deletePlayerData(UUID playerId)` → `CompletableFuture<Void>`
4. `listPlayerData()` → `CompletableFuture<List<UUID>>`
5. `hasPlayerData(UUID playerId)` → `CompletableFuture<Boolean>`
6. Global data equivalents

**Implementation Pattern**:
- Use Gson for JSON serialization
- Store in `data/players/{uuid}.json` and `data/global/{key}.json`
- Use ConcurrentHashMap for in-memory caching
- CompletableFuture.runAsync/supplyAsync for async operations

**Thread Safety**: All file operations on async executor, cache operations thread-safe

**Implementation Plan Reference**: See EXHAUSTIVE_STUB_IMPLEMENTATION_PLAN.md Batch 5
```

### Prompt: Batch 6 - Model & Animation

```
Implement the Model & Animation Accessor in `02-adapter-hytale`.

**Target File**: `HytaleModelAnimationAccessor.java`

**Methods to Implement (Partial)**:
1. `getModelAssets()` → `List<String>` - Use AssetRegistry
2. `getModel(String modelId)` → `Optional<ModelInfo>` - Lookup model
3. `getAnimationSets(String modelId)` → `List<String>` - If SDK exposes

**Methods to DEFER (throw UnsupportedOperationException)**:
- `playAnimation()` - Log warning, throw with message about SDK research needed
- `stopAnimation()` - Same
- `getAnimationDuration()` - Same

**SDK Reference**:
- Search for Model, ModelAsset, AnimationComponent in SDK docs

**Implementation Plan Reference**: See EXHAUSTIVE_STUB_IMPLEMENTATION_PLAN.md Batch 6
```

### Prompt: Batch 7 - Hologram Accessor

```
Implement the Hologram Accessor in `02-adapter-hytale`.

**Target File**: `HytaleHologramAccessor.java`

**Methods to Implement**:
1. `createHologram(LocationData location, List<String> lines)` → `String` (hologramId)
2. `deleteHologram(String hologramId)` → `void`
3. `updateHologram(String hologramId, List<String> lines)` → `void`
4. `moveHologram(String hologramId, LocationData newLocation)` → `void`

**Implementation Options** (try in order):
1. World-space HyUI elements (if available)
2. Invisible NPC with custom display name
3. Armor stand with name (if SDK supports)

**State Tracking**: Map<String, HologramData> for active holograms

**Implementation Plan Reference**: See EXHAUSTIVE_STUB_IMPLEMENTATION_PLAN.md Batch 7
```

### Prompt: Batch 8 - Guild Accessor

```
Implement the Guild Accessor with custom persistence in `02-adapter-hytale`.

**Target File**: `HytaleGuildAccessor.java`

**Methods to Implement**:
1. `createGuild(String name, UUID ownerId)` → `CompletableFuture<GuildData>`
2. `deleteGuild(String guildId)` → `CompletableFuture<Boolean>`
3. `inviteMember(String guildId, UUID playerId)` → `CompletableFuture<Boolean>`
4. `removeMember(String guildId, UUID playerId)` → `CompletableFuture<Boolean>`
5. `setRank(String guildId, UUID playerId, GuildRank rank)` → `CompletableFuture<Boolean>`
6. `setGuildMotd(String guildId, String motd)` → `CompletableFuture<Boolean>`
7. `setGuildDescription(String guildId, String description)` → `CompletableFuture<Boolean>`
8. `setRecruitmentStatus(String guildId, RecruitmentStatus status)` → `CompletableFuture<Boolean>`
9. `setGuildEmblem(String guildId, EmblemData emblem)` → `CompletableFuture<Boolean>`

**Dependency**: Uses StorageAccessor (Batch 5) for persistence

**State Tracking**:
- `Map<String, GuildData> guildCache`
- `Map<UUID, String> playerGuildMap`

**Implementation Plan Reference**: See EXHAUSTIVE_STUB_IMPLEMENTATION_PLAN.md Batch 8
```

### Prompt: Batch 9 - Registry Accessor

```
Implement the Registry Accessor for Quest Designer in `02-adapter-hytale`.

**Target File**: `HytaleRegistryAccessorImpl.java`

**Methods to Implement**:
1. `getItemIds()` → `List<String>` - Use AssetRegistry item store
2. `getEntityIds()` → `List<String>` - Use AssetRegistry entity store
3. `getNpcRoles()` → `List<String>` - Use NPCPlugin.getRoleTemplateNames()
4. `getZoneIds()` → `List<String>` - May need world context, stub if needed

**Also Update**: `HytaleAssetAccessorImpl.java`
1. `loadAsset(String assetPath)` → `Optional<byte[]>` - Try classpath, then AssetRegistry
2. `getAssetPath(String assetId, AssetType type)` → `Optional<String>` - Resolve ID to path

**SDK Reference**:
- NPCPlugin.get().getRoleTemplateNames(boolean includeUnspawnable)
- AssetRegistry pattern from previous implementations

**Implementation Plan Reference**: See EXHAUSTIVE_STUB_IMPLEMENTATION_PLAN.md Batch 9
```

---

## Multi-Hotbar Implementation Prompt

```
Implement the Multi-Hotbar System for `06-mod-dual-hotbar` (to be renamed `06-mod-multi-hotbar`).

**Reference Document**: `06-mod-dual-hotbar/MULTI_HOTBAR_IMPLEMENTATION_PLAN.md`
**Specification**: `00-Argonath-Specifications/SM-UI-050-multi-hotbar-system.md`

**Implementation Phases**:

1. **Phase 1: Native Hotbar Hiding** (7h)
   - Add `HudComponentType` enum to `02-framework-accessor`
   - Add `hideHudComponents()/showHudComponents()` to UIAccessor
   - Implement in `HytaleUIAccessorImpl` using `HudManager.hideHudComponents()`
   - Update `MultiHotbarManager.enableForPlayer()/disableForPlayer()`

2. **Phase 2: HyUI Rendering** (15h)
   - Refactor `MultiHotbarHud.java` with ItemGrid for open slots
   - Implement slot type rendering (OPEN, LOCKED, EMPTY)
   - Add selection indicators and cooldown overlays
   - Create CSS stylesheet

3. **Phase 3: Input Interception** (12h)
   - Create `HotbarPacketFilter` for `SyncInteractionChains`
   - Intercept `InteractionType.SwapTo` for slot selection
   - Route to appropriate handler based on slot type

4. **Phase 4: Inventory Blocking** (11h)
   - Create `InventoryBlockListener` for `LivingEntityInventoryChangeEvent`
   - Block drag/shift-click to LOCKED/EMPTY slots
   - Add player feedback

5. **Phase 5: Action Execution** (11h)
   - Implement `ActionExecutor` with COMMAND/EVENT/MENU/HANDLER types
   - Add cooldown tracking
   - Placeholder processing

6. **Phase 6: Sound Feedback** (6h)
   - Integrate `SoundAccessor` for selection/action/error sounds

7. **Phase 7: Texture Management** (10h)
   - Create `TextureManager` with WatchService hot-reload
   - Create required texture assets

**SDK Classes**: See MULTI_HOTBAR_IMPLEMENTATION_PLAN.md SDK Reference Index

**After Each Phase**:
- Run `just build` to verify compilation
- Test in-game if possible
- Update IMPLEMENTATION_TRACKING.md
```

---

## Usage Instructions

1. **Copy the Master Prompt** to start a new HytaleModder session
2. **Use Batch-Specific Prompts** for focused implementation sessions
3. **Reference the EXHAUSTIVE_STUB_IMPLEMENTATION_PLAN.md** for detailed patterns
4. **Check SDK docs** in `00-Argonath-External-Docs/hytale-sdk/extract/md/` for exact signatures
5. **Update tracking** after each batch completion

---

**Last Updated**: 2026-02-03  
**Author**: HytaleArchitect
