# HIGH RISK API Research Document

**Date:** 2025-01-14
**Status:** Research Complete
**Purpose:** Document SDK APIs required for deferred HIGH RISK method implementations

---

## Executive Summary

This document captures research findings for the 4 HIGH RISK method categories that were deferred during Phase 5 implementation. Each category requires complex SDK integration with multiple interdependent components.

| Category | Methods | Complexity | SDK Package |
|----------|---------|------------|-------------|
| Pathfinding | `navigateTo()` | Very High | `server.npc.navigation.*`, `server.npc.movement.controllers.*` |
| Biome | `getBiomeAt()` | High | `builtin.hytalegenerator.biome.*`, `builtin.hytalegenerator.assets.biomes.*` |
| Zone/Region | `getZoneAt()`, `getRegionAt()` | High | `server.core.universe.world.worldmap.*` |
| Weather | `getWeather()`, `setWeather()` | Medium-High | `builtin.weather.*` |

---

## 1. Pathfinding API (navigateTo)

### Overview
The Hytale SDK provides a comprehensive A* pathfinding system for NPC navigation.

### Key Classes

#### 1.1 AStarWithTarget (Core Pathfinder)
**Package:** `com.hypixel.hytale.server.npc.navigation.AStarWithTarget`

```java
// Primary pathfinding entry point
public Progress initComputePath(
    Ref<EntityStore> entityRef,
    Vector3d startPos,
    Vector3d targetPos,
    AStarEvaluator evaluator,
    MotionController motionController,
    ProbeMoveData probeMoveData,
    AStarNodePoolProvider nodePoolProvider,
    ComponentAccessor<EntityStore> componentAccessor
);

// Compute next step
public Progress computePath(int maxTime, Ref<EntityStore> ref, ComponentAccessor<EntityStore> accessor);
```

**Returns:** `Progress` enum with values like `COMPLETE`, `IN_PROGRESS`, `FAILED`

#### 1.2 PathFollower (Path Execution)
**Package:** `com.hypixel.hytale.server.npc.navigation.PathFollower`

```java
// Set a computed path for execution
public void setPath(IWaypoint waypoint, Vector3d position);

// Execute one step of path following
public ExecutePathResult executePath(
    Vector3d currentPosition, 
    MotionController motionController, 
    Steering steering
);

// Clear current path
public void clearPath();

// Smooth the path for more natural movement
public IWaypoint smoothPath(
    Ref<EntityStore> entityRef, 
    MotionController motionController, 
    IWaypoint waypoint, 
    ComponentAccessor<EntityStore> accessor
);
```

#### 1.3 IWaypoint (Path Point Interface)
**Package:** `com.hypixel.hytale.server.npc.navigation.IWaypoint`

```java
public interface IWaypoint {
    Vector3d getPosition();   // Get waypoint world position
    IWaypoint next();         // Get next waypoint in path
    void advance(int steps);  // Skip ahead in path
    int getLength();          // Total waypoints remaining
}
```

#### 1.4 MotionController (Movement Types)
**Package:** `com.hypixel.hytale.server.npc.movement.controllers.*`

| Controller | Use Case |
|------------|----------|
| `MotionControllerWalk` | Ground-based NPCs (humanoids, animals) |
| `MotionControllerFly` | Flying NPCs (birds, dragons) |
| `MotionControllerDive` | Swimming/underwater NPCs |
| `MotionControllerHover` | Stationary flying NPCs |

Key methods:
```java
public interface MotionController {
    String getType();
    double getMaximumSpeed();
    boolean isInProgress();
    boolean onGround();
    boolean inWater();
    boolean inAir();
    
    double steer(
        Ref<EntityStore> entityRef, 
        Role role, 
        Steering current, 
        Steering target, 
        double deltaTime, 
        ComponentAccessor<EntityStore> accessor
    );
    
    double probeMove(
        Ref<EntityStore> entityRef, 
        Vector3d from, 
        Vector3d to, 
        ProbeMoveData data, 
        ComponentAccessor<EntityStore> accessor
    );
}
```

### Implementation Pattern for navigateTo()

```java
// Pseudocode pattern discovered from SDK
public void navigateTo(UUID entityId, double x, double y, double z) {
    // 1. Get entity reference
    Ref<EntityStore> entityRef = getEntityRef(entityId);
    
    // 2. Get MotionController from entity's NPC role
    Role role = entityRef.get(RoleComponent.getComponentType()).getRole();
    MotionController controller = role.getMotionController();
    
    // 3. Get or create PathFollower from entity
    PathFollower pathFollower = getOrCreatePathFollower(entityRef);
    
    // 4. Create AStarWithTarget and compute path
    AStarWithTarget pathfinder = new AStarWithTarget();
    Vector3d start = getEntityPosition(entityRef);
    Vector3d target = new Vector3d(x, y, z);
    
    // 5. Init path computation (may take multiple ticks)
    Progress progress = pathfinder.initComputePath(
        entityRef, start, target,
        AStarEvaluator.DEFAULT,  // or custom evaluator
        controller,
        new ProbeMoveData(),
        nodePoolProvider,
        componentAccessor
    );
    
    // 6. Continue computing until complete
    while (progress == Progress.IN_PROGRESS) {
        progress = pathfinder.computePath(MAX_TIME_PER_TICK, entityRef, accessor);
    }
    
    // 7. If successful, get waypoints and set path
    if (progress == Progress.COMPLETE) {
        IWaypoint path = pathfinder.getPath();
        pathFollower.setPath(path, start);
    }
}
```

### Risk Assessment
- **Complexity:** Very High - requires understanding of A* algorithm, async computation, tick-based execution
- **Dependencies:** MotionController type must match entity capabilities
- **Threading:** Path computation may span multiple ticks
- **Testing:** Requires loaded world chunks for navigation mesh

---

## 2. Biome API (getBiomeAt)

### Overview
Biome data is primarily used during world generation. Runtime biome queries require accessing the generator cache.

### Key Classes

#### 2.1 BiomeAsset (Configuration)
**Package:** `com.hypixel.hytale.builtin.hytalegenerator.assets.biomes.BiomeAsset`

```java
public class BiomeAsset {
    // Get biome ID
    public String getId();
    
    // Get human-readable name
    public String getBiomeName();
    
    // Build runtime BiomeType
    public BiomeType build(
        MaterialCache materialCache,
        SeedBox seedBox,
        ReferenceBundle refs,
        WorkerIndexer indexer
    );
    
    // Get all loaded biomes
    public static AssetStore<String, BiomeAsset, ...> getAssetStore();
}
```

#### 2.2 BiomeType (Runtime Interface)
**Package:** `com.hypixel.hytale.builtin.hytalegenerator.biome.BiomeType`

```java
public interface BiomeType extends MaterialSource, PropsSource, EnvironmentSource, TintSource {
    String getBiomeName();
    Density getTerrainDensity();
}
```

#### 2.3 BiomeInterpolation
**Package:** `com.hypixel.hytale.server.worldgen.biome.BiomeInterpolation`

This class likely contains the runtime biome query logic for interpolating between biomes at coordinates.

### Implementation Pattern for getBiomeAt()

```java
// Pseudocode pattern - requires further investigation
public String getBiomeAt(String worldName, int x, int y, int z) {
    World world = getWorld(worldName);
    
    // Option 1: Access via ChunkGeneratorCache
    ChunkGeneratorCache cache = world.getWorldGenerator().getCache();
    BiomeType biome = cache.getBiomeAt(x, y, z);
    return biome.getBiomeName();
    
    // Option 2: Access via BiomePatternGenerator
    BiomePatternGenerator generator = ...;
    BiomeType biome = generator.getBiomeAt(x, z); // May be 2D only
    return biome.getBiomeName();
}
```

### Risk Assessment
- **Complexity:** High - generation-time vs runtime access unclear
- **Dependencies:** Requires world generator access
- **Performance:** May involve chunk loading/caching
- **Note:** Biome may be Y-independent (2D) in Hytale

---

## 3. Zone/Region API (getZoneAt, getRegionAt)

### Overview
The SDK provides WorldMapManager for managing map regions and markers.

### Key Classes

#### 3.1 WorldMapManager
**Package:** `com.hypixel.hytale.server.core.universe.world.worldmap.WorldMapManager`

```java
public class WorldMapManager {
    // Get world reference
    public World getWorld();
    
    // Check if world map is enabled
    public boolean isWorldMapEnabled();
    
    // Get map settings
    public WorldMapSettings getWorldMapSettings();
    
    // Get points of interest
    public Map<String, MapMarker> getPointsOfInterest();
    
    // Add marker provider for custom markers
    public void addMarkerProvider(String id, MarkerProvider provider);
    
    // Get marker providers
    public Map<String, MarkerProvider> getMarkerProviders();
    
    // Create player marker
    public static PlayerMarkerReference createPlayerMarker(
        Ref<EntityStore> entityRef,
        MapMarker marker,
        ComponentAccessor<EntityStore> accessor
    );
}
```

#### 3.2 IWorldMap (Map Generation Interface)
**Package:** `com.hypixel.hytale.server.core.universe.world.worldmap.IWorldMap`

```java
public interface IWorldMap {
    WorldMapSettings getWorldMapSettings();
    
    CompletableFuture<WorldMap> generate(
        World world, 
        int x, 
        int z, 
        LongSet chunkKeys
    );
    
    CompletableFuture<Map<String, MapMarker>> generatePointsOfInterest(World world);
}
```

### Implementation Pattern for getZoneAt()

```java
// Pseudocode - zones may be custom implementation
public String getZoneAt(String worldName, int x, int y, int z) {
    World world = getWorld(worldName);
    WorldMapManager mapManager = world.getWorldMapManager();
    
    // Check map markers/POIs
    Map<String, MapMarker> pois = mapManager.getPointsOfInterest();
    
    // Find containing zone - may require custom zone registry
    for (MapMarker poi : pois.values()) {
        if (poi.contains(x, y, z)) {
            return poi.getName();
        }
    }
    
    return "wilderness"; // Default zone
}
```

### Risk Assessment
- **Complexity:** High - WorldMapManager is for map display, not zone logic
- **Note:** Zone/Region may be a custom Argonath concept (not SDK built-in)
- **Alternative:** May need 04-framework-protection for region definitions

---

## 4. Weather API (getWeather, setWeather)

### Overview
The SDK has a comprehensive weather system via WeatherPlugin.

### Key Classes

#### 4.1 WeatherPlugin (Main Entry Point)
**Package:** `com.hypixel.hytale.builtin.weather.WeatherPlugin`

```java
public class WeatherPlugin extends JavaPlugin {
    // Singleton access
    public static WeatherPlugin get();
    
    // Get weather tracker component type
    public ComponentType<EntityStore, WeatherTracker> getWeatherTrackerComponentType();
    
    // Get weather resource type
    public ResourceType<EntityStore, WeatherResource> getWeatherResourceType();
}
```

#### 4.2 WeatherTracker (Per-Entity Weather State)
**Package:** `com.hypixel.hytale.builtin.weather.components.WeatherTracker`

```java
public class WeatherTracker implements Component<EntityStore> {
    // Get current weather index
    public int getWeatherIndex();
    
    // Set weather for player
    public void setWeatherIndex(PlayerRef playerRef, int index);
    
    // Get environment ID
    public int getEnvironmentId();
    
    // Update weather
    public void updateWeather(
        PlayerRef playerRef,
        WeatherResource resource,
        TransformComponent transform,
        float deltaTime,
        ComponentAccessor<EntityStore> accessor
    );
}
```

#### 4.3 WeatherResource (World Weather State)
**Package:** `com.hypixel.hytale.builtin.weather.resources.WeatherResource`

```java
public class WeatherResource implements Resource<EntityStore> {
    // Get weather for specific environment
    public int getWeatherIndexForEnvironment(int environmentId);
    
    // Get forced weather (overridden)
    public int getForcedWeatherIndex();
    
    // Set forced weather by name
    public void setForcedWeather(String weatherName);
    
    // Get all environment weather mappings
    public Int2IntMap getEnvironmentWeather();
}
```

### Implementation Pattern for getWeather()/setWeather()

```java
// Get weather at player's location
public String getWeather(UUID entityId) {
    Ref<EntityStore> entityRef = getEntityRef(entityId);
    WeatherTracker tracker = entityRef.get(WeatherTracker.getComponentType());
    int weatherIndex = tracker.getWeatherIndex();
    return resolveWeatherName(weatherIndex);
}

// Set weather for world
public void setWeather(String worldName, String weatherType) {
    World world = getWorld(worldName);
    
    // Access world's WeatherResource
    world.execute(() -> {
        Store<EntityStore> store = world.getStore();
        WeatherResource resource = store.getResource(WeatherResource.getResourceType());
        resource.setForcedWeather(weatherType);
    });
}
```

### Risk Assessment
- **Complexity:** Medium-High - well-documented but requires component access
- **Dependencies:** WeatherPlugin must be loaded
- **Note:** Weather may be per-environment, not per-world

---

## Recommendations

### 1. Priority Order
1. **Weather API** - Most straightforward, good first implementation
2. **Biome API** - May require world generator integration
3. **Zone/Region API** - May need custom Argonath implementation
4. **Pathfinding API** - Most complex, defer until needed

### 2. Next Steps
1. Create stub implementations that throw `UnsupportedOperationException`
2. Implement Weather API first as proof-of-concept
3. Investigate BiomePatternGenerator for runtime biome queries
4. Determine if Zone/Region is SDK or custom concept
5. Create pathfinding prototype with MotionControllerWalk

### 3. Testing Considerations
- These APIs require live Hytale server for integration testing
- Unit tests can mock SDK components
- E2E tests should be created in `09-testing-framework`

---

## Appendix: SDK Package Structure

```
com.hypixel.hytale.server.npc.navigation/
├── AStarBase.md
├── AStarNode.md
├── AStarNodePool.md
├── AStarWithTarget.md
├── IWaypoint.md
├── PathFollower.md
└── ...

com.hypixel.hytale.server.npc.movement.controllers/
├── MotionController.md
├── MotionControllerWalk.md
├── MotionControllerFly.md
├── MotionControllerDive.md
└── ...

com.hypixel.hytale.builtin.hytalegenerator.biome/
├── BiomeType.md
├── SimpleBiomeType.md
└── ...

com.hypixel.hytale.builtin.weather/
├── WeatherPlugin.md
├── components/
│   └── WeatherTracker.md
├── resources/
│   └── WeatherResource.md
└── systems/
    └── WeatherSystem.md

com.hypixel.hytale.server.core.universe.world.worldmap/
├── IWorldMap.md
├── WorldMapManager.md
├── WorldMapSettings.md
└── markers/
```

---

## Document History
- 2025-01-14: Initial research complete - documented 4 HIGH RISK API categories
