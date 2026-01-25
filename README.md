# Hytale Adapter

> A LordOfTheTales adapter

## ✅ Architecture Role

This is a **Platform Adapter** (sa-*).

**This is the ONLY module that can import Hytale classes.**

All platform-specific code is isolated here to protect business logic from API changes.

## Responsibilities

1. **Implement Accessor Interfaces** - Provide concrete implementations of all `accessor-api` interfaces
2. **Type Conversion** - Convert between Hytale types and platform-agnostic DTOs
3. **Event Translation** - Translate Hytale events to accessor events
4. **Lifecycle Management** - Handle plugin lifecycle with Hytale server

## Maven Coordinates

```xml
<dependency>
    <groupId>com.argonathsystems.adapter</groupId>
    <artifactId>hytale-adapter</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

## Quick Start

```bash
# Build
just build

# Run tests  
just test

# Deploy to Hytale mods folder
just deploy
```

## Package Structure

```
com/argonathsystems/adapter/hytaleadapter/
├── HytaleAdapterPlugin.java      # Main plugin entry point
├── HytaleAdapterProvider.java    # AccessorProvider implementation
├── accessor/                     # Accessor implementations
│   ├── HytalePlayerAccessor.java
│   ├── HytaleItemAccessor.java
│   ├── HytaleWorldAccessor.java
│   └── ...
├── converter/                    # DTO <-> Hytale converters
│   ├── LocationConverter.java
│   ├── ItemDataConverter.java
│   └── ...
└── event/                        # Event translation
    └── HytaleEventAdapter.java
```

## Implementation Example

```java
// ✅ ALLOWED - Hytale imports in adapter module
import com.hytale.api.entity.Player;
import com.hytale.api.Server;

// Implement the accessor interface
public class HytalePlayerAccessor implements PlayerAccessor {
    
    private final Server server;
    
    public HytalePlayerAccessor(Server server) {
        this.server = server;
    }
    
    @Override
    public Optional<PlayerData> getPlayer(UUID playerId) {
        // Convert Hytale Player to platform-agnostic DTO
        Player hytalePlayer = server.getPlayer(playerId);
        if (hytalePlayer == null) {
            return Optional.empty();
        }
        return Optional.of(PlayerConverter.toDto(hytalePlayer));
    }
}
```

## API Volatility Notice

> ⚠️ **Hytale is in Alpha/Pre-release**
>
> The Hytale API will change significantly before 1.0. By isolating all Hytale
> calls to this module:
> - Breaking changes only affect this adapter
> - Business logic in framework/mods remains stable
> - Updates can be done quickly in one place

## License

MIT