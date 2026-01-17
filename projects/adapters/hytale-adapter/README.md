# SA-01: Hytale Adapter

> Hytale-specific implementation of all Accessor API interfaces. **ONLY module with Hytale imports.**

---

## 📋 Overview

| Attribute | Value |
|-----------|-------|
| **Module ID** | `sa-hytale-adapter` |
| **Maven Artifact** | `com.lordofthetales.adapter:hytale-adapter` |
| **License** | MIT |
| **Dependencies** | SF-01 Accessor API, Hytale Server API |
| **Java Version** | 25+ |

### Purpose

- Implement ALL interfaces from SF-01 Accessor API
- Bridge abstract DTOs to Hytale engine types
- Handle Hytale's ECS architecture and thread model
- Isolate ALL Hytale API calls to this single module

---

## 🏗️ C4 Architecture

### C1: System Context

```
┌─────────────────────────────────────────────────────────────────────────┐
│                        FRAMEWORK LAYER                                  │
├─────────────────────────────────────────────────────────────────────────┤
│  ┌─────────────────────────────────────────────────────────────────┐   │
│  │              SF-01: ACCESSOR API (Interfaces)                   │   │
│  │                   ** ZERO HYTALE IMPORTS **                     │   │
│  │                                                                 │   │
│  │  PlayerAccessor │ EntityAccessor │ WorldAccessor │ etc.        │   │
│  └──────────────────────────────┬──────────────────────────────────┘   │
│                                 │                                      │
│                                 │ implements                           │
│                                 ▼                                      │
│  ╔═════════════════════════════════════════════════════════════════╗   │
│  ║            SA-01: HYTALE ADAPTER (This Module)                  ║   │
│  ║              ** ONLY HYTALE IMPORTS HERE **                     ║   │
│  ║                                                                 ║   │
│  ║  HytalePlayerAccessor │ HytaleEntityAccessor │ etc.            ║   │
│  ║                                                                 ║   │
│  ║  UTILITIES:                                                     ║   │
│  ║  • PlayerRefCache - Track online players (Hytale has no lookup)║   │
│  ║  • WorldThreadExecutor - ECS thread safety                     ║   │
│  ║  • ComponentHelper - Safe component access                     ║   │
│  ║  • Converters - DTO ↔ Hytale type conversion                   ║   │
│  ╚═════════════════════════════════════════════════════════════════╝   │
│                                 │                                      │
│                                 │ calls                                │
│                                 ▼                                      │
│  ┌─────────────────────────────────────────────────────────────────┐   │
│  │                   HYTALE GAME ENGINE API                        │   │
│  │                      (Alpha - Will Change)                      │   │
│  │                                                                 │   │
│  │  import com.hypixel.hytale.server.core.*                       │   │
│  │  import com.hypixel.hytale.server.core.universe.*              │   │
│  │  import com.hypixel.hytale.server.core.entity.*                │   │
│  │  import com.hypixel.hytale.server.core.component.*             │   │
│  └─────────────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────────────┘
```

### C2: Container View

```
┌──────────────────────────────────────────────────────────────────────────────┐
│                         SA-01: HYTALE ADAPTER                                │
├──────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│  ┌────────────────────────────────────────────────────────────────────────┐ │
│  │                     HytaleAdapterProvider                              │ │
│  │                 (Implements AccessorProvider)                          │ │
│  │                                                                        │ │
│  │  • Initializes all accessor implementations                           │ │
│  │  • Manages PlayerRefCache lifecycle                                   │ │
│  │  • Registers for player lifecycle events                              │ │
│  │  • Creates shared WorldThreadExecutor                                 │ │
│  └────────────────────────────────────────────────────────────────────────┘ │
│                                                                              │
│  ┌────────────────────────────────────────────────────────────────────────┐ │
│  │                      UTILITY CLASSES (Critical)                        │ │
│  │                                                                        │ │
│  │  ┌──────────────────┐  ┌──────────────────┐  ┌──────────────────┐    │ │
│  │  │  PlayerRefCache  │  │WorldThreadExecutor│  │ ComponentHelper  │    │ │
│  │  │                  │  │                  │  │                  │    │ │
│  │  │ UUID→PlayerRef   │  │ world.execute()  │  │ Safe null checks │    │ │
│  │  │ mapping          │  │ wrapper          │  │ for components   │    │ │
│  │  └──────────────────┘  └──────────────────┘  └──────────────────┘    │ │
│  │                                                                        │ │
│  │  ┌──────────────────┐  ┌──────────────────┐                           │ │
│  │  │  BsonConverter   │  │  MessageBuilder  │                           │ │
│  │  │                  │  │                  │                           │ │
│  │  │ BsonDocument ↔   │  │ MessageData →    │                           │ │
│  │  │ Map<String,Obj>  │  │ Message.raw()    │                           │ │
│  │  └──────────────────┘  └──────────────────┘                           │ │
│  └────────────────────────────────────────────────────────────────────────┘ │
│                                                                              │
│  ┌────────────────────────────────────────────────────────────────────────┐ │
│  │                    ACCESSOR IMPLEMENTATIONS                            │ │
│  │                                                                        │ │
│  │  ┌────────────────┐  ┌────────────────┐  ┌────────────────┐           │ │
│  │  │HytalePlayer    │  │HytaleEntity    │  │ HytaleItem     │           │ │
│  │  │Accessor        │  │Accessor        │  │ Accessor       │           │ │
│  │  └────────────────┘  └────────────────┘  └────────────────┘           │ │
│  │                                                                        │ │
│  │  ┌────────────────┐  ┌────────────────┐  ┌────────────────┐           │ │
│  │  │HytaleInventory │  │HytaleWorld     │  │ HytaleEvent    │           │ │
│  │  │Accessor        │  │Accessor        │  │ Accessor       │           │ │
│  │  └────────────────┘  └────────────────┘  └────────────────┘           │ │
│  │                                                                        │ │
│  │  ┌────────────────┐  ┌────────────────┐  ┌────────────────┐           │ │
│  │  │HytaleScheduler │  │HytaleStorage   │  │ HytaleUI       │           │ │
│  │  │Accessor        │  │Accessor        │  │ Accessor       │           │ │
│  │  └────────────────┘  └────────────────┘  └────────────────┘           │ │
│  │                                                                        │ │
│  │  ┌────────────────┐  ┌────────────────┐  ┌────────────────┐           │ │
│  │  │HytaleSound     │  │HytaleNotify    │  │ HytaleHologram │           │ │
│  │  │Accessor        │  │Accessor        │  │ Accessor       │           │ │
│  │  └────────────────┘  └────────────────┘  └────────────────┘           │ │
│  └────────────────────────────────────────────────────────────────────────┘ │
│                                                                              │
│  ┌────────────────────────────────────────────────────────────────────────┐ │
│  │                         CONVERTERS                                     │ │
│  │                                                                        │ │
│  │  LocationConverter:  LocationData ↔ TransformComponent / Vec3         │ │
│  │  ItemDataConverter:  ItemData ↔ ItemStack + BsonDocument              │ │
│  │  PlayerDataConverter: PlayerData ↔ Player component                   │ │
│  │  EntityDataConverter: EntityData ↔ Entity components                  │ │
│  │  MessageConverter:   MessageData ↔ Message.raw().color()              │ │
│  └────────────────────────────────────────────────────────────────────────┘ │
│                                                                              │
└──────────────────────────────────────────────────────────────────────────────┘
```

### C3: Key Component - HytalePlayerAccessor

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                         HytalePlayerAccessor                                    │
│                     (implements PlayerAccessor)                                 │
├─────────────────────────────────────────────────────────────────────────────────┤
│                                                                                 │
│  DEPENDENCIES:                                                                  │
│  ┌──────────────────┐  ┌──────────────────┐  ┌──────────────────┐             │
│  │  PlayerRefCache  │  │WorldThreadExecutor│  │ PlayerFlagStorage│             │
│  └────────┬─────────┘  └────────┬─────────┘  └────────┬─────────┘             │
│           │                     │                     │                        │
│           ▼                     ▼                     ▼                        │
│                                                                                 │
│  METHODS (All use worldExecutor.executeSync internally):                       │
│                                                                                 │
│  ┌─────────────────────────────────────────────────────────────────────────┐   │
│  │  getPlayer(UUID playerId): Optional<PlayerData>                         │   │
│  │  ────────────────────────────────────────────────────────────────────── │   │
│  │  1. Get PlayerRef from cache (may be null if offline)                   │   │
│  │  2. worldExecutor.executeSync(() -> {                                   │   │
│  │       Ref<EntityStore> entityRef = playerRef.getReference();            │   │
│  │       Store<EntityStore> store = entityRef.getStore();                  │   │
│  │       Player player = store.getComponent(entityRef, Player.type);       │   │
│  │       if (player == null) return Optional.empty();                      │   │
│  │       TransformComponent transform = store.getComponent(...);           │   │
│  │       return Optional.of(convertToPlayerData(player, transform));       │   │
│  │     });                                                                 │   │
│  └─────────────────────────────────────────────────────────────────────────┘   │
│                                                                                 │
│  ┌─────────────────────────────────────────────────────────────────────────┐   │
│  │  teleport(UUID playerId, LocationData destination)                      │   │
│  │  ────────────────────────────────────────────────────────────────────── │   │
│  │  worldExecutor.executeSync(() -> {                                      │   │
│  │    PlayerRef ref = playerCache.get(playerId);                           │   │
│  │    if (ref == null) return;  // Player offline                          │   │
│  │    Ref<EntityStore> entityRef = ref.getReference();                     │   │
│  │    if (!entityRef.isValid()) return;  // Disconnected mid-op            │   │
│  │    TransformComponent transform = getComponent(entityRef, Transform);   │   │
│  │    transform.setPosition(destination.x(), destination.y(), destination.z());│
│  │  });                                                                    │   │
│  └─────────────────────────────────────────────────────────────────────────┘   │
│                                                                                 │
│  ┌─────────────────────────────────────────────────────────────────────────┐   │
│  │  hasPermission(UUID playerId, String permission): boolean               │   │
│  │  ────────────────────────────────────────────────────────────────────── │   │
│  │  // Uses Hytale's PermissionsModule singleton                           │   │
│  │  PermissionsModule perms = PermissionsModule.get();                     │   │
│  │  return perms.hasPermission(playerId, permission);                      │   │
│  └─────────────────────────────────────────────────────────────────────────┘   │
│                                                                                 │
└─────────────────────────────────────────────────────────────────────────────────┘
```

---

## ⚠️ Hytale API Challenges & Solutions

### Challenge 1: ECS Architecture

| Problem | Hytale uses ECS, not OOP. `Player` is a component, not an entity. |
|---------|-------------------------------------------------------------------|
| **Impact** | Cannot call `player.teleport()`. Must access components via Store. |
| **Solution** | Use `store.getComponent(entityRef, ComponentType)` pattern. |

```java
// ❌ Expected (doesn't exist)
Player player = server.getPlayer(uuid);
player.teleport(location);

// ✅ Actual Hytale pattern
Ref<EntityStore> entityRef = playerRef.getReference();
Store<EntityStore> store = entityRef.getStore();
world.execute(() -> {
    TransformComponent transform = store.getComponent(entityRef, TransformComponent.getComponentType());
    transform.setPosition(x, y, z);
});
```

### Challenge 2: No Player Lookup

| Problem | Hytale has NO `server.getPlayer(UUID)` method. |
|---------|------------------------------------------------|
| **Impact** | Cannot find players by UUID directly. |
| **Solution** | Maintain `PlayerRefCache` populated via events. |

```java
public class PlayerRefCache {
    private final Map<UUID, PlayerRef> cache = new ConcurrentHashMap<>();
    
    // Populated when PlayerReadyEvent fires
    public void put(UUID id, PlayerRef ref) { cache.put(id, ref); }
    
    // Cleaned when player disconnects
    public void remove(UUID id) { cache.remove(id); }
    
    // Used by all accessors
    public PlayerRef get(UUID id) { return cache.get(id); }
}
```

### Challenge 3: Thread Safety (CRITICAL)

| Problem | ALL entity operations MUST run on world thread. |
|---------|--------------------------------------------------|
| **Impact** | Random crashes if accessed from wrong thread. |
| **Solution** | `WorldThreadExecutor` wraps all operations. |

```java
public class WorldThreadExecutor {
    public void executeSync(Runnable action) {
        World world = getCurrentWorld();
        CompletableFuture<Void> future = new CompletableFuture<>();
        world.execute(() -> {
            try {
                action.run();
                future.complete(null);
            } catch (Exception e) {
                future.completeExceptionally(e);
            }
        });
        future.get(5000, TimeUnit.MILLISECONDS);  // Block until done
    }
}
```

### Challenge 4: Mid-Operation Disconnects

| Problem | Players can disconnect while operation is queued. |
|---------|---------------------------------------------------|
| **Impact** | NullPointerException if not handled. |
| **Solution** | Always null-check after getting from cache. |

```java
worldExecutor.executeSync(() -> {
    PlayerRef ref = playerCache.get(playerId);
    if (ref == null) return;  // Already disconnected
    
    Ref<EntityStore> entityRef = ref.getReference();
    if (!entityRef.isValid()) return;  // Disconnected mid-operation
    
    Player player = store.getComponent(entityRef, Player.getComponentType());
    if (player == null) return;  // Component removed
    
    // NOW safe to operate
});
```

### Challenge 5: Message Formatting

| Problem | Hytale uses builder pattern for messages, not simple strings. |
|---------|--------------------------------------------------------------|
| **Impact** | Cannot just call `player.sendMessage("Hello")`. |
| **Solution** | `MessageConverter` translates MessageData to Hytale format. |

```java
// Our abstraction
MessageData message = MessageData.of("Hello, ", MessageData.TextColor.GREEN)
    .append("world!", MessageData.TextColor.GOLD);

// Converted to Hytale
Message hytaleMessage = Message.raw("Hello, ").color(Color.GREEN)
    .append(Message.raw("world!").color(Color.GOLD));
```

### Challenge 6: BsonDocument for Item Metadata

| Problem | Hytale stores item metadata in BsonDocument, not Map. |
|---------|-------------------------------------------------------|
| **Impact** | Cannot easily serialize/deserialize item data. |
| **Solution** | `BsonConverter` handles bidirectional conversion. |

```java
public class BsonConverter {
    public static Map<String, Object> toMap(BsonDocument doc) {
        Map<String, Object> result = new HashMap<>();
        for (String key : doc.keySet()) {
            result.put(key, convertBsonValue(doc.get(key)));
        }
        return result;
    }
    
    public static BsonDocument fromMap(Map<String, Object> map) {
        BsonDocument doc = new BsonDocument();
        for (var entry : map.entrySet()) {
            doc.put(entry.getKey(), convertToBsonValue(entry.getValue()));
        }
        return doc;
    }
}
```

---

## 📦 Package Structure

```
com.lordofthetales.adapter.hytale/
├── HytaleAdapterProvider.java      # Main provider implementation
│
├── util/
│   ├── PlayerRefCache.java         # UUID→PlayerRef cache
│   ├── WorldThreadExecutor.java    # Thread-safe execution
│   ├── ComponentHelper.java        # Safe component access
│   └── BsonConverter.java          # BsonDocument ↔ Map conversion
│
├── player/
│   ├── HytalePlayerAccessor.java   # PlayerAccessor implementation
│   └── PlayerFlagStorage.java      # Player flag persistence
│
├── entity/
│   └── HytaleEntityAccessor.java   # EntityAccessor implementation
│
├── item/
│   └── HytaleItemAccessor.java     # ItemAccessor implementation
│
├── inventory/
│   └── HytaleInventoryAccessor.java # InventoryAccessor implementation
│
├── world/
│   └── HytaleWorldAccessor.java    # WorldAccessor implementation
│
├── event/
│   ├── HytaleEventAccessor.java    # EventAccessor implementation
│   └── EventBridge.java            # Hytale→Accessor event translation
│
├── scheduler/
│   └── HytaleSchedulerAccessor.java # SchedulerAccessor implementation
│
├── storage/
│   └── HytaleStorageAccessor.java  # StorageAccessor implementation
│
├── ui/
│   └── HytaleUIAccessor.java       # UIAccessor implementation
│
├── sound/
│   └── HytaleSoundAccessor.java    # SoundAccessor implementation
│
├── notification/
│   └── HytaleNotificationAccessor.java # NotificationAccessor implementation
│
├── hologram/
│   └── HytaleHologramAccessor.java # HologramAccessor implementation
│
└── converter/
    ├── LocationConverter.java      # LocationData ↔ TransformComponent
    ├── ItemDataConverter.java      # ItemData ↔ ItemStack
    ├── PlayerDataConverter.java    # PlayerData ↔ Player component
    ├── EntityDataConverter.java    # EntityData ↔ Entity components
    └── MessageConverter.java       # MessageData ↔ Message
```

---

## 🔧 Usage

### Maven Coordinates

```xml
<dependency>
    <groupId>com.lordofthetales.adapter</groupId>
    <artifactId>hytale-adapter</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>

<!-- Also requires Hytale Server API (provided at runtime) -->
<dependency>
    <groupId>com.hypixel.hytale</groupId>
    <artifactId>HytaleServer-parent</artifactId>
    <version>1.0-SNAPSHOT</version>
    <scope>provided</scope>
</dependency>
```

### Initialization

```java
public class MyPlugin extends JavaPlugin {
    private HytaleAdapterProvider adapterProvider;
    
    @Override
    public void setup() {
        // Initialize adapter (registers event listeners)
        adapterProvider = new HytaleAdapterProvider(this);
        
        // Pass to mods that need it
        MyMod mod = new MyMod(adapterProvider);
    }
}
```

### Quick Start

```bash
just build
just test
just install
```

---

## 📋 Implementation Checklist

### Utility Classes

- [ ] `PlayerRefCache` - Thread-safe UUID→PlayerRef mapping
- [ ] `WorldThreadExecutor` - Sync/async world thread execution
- [ ] `ComponentHelper` - Safe component access with null checks
- [ ] `BsonConverter` - BsonDocument ↔ Map conversion

### Accessor Implementations

- [ ] `HytaleAdapterProvider` - Central factory
- [ ] `HytalePlayerAccessor` - Player operations
- [ ] `HytaleEntityAccessor` - Entity CRUD
- [ ] `HytaleItemAccessor` - Item definitions
- [ ] `HytaleInventoryAccessor` - Inventory management
- [ ] `HytaleWorldAccessor` - World queries
- [ ] `HytaleEventAccessor` - Event registration
- [ ] `HytaleSchedulerAccessor` - Task scheduling
- [ ] `HytaleStorageAccessor` - Data persistence
- [ ] `HytaleUIAccessor` - UI rendering
- [ ] `HytaleSoundAccessor` - Sound playback
- [ ] `HytaleNotificationAccessor` - Notifications
- [ ] `HytaleHologramAccessor` - Floating text

### Converters

- [ ] `LocationConverter` - LocationData ↔ TransformComponent
- [ ] `ItemDataConverter` - ItemData ↔ ItemStack
- [ ] `PlayerDataConverter` - PlayerData ↔ Player
- [ ] `EntityDataConverter` - EntityData ↔ Entity
- [ ] `MessageConverter` - MessageData ↔ Message

### Event Bridge

- [ ] PlayerReadyEvent → PlayerJoinEvent
- [ ] PlayerDisconnectEvent → PlayerQuitEvent
- [ ] EntityDeathEvent → EntityKillEvent
- [ ] ItemPickupEvent mapping
- [ ] BlockBreakEvent mapping
- [ ] CraftEvent mapping

---

## 🧪 Testing Strategy

Since this module requires Hytale runtime, testing is challenging:

| Test Type | Approach |
|-----------|----------|
| Unit Tests | Mock Hytale classes (limited) |
| Integration | Run on actual Hytale server |
| Converters | Unit-testable with sample data |

### Mock Testing Example

```java
@Test
void testPlayerRefCache() {
    PlayerRefCache cache = new PlayerRefCache();
    PlayerRef mockRef = mock(PlayerRef.class);
    UUID playerId = UUID.randomUUID();
    
    cache.put(playerId, mockRef);
    assertThat(cache.get(playerId)).isEqualTo(mockRef);
    
    cache.remove(playerId);
    assertThat(cache.get(playerId)).isNull();
}
```

---

## 📚 Related Documentation

- [SA-01 Spec](../../specs/standalone-adapter/SA-01-hytale-adapter.md) - Full specification
- [SF-01 Accessor API](../frameworks/accessor-api/) - Interface definitions
- [C4 Architecture](../../design/C4/) - System diagrams
