# Hytale Adapter - Comprehensive Stub Implementation Plan

> **Module**: `02-adapter-hytale`  
> **Created**: 2026-01-30  
> **Status**: 📋 Planning Complete  
> **Total Stubs**: 95 `UnsupportedOperationException` + 30 `TODO` comments  
> **Estimated Effort**: 80-120 hours

---

## 📊 Executive Summary

This document outlines the complete implementation plan for all stub methods, TODOs, and unimplemented features in the Hytale Adapter module. The plan is organized by:

1. **Priority Phases** - Critical path to minimal viable functionality
2. **SDK Research Tasks** - Documentation research required before implementation
3. **Implementation Batches** - Grouped by accessor/feature area
4. **Risk Assessment** - SDK availability and complexity factors

---

## 🎯 Priority Phases

### Phase 1: Foundation (Priority: 🔴 Critical)
**Effort**: 20-25 hours  
**Goal**: Core gameplay features - entities, players, inventory

| Accessor | Stubs | SDK Dependency | Research Required |
|----------|-------|----------------|-------------------|
| HytaleNPCEntityAccessor | 12 | EntityStore, ECS components | Medium |
| HytaleStorageAccessor | 7 | Player data persistence | High |
| HytalePlayerAccessor | 1 | PlayerRef (mostly done) | Low |

### Phase 2: World Interaction (Priority: 🟠 High)
**Effort**: 15-20 hours  
**Goal**: World manipulation, blocks, environment

| Accessor | Stubs | SDK Dependency | Research Required |
|----------|-------|----------------|-------------------|
| HytaleWorldAccessor | 8 | BlockChunk, ChunkStore, Biome | High |
| Converters (Entity, Item) | 4 | Type mappings | Low |

### Phase 3: UI & Presentation (Priority: 🟡 Medium)
**Effort**: 20-25 hours  
**Goal**: Advanced UI features, holograms

| Accessor | Stubs | SDK Dependency | Research Required |
|----------|-------|----------------|-------------------|
| HytaleUIAccessor | 19 | HyUI advanced features | Medium |
| HytaleHologramAccessor | 4 | Custom (no SDK) | Low |
| HytaleModelAccessor | 14 | Model/Animation SDK | Very High |

### Phase 4: Social & Persistence (Priority: 🟢 Low)
**Effort**: 15-20 hours  
**Goal**: Guilds, storage, utilities

| Accessor | Stubs | SDK Dependency | Research Required |
|----------|-------|----------------|-------------------|
| HytaleGuildAccessor | 9 | Custom persistence | Medium |
| Utility classes | 10 | Various | Low |

---

## 🔬 SDK Research Tasks

### RESEARCH-001: Entity System (ECS Pattern)
**Priority**: 🔴 Critical  
**Estimated Time**: 4-6 hours  
**Blocks**: BATCH-001 (HytaleNPCEntityAccessor)

**Objectives**:
1. Document EntityStore component iteration pattern
2. Understand Ref<EntityStore> and ComponentAccessor usage
3. Document entity spawning via entity factory
4. Research spatial queries for `getEntitiesNear()`

**SDK Classes to Research**:
```
00-Argonath-External-Docs/javadoc/com/hypixel/hytale/server/core/universe/world/storage/EntityStore.md
00-Argonath-External-Docs/javadoc/com/hypixel/hytale/component/Store.md
00-Argonath-External-Docs/javadoc/com/hypixel/hytale/component/Ref.md
00-Argonath-External-Docs/javadoc/com/hypixel/hytale/component/ComponentAccessor.md
00-Argonath-External-Docs/javadoc/com/hypixel/hytale/server/core/entity/Entity.md
```

**Key Questions to Answer**:
- [ ] How to iterate all entities in a world?
- [ ] How to filter entities by type?
- [ ] How to get entities within radius of a point?
- [ ] How to spawn a new entity programmatically?
- [ ] What is the entity factory pattern?

**Deliverables**:
- [ ] Pattern document: `patterns/entity-iteration.md`
- [ ] Pattern document: `patterns/entity-spawning.md`
- [ ] Pattern document: `patterns/spatial-query.md`
- [ ] Code example: Entity iteration snippet

---

### RESEARCH-002: Mount System
**Priority**: 🔴 Critical  
**Estimated Time**: 2-3 hours  
**Blocks**: BATCH-001 (mount methods in HytaleNPCEntityAccessor)

**Objectives**:
1. Document MountedComponent and MountedByComponent usage
2. Understand mount/unmount API
3. Document passenger enumeration

**SDK Classes to Research**:
```
00-Argonath-External-Docs/javadoc/com/hypixel/hytale/builtin/mounts/MountedComponent.md
00-Argonath-External-Docs/javadoc/com/hypixel/hytale/builtin/mounts/MountedByComponent.md
00-Argonath-External-Docs/javadoc/com/hypixel/hytale/builtin/mounts/BlockMountAPI.md
00-Argonath-External-Docs/javadoc/com/hypixel/hytale/builtin/mounts/MountPlugin.md
00-Argonath-External-Docs/javadoc/com/hypixel/hytale/builtin/mounts/interactions/MountInteraction.md
```

**Key Questions to Answer**:
- [ ] How to mount an entity onto another?
- [ ] How to unmount an entity?
- [ ] How to get the entity a player is mounted on?
- [ ] How to get all passengers of a mount?
- [ ] Is mounting handled via components or API calls?

**Deliverables**:
- [ ] Pattern document: `patterns/mount-unmount.md`
- [ ] Pattern document: `patterns/passenger-enumeration.md`

---

### RESEARCH-003: Block & Chunk System
**Priority**: 🟠 High  
**Estimated Time**: 4-5 hours  
**Blocks**: BATCH-002 (HytaleWorldAccessor)

**Objectives**:
1. Document BlockChunk read/write operations
2. Understand ChunkStore chunk loading
3. Research block type enumeration
4. Document SetBlockSettings usage

**SDK Classes to Research**:
```
00-Argonath-External-Docs/javadoc/com/hypixel/hytale/server/core/modules/block/BlockChunk.md
00-Argonath-External-Docs/javadoc/com/hypixel/hytale/server/core/modules/block/BlockModule.md
00-Argonath-External-Docs/javadoc/com/hypixel/hytale/server/core/universe/world/chunk/ChunkStore.md
00-Argonath-External-Docs/javadoc/com/hypixel/hytale/server/core/universe/world/chunk/Chunk.md
00-Argonath-External-Docs/javadoc/com/hypixel/hytale/server/core/modules/block/SetBlockSettings.md
```

**Key Questions to Answer**:
- [ ] How to get block type at coordinates?
- [ ] How to set block at coordinates?
- [ ] How to check if a chunk is loaded?
- [ ] How to load/unload a chunk?
- [ ] What is SetBlockSettings and when to use it?

**Deliverables**:
- [ ] Pattern document: `patterns/block-operations.md`
- [ ] Pattern document: `patterns/chunk-management.md`

---

### RESEARCH-004: Biome & Weather System
**Priority**: 🟠 High  
**Estimated Time**: 3-4 hours  
**Blocks**: BATCH-002 (biome/weather methods)

**Objectives**:
1. Document biome lookup at coordinates
2. Understand weather state access
3. Research zone/region system (if exists)

**SDK Classes to Research**:
```
00-Argonath-External-Docs/javadoc/com/hypixel/hytale/builtin/hytalegenerator/biome/BiomeType.md
00-Argonath-External-Docs/javadoc/com/hypixel/hytale/builtin/hytalegenerator/biomemap/BiomeMap.md
00-Argonath-External-Docs/javadoc/com/hypixel/hytale/builtin/hytalegenerator/assets/biomes/BiomeAsset.md
00-Argonath-External-Docs/javadoc/com/hypixel/hytale/builtin/adventure/objectives/config/triggercondition/WeatherTriggerCondition.md
```

**Key Questions to Answer**:
- [ ] How to get biome at world coordinates?
- [ ] How to get current weather state?
- [ ] Is there a zone/region system in the SDK?
- [ ] Can weather be modified programmatically?

**Deliverables**:
- [ ] Pattern document: `patterns/biome-lookup.md`
- [ ] Pattern document: `patterns/weather-state.md`
- [ ] Assessment: Zone/Region API availability

---

### RESEARCH-005: Model & Animation System
**Priority**: 🟡 Medium  
**Estimated Time**: 4-6 hours  
**Blocks**: BATCH-004 (HytaleModelAccessor)

**Objectives**:
1. Document Model and ModelReference access
2. Understand animation set enumeration
3. Research animation playback API
4. Document sound event attachment

**SDK Classes to Research**:
```
00-Argonath-External-Docs/javadoc/com/hypixel/hytale/server/core/asset/type/model/config/Model.md
00-Argonath-External-Docs/javadoc/com/hypixel/hytale/server/core/asset/type/model/config/Model$ModelReference.md
00-Argonath-External-Docs/javadoc/com/hypixel/hytale/server/core/modules/entity/component/ (search for AnimationComponent)
```

**Key Questions to Answer**:
- [ ] How to list all available models?
- [ ] How to get animations for a model?
- [ ] How to play an animation on an entity?
- [ ] How to stop an animation?
- [ ] How to get animation duration?
- [ ] Is there an AnimationComponent?

**Risk Assessment**:
- Animation playback may not be exposed in SDK
- May need to defer to future SDK version

**Deliverables**:
- [ ] Pattern document: `patterns/model-access.md`
- [ ] Pattern document: `patterns/animation-playback.md` (if possible)
- [ ] Risk document: `patterns/animation-risk-assessment.md`

---

### RESEARCH-006: Storage & Persistence
**Priority**: 🔴 Critical  
**Estimated Time**: 3-4 hours  
**Blocks**: BATCH-005 (HytaleStorageAccessor)

**Objectives**:
1. Document player data persistence pattern
2. Understand BsonDocument usage for entity metadata
3. Research custom data storage options

**SDK Classes to Research**:
```
Search for: BsonDocument, codec, persistence, PlayerData
00-Argonath-External-Docs/javadoc/com/hypixel/hytale/codec/*.md
00-Argonath-External-Docs/javadoc/com/hypixel/hytale/server/core/universe/world/storage/*.md
```

**Key Questions to Answer**:
- [ ] How does Hytale persist player data?
- [ ] Can plugins store custom data per player?
- [ ] How to use BsonDocument for metadata?
- [ ] Is there a key-value storage API?

**Alternative Approach**:
If SDK doesn't provide persistence, use:
- `03-framework-storage` with file-based backend
- SQLite via JDBC (if available)

**Deliverables**:
- [ ] Pattern document: `patterns/player-persistence.md`
- [ ] Pattern document: `patterns/entity-metadata.md`
- [ ] Architecture decision: Storage backend choice

---

### RESEARCH-007: Pathfinding System
**Priority**: 🟡 Medium  
**Estimated Time**: 2-3 hours  
**Blocks**: BATCH-001 (moveToLocation method)

**Objectives**:
1. Document NPC movement/navigation API
2. Understand pathfinding component (if exists)
3. Research entity AI behavior system

**SDK Classes to Research**:
```
Search for: Navigation, Pathfind, AI, Behavior
00-Argonath-External-Docs/javadoc/com/hypixel/hytale/builtin/adventure/npc/*.md
```

**Key Questions to Answer**:
- [ ] Is there a pathfinding API?
- [ ] How to make an entity move to a location?
- [ ] Is there a NavigationComponent?
- [ ] Can we hook into entity AI?

**Fallback Plan**:
If no pathfinding API exists:
- Implement simple linear movement (teleport steps)
- Log warning about no pathfinding
- Document as "partial implementation"

**Deliverables**:
- [ ] Pattern document: `patterns/npc-navigation.md`
- [ ] Assessment: Pathfinding API availability

---

### RESEARCH-008: Damage & Stats System
**Priority**: 🔴 Critical  
**Estimated Time**: 2-3 hours  
**Blocks**: BATCH-001 (damage/heal methods)

**Objectives**:
1. Document EntityStatMap access pattern
2. Understand damage application API
3. Research health modification

**SDK Classes to Research**:
```
00-Argonath-External-Docs/javadoc/com/hypixel/hytale/server/core/modules/entitystats/EntityStatMap.md
00-Argonath-External-Docs/javadoc/com/hypixel/hytale/server/core/modules/entitystats/EntityStatValue.md
00-Argonath-External-Docs/javadoc/com/hypixel/hytale/server/core/modules/entitystats/EntityStatMapComponent.md
Search for: Damage, DamageSource, Health
```

**Key Questions to Answer**:
- [ ] How to get/set health via EntityStatMap?
- [ ] What stat indices are health/maxHealth?
- [ ] How to apply damage to an entity?
- [ ] Is there a DamageSource system?

**Deliverables**:
- [ ] Pattern document: `patterns/entity-stats.md`
- [ ] Pattern document: `patterns/damage-application.md`

---

### RESEARCH-009: Registry & Asset System
**Priority**: 🟠 High  
**Estimated Time**: 3-4 hours  
**Blocks**: BATCH-009 (Quest Designer)

**Objectives**:
1. Document ItemRegistry/EntityRegistry access
2. Understand AssetStore enumeration
3. Research asset loading API

**SDK Classes to Research**:
```
00-Argonath-External-Docs/javadoc/com/hypixel/hytale/assetstore/AssetRegistry.md
00-Argonath-External-Docs/javadoc/com/hypixel/hytale/assetstore/AssetStore.md
00-Argonath-External-Docs/javadoc/com/hypixel/hytale/server/core/asset/type/item/config/Item.md
Search for: ItemRegistry, EntityRegistry, EntityConfig
```

**Key Questions to Answer**:
- [ ] How to list all registered items?
- [ ] How to list all registered entities?
- [ ] How to get item/entity by ID?
- [ ] How to load asset icons/textures?

**Deliverables**:
- [ ] Pattern document: `patterns/registry-access.md`
- [ ] Pattern document: `patterns/asset-loading.md`

---

## 📦 Implementation Batches

### BATCH-001: HytaleNPCEntityAccessor (12 stubs)
**Dependencies**: RESEARCH-001, RESEARCH-002, RESEARCH-007, RESEARCH-008  
**Effort**: 12-16 hours

| Method | SDK Pattern | Complexity | Research Dep |
|--------|-------------|------------|--------------|
| `getEntities(worldName)` | EntityStore iteration | Medium | R-001 |
| `getEntitiesNear(location, radius)` | Spatial query | High | R-001 |
| `spawnEntity(type, location)` | Entity factory | High | R-001 |
| `damageEntity(entityId, amount)` | DamageComponent/EntityStatMap | Medium | R-008 |
| `healEntity(entityId, amount)` | EntityStatMap | Low | R-008 |
| `moveToLocation(entityId, target)` | Pathfinding/Navigation | High | R-007 |
| `setMetadata(entityId, key, value)` | BsonDocument | Medium | R-006 |
| `getMetadata(entityId, key)` | BsonDocument | Low | R-006 |
| `mountEntity(riderId, mountId)` | MountedComponent | Medium | R-002 |
| `unmountEntity(riderId)` | MountedComponent removal | Low | R-002 |
| `getMountedEntity(riderId)` | MountedComponent.getMountedToEntity() | Low | R-002 |
| `getPassengers(mountId)` | MountedByComponent.getPassengers() | Low | R-002 |

**Implementation Order**:
1. First: `getEntities`, `getEntitiesNear` (enables testing)
2. Second: `damageEntity`, `healEntity` (combat critical)
3. Third: Mount operations (mount module dependency)
4. Last: `spawnEntity`, `moveToLocation` (complex)

---

### BATCH-002: HytaleWorldAccessor (8 stubs)
**Dependencies**: RESEARCH-003, RESEARCH-004  
**Effort**: 10-14 hours

| Method | SDK Pattern | Complexity | Research Dep |
|--------|-------------|------------|--------------|
| `getBiome(location)` | BiomeMap lookup | Medium | R-004 |
| `getZone(location)` | Zone API (may not exist) | Unknown | R-004 |
| `getWeather(worldName)` | WeatherTriggerCondition | Medium | R-004 |
| `getBlockType(location)` | BlockChunk.getBlockType() | Low | R-003 |
| `getHighestBlock(x, z)` | Raycast/iteration | Medium | R-003 |
| `isChunkLoaded(x, z)` | ChunkStore.isLoaded() | Low | R-003 |
| `loadChunk(x, z)` | ChunkStore.load() | Low | R-003 |
| `setBlock(location, type)` | BlockChunk + SetBlockSettings | Medium | R-003 |

**Implementation Order**:
1. First: `getBlockType`, `setBlock` (core block ops)
2. Second: `isChunkLoaded`, `loadChunk` (chunk management)
3. Third: `getBiome`, `getWeather` (environment)
4. Last: `getHighestBlock`, `getZone` (complex/unknown)

---

### BATCH-003: HytaleUIAccessor (19 stubs)
**Dependencies**: HyUI documentation (already available)  
**Effort**: 15-20 hours

| Category | Methods | SDK Pattern | Complexity |
|----------|---------|-------------|------------|
| **Toast** | `showToast`, `hideToast`, `updateToast` | HyUI Toast API | Low |
| **Tooltip** | `showTooltip`, `hideTooltip`, `updateTooltip` | HyUI Tooltip API | Low |
| **Overlay** | `showOverlay`, `hideOverlay`, `updateOverlay`, `hasOverlay` | HyUI Overlay API | Medium |
| **Sidebar** | `showSidebar`, `hideSidebar`, `updateSidebar`, `hasSidebar` | HyUI Sidebar API | Medium |
| **Animation** | `playUIAnimation`, `stopUIAnimation`, `getAnimationState` | HyUI Animation API | Medium |
| **Input** | `captureInput`, `releaseInput` | HyUI Input handling | Medium |

**Implementation Order**:
1. First: Toast, Tooltip (simple UI elements)
2. Second: Overlay, Sidebar (layout elements)
3. Last: Animation, Input (interactive elements)

**Reference**: `00-Argonath-External-Docs/HyUI/docs/`

---

### BATCH-004: HytaleModelAccessor (14 stubs)
**Dependencies**: RESEARCH-005  
**Effort**: 12-16 hours

| Method | SDK Pattern | Complexity |
|--------|-------------|------------|
| `getModelAssets()` | AssetStore enumeration | Medium |
| `getModel(modelId)` | Asset lookup | Low |
| `getAnimationSets(modelId)` | Model.getAnimations() | Medium |
| `getAnimation(modelId, animId)` | Animation lookup | Low |
| `playAnimation(entityId, anim)` | AnimationComponent | High |
| `stopAnimation(entityId)` | AnimationComponent | Medium |
| `getAnimationDuration(anim)` | Animation metadata | Low |
| `getAnimationsWithSounds(modelId)` | Sound event filtering | Medium |
| `getModelBounds(modelId)` | Model bounds | Medium |
| `getModelAttachPoints(modelId)` | Attachment points | Medium |
| `getModelMaterials(modelId)` | Material enumeration | Medium |
| `setEntityModel(entityId, modelId)` | Model component | High |
| `getEntityModel(entityId)` | Model component read | Low |
| `validateModel(modelId)` | Validation | Low |

**Risk**: Model/Animation API may not be fully exposed in SDK. May need workarounds or stub placeholders.

---

### BATCH-005: HytaleStorageAccessor (7 stubs)
**Dependencies**: RESEARCH-006  
**Effort**: 8-10 hours

| Method | SDK Pattern | Complexity |
|--------|-------------|------------|
| `savePlayerData(uuid, data)` | BsonDocument persistence | Medium |
| `loadPlayerData(uuid)` | BsonDocument load | Medium |
| `saveGlobalData(key, data)` | File/DB storage | Medium |
| `loadGlobalData(key)` | File/DB load | Medium |
| `deletePlayerData(uuid)` | File/DB delete | Low |
| `listPlayerData()` | Directory enumeration | Low |
| `hasPlayerData(uuid)` | File existence check | Low |

**Pattern**: Use `03-framework-storage` abstraction with Hytale-specific implementation.

---

### BATCH-006: HytaleGuildAccessor (9 stubs)
**Dependencies**: BATCH-005 (storage)  
**Effort**: 8-12 hours

| Method | SDK Pattern | Complexity |
|--------|-------------|------------|
| `createGuild(name, owner)` | Custom persistence | Medium |
| `deleteGuild(guildId)` | Custom persistence | Low |
| `inviteMember(guildId, player)` | Custom persistence | Low |
| `removeMember(guildId, player)` | Custom persistence | Low |
| `setRank(guildId, player, rank)` | Custom persistence | Low |
| `setGuildMotd(guildId, motd)` | Custom persistence | Low |
| `setGuildDescription(guildId, desc)` | Custom persistence | Low |
| `setRecruitmentStatus(guildId, status)` | Custom persistence | Low |
| `setGuildEmblem(guildId, emblem)` | Custom persistence | Low |

**Note**: Guild system is entirely custom - no SDK dependency. Uses storage accessor for persistence.

---

### BATCH-007: HytaleHologramAccessor (4 stubs)
**Dependencies**: None (custom implementation)  
**Effort**: 6-8 hours

| Method | SDK Pattern | Complexity |
|--------|-------------|------------|
| `createHologram(location, lines)` | Custom entity or armor stand | High |
| `deleteHologram(id)` | Entity removal | Low |
| `updateHologram(id, lines)` | Entity metadata update | Medium |
| `moveHologram(id, location)` | Entity teleport | Low |

**Implementation Options**:
1. Use invisible entities with display name text
2. Use floating text entities (if SDK supports)
3. Use HyUI world-space UI elements

---

### BATCH-008: Utility & Converter Classes (10 stubs)
**Dependencies**: Earlier batches  
**Effort**: 6-8 hours

| Class | Stubs | Description |
|-------|-------|-------------|
| `BsonConverter` | 2 | BSON ↔ DataValue conversion |
| `EntityDataConverter` | 2 | Entity ↔ EntityData DTO |
| `ItemDataConverter` | 2 | Item ↔ ItemData DTO |
| `PlayerRefCache` | 2 | PlayerRef caching |
| `ComponentHelper` | 1 | ECS component access helpers |
| `HytaleWorldExecutor` | 3 | Scheduled task execution |

---

### BATCH-009: Quest Designer Integration (6 TODOs)
**Dependencies**: RESEARCH-009  
**Effort**: 4-6 hours

| Class | TODOs | Description |
|-------|-------|-------------|
| `HytaleRegistryAccessorImpl` | 4 | Item/Entity/NPC/Zone registry |
| `HytaleAssetAccessorImpl` | 2 | Asset loading |

---

## 📅 Implementation Timeline

### Week 1: Research Phase (20-25 hours)
| Day | Task | Deliverables |
|-----|------|--------------|
| 1 | RESEARCH-001: Entity System | Pattern documents |
| 2 | RESEARCH-002: Mount System | Pattern documents |
| 3 | RESEARCH-003: Block/Chunk System | Pattern documents |
| 4 | RESEARCH-008: Damage/Stats System | Pattern documents |
| 5 | RESEARCH-006: Storage/Persistence | Pattern documents |

### Week 2: Critical Path (20-25 hours)
| Day | Task | Deliverables |
|-----|------|--------------|
| 1-2 | BATCH-001: NPC Entity (first 6 methods) | Working entity iteration |
| 3 | BATCH-001: Mount operations (4 methods) | Working mount system |
| 4 | BATCH-002: Block ops (first 4 methods) | Working block ops |
| 5 | BATCH-005: Storage (core methods) | Working persistence |

### Week 3: Core Features (20-25 hours)
| Day | Task | Deliverables |
|-----|------|--------------|
| 1 | BATCH-001: Remaining methods | Complete entity accessor |
| 2 | BATCH-002: Remaining methods | Complete world accessor |
| 3-4 | BATCH-006: Guild (all methods) | Complete guild accessor |
| 5 | BATCH-008: Utilities | Complete utilities |

### Week 4: UI & Polish (15-20 hours)
| Day | Task | Deliverables |
|-----|------|--------------|
| 1-2 | BATCH-003: UI Accessor (first 10 methods) | Core UI features |
| 3 | BATCH-003: UI Accessor (remaining) | Complete UI accessor |
| 4 | BATCH-007: Hologram | Complete hologram accessor |
| 5 | Testing & Documentation | Integration tests |

### Week 5: Advanced Features (15-20 hours)
| Day | Task | Deliverables |
|-----|------|--------------|
| 1-2 | RESEARCH-005 + BATCH-004 (if possible) | Model accessor |
| 3 | BATCH-009: Quest Designer | Registry integration |
| 4-5 | Final testing & cleanup | Release candidate |

---

## ⚠️ Risk Assessment

### High Risk Items

| Item | Risk | Mitigation |
|------|------|------------|
| Model/Animation API | SDK may not expose animation playback | Stub with logging, defer to future SDK version |
| Pathfinding API | May not exist in current SDK | Implement simple linear movement, flag as partial |
| Zone/Region System | May not exist | Return null/empty, document as unsupported |
| Hologram System | No SDK support | Custom implementation with floating entities |
| Entity Spawning | Factory pattern may differ | Research EntityFactory thoroughly |

### SDK Availability Matrix

| Feature | SDK Status | Confidence | Notes |
|---------|------------|------------|-------|
| Entity System (ECS) | ✅ Documented | High | EntityStore, Ref, Component pattern |
| Mount System | ✅ Documented | High | MountedComponent, MountedByComponent |
| Block/Chunk System | ✅ Documented | High | BlockChunk, ChunkStore |
| Biome System | ✅ Documented | Medium | BiomeMap, BiomeType |
| Weather System | 🟡 Partial | Medium | WeatherTriggerCondition exists |
| EntityStats | ✅ Documented | High | EntityStatMap, EntityStatValue |
| Pathfinding | ❓ Unknown | Low | May not exist |
| Model/Animation | 🟡 Partial | Medium | Model exists, animation playback unclear |
| Storage/Persistence | 🟡 Custom needed | Medium | May need file-based fallback |
| Registry Access | ✅ Documented | High | AssetRegistry, AssetStore |

---

## ✅ Completion Criteria

### Per-Accessor Criteria
- [ ] All methods implemented (no `UnsupportedOperationException`)
- [ ] All TODOs resolved or converted to GitHub issues
- [ ] Javadoc for each public method
- [ ] Unit tests for each public method
- [ ] Integration tests for accessor as a whole

### Module Criteria
- [ ] BUILD SUCCESS with 0 errors, 0 warnings
- [ ] All 95 stubs resolved
- [ ] All 30 TODOs resolved
- [ ] IMPLEMENTATION_TRACKING.md updated to 100%
- [ ] CHANGELOG.md updated with implementation entries
- [ ] Pattern documents created for each research task

---

## 📁 Deliverable Structure

```
02-adapter-hytale/
├── docs/
│   └── patterns/                    # SDK pattern documents
│       ├── entity-iteration.md
│       ├── entity-spawning.md
│       ├── spatial-query.md
│       ├── mount-unmount.md
│       ├── passenger-enumeration.md
│       ├── block-operations.md
│       ├── chunk-management.md
│       ├── biome-lookup.md
│       ├── weather-state.md
│       ├── entity-stats.md
│       ├── damage-application.md
│       ├── player-persistence.md
│       ├── entity-metadata.md
│       ├── npc-navigation.md
│       ├── model-access.md
│       ├── animation-playback.md
│       ├── registry-access.md
│       └── asset-loading.md
├── src/
│   ├── main/java/...               # Implementations
│   └── test/java/...               # Unit + Integration tests
├── IMPLEMENTATION_TRACKING.md       # Updated tracking
├── IMPLEMENTATION_PLAN.md           # Original plan (archived)
├── STUB_IMPLEMENTATION_PLAN.md      # This document
└── CHANGELOG.md                     # Updated changelog
```

---

## 📚 Reference Documentation

### SDK Documentation
- `00-Argonath-External-Docs/javadoc/` - 7195 classes documented
- `00-Argonath-External-Docs/hytale-sdk/` - Official SDK docs
- `00-Argonath-External-Docs/HyUI/docs/` - HyUI documentation

### Architecture References
- `00-Argonath-Specifications/00-Architecture/` - C4 models
- `00-Argonath-Wiki/docs/` - Developer documentation

---

## 🔄 Update History

| Date | Update | Author |
|------|--------|--------|
| 2026-01-30 | Comprehensive plan created with SDK research tasks | HytaleArchitect |
