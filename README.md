# Hytale Adapter

> **Platform-specific implementation layer for Hytale**

[![GitHub](https://img.shields.io/badge/GitHub-Argonath--Systems-181717?logo=github)](https://github.com/Argonath-Systems/02-adapter-hytale)
[![Hytale](https://img.shields.io/badge/Hytale-Alpha-00A8E8?logo=data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAA4AAAAOCAYAAAAfSC3RAAAABHNCSVQICAgIfAhkiAAAAAlwSFlzAAAAdgAAAHYBTnsmCAAAABl0RVh0U29mdHdhcmUAd3d3Lmlua3NjYXBlLm9yZ5vuPBoAAADGSURBVCiRY/hPAMOQa2T4//8/g/+LS0yoJv8HI7z//39GBpgEAwPDPwYGhv+MjIz/GRgY/jMyMjAwMjL+Z2Rk/M/AwPCfkZHxPyMj438GBob/jIyM/xkYGP4zMjL+Z2Bg+M/IyPifgYHhPyMj439GRsb/DAwM/xkZGf8zMDD8Z2Rk/M/AwPCfkZHxPwMDw39GRsb/DAwM/xkZGf8zMDD8Z2Rk/M/AwPCfkZHxPwMDw39GRsb/DAwM/xkZGf8zMDD8Z2T8zwAAg4wX1bNiPWUAAAAASUVORK5CYII=)](https://hytalemodding.dev/)
[![Java](https://img.shields.io/badge/Java-25-orange?logo=openjdk)](https://openjdk.org/)
[![License](https://img.shields.io/badge/License-MIT-green.svg)](../LICENSE)
[![Status](https://img.shields.io/badge/Status-Alpha-yellow)](https://github.com/Argonath-Systems/02-adapter-hytale)

---

## ⚠️ Architecture Role

This is a **Platform Adapter** module.

**🚨 THIS IS THE ONLY MODULE THAT CAN IMPORT HYTALE CLASSES 🚨**

All platform-specific code is isolated here to protect business logic from API changes during Hytale's Alpha development phase.

## 🎯 Responsibilities

### 1. 🔌 Implement Accessor Interfaces
Provide concrete implementations of all `accessor-api` interfaces using Hytale's native API.

### 2. 🔄 Type Conversion
Convert between Hytale types and platform-agnostic DTOs:
- `com.hypixel.hytale.entity.Player` ↔ `PlayerData` (DTO)
- `com.hypixel.hytale.item.ItemStack` ↔ `ItemData` (DTO)
- `com.hypixel.hytale.world.Location` ↔ `LocationData` (DTO)

### 3. ⚡ Event Translation
Translate Hytale events to accessor events:
- Subscribe to Hytale's event bus
- Convert event data to platform-agnostic format
- Dispatch to accessor event listeners

### 4. 🔁 Lifecycle Management
Handle plugin lifecycle with Hytale server:
- Server startup/shutdown
- Plugin enable/disable
- Resource loading/unloading

## 📦 Maven Coordinates

```xml
<dependency>
    <groupId>com.argonathsystems.adapter</groupId>
    <artifactId>hytale-adapter</artifactId>
    <version>1.0.0-SNAPSHOT</version>
    <scope>runtime</scope> <!-- Runtime only - not compile dependency -->
</dependency>
```

> **Note**: Mods should depend on `accessor-api` at compile time, not this adapter. The adapter is loaded at runtime by the server.

## 🚀 Quick Start

### Building

```bash
# Build the adapter
mvn clean package

# Or use justfile
just build
```

### Testing

```bash
# Run unit tests
mvn test

# Or use justfile
just test
```

### Deploying

```bash
# Deploy to Hytale mods folder
mvn install
just deploy

# Manual deployment
cp target/hytale-adapter-1.0.0-SNAPSHOT.jar ~/Hytale/UserData/Mods/
```

## 📁 Package Structure

```
com/argonathsystems/adapter/hytaleadapter/
├── HytaleAdapterPlugin.java      # Main plugin entry point (extends JavaPlugin)
├── HytaleAdapterProvider.java    # AccessorProvider implementation
├── accessor/                     # Accessor implementations
│   ├── HytalePlayerAccessor.java
│   ├── HytaleEntityAccessor.java
│   ├── HytaleItemAccessor.java
│   ├── HytaleWorldAccessor.java
│   ├── HytaleInventoryAccessor.java
│   ├── HytaleEventAccessor.java
│   └── HytaleCommandAccessor.java
├── converter/                    # DTO ↔ Hytale converters
│   ├── LocationConverter.java
│   ├── ItemDataConverter.java
│   ├── PlayerDataConverter.java
│   └── EntityDataConverter.java
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

## ⚠️ API Volatility Notice

> **🚧 Hytale is in Alpha/Pre-release**
>
> The Hytale API will change significantly before 1.0. By isolating all Hytale
> calls to this module:
> - ✅ Breaking changes only affect this adapter
> - ✅ Business logic in frameworks/mods remains stable
> - ✅ Updates can be done quickly in one place
> - ✅ Multiple platform support becomes possible (future)

## 🔗 Dependencies

### Compile Time
- [**01-platform-core**](https://github.com/Argonath-Systems/01-platform-core) - Parent POM
- [**02-framework-accessor**](https://github.com/Argonath-Systems/02-framework-accessor) - Accessor API to implement
- **Hytale Server API** (provided) - Game engine API

### Runtime
- Hytale Server (provided by game)

## 📚 Documentation

- 🌐 [**Documentation Website**](https://argonath-systems.github.io/00-Argonath-Wiki)
- 📖 [**Adapter Guide**](https://argonath-systems.github.io/00-Argonath-Wiki/docs/architecture/adapters.html)
- 🔧 [**Hytale Modding Docs**](https://hytalemodding.dev/en/docs)
- 🎮 [**Hytale Plugin Template**](https://github.com/HytaleModding/plugin-template)

## 🤝 Contributing

See [CONTRIBUTING.md](../CONTRIBUTING.md) for development guidelines.

**Special Note for Adapter Contributions**:
- Keep Hytale imports isolated to this module only
- Add comprehensive error handling (Hytale API can be unstable)
- Document any API workarounds or known issues
- Add unit tests with mocked Hytale objects

## 💬 Community

- 💬 [**Discord**](https://discord.gg/RK3MtpyH) - Chat and support
- 🐛 [**Issues**](https://github.com/orgs/Argonath-Systems/issues) - Bug reports
- 📖 [**Discussions**](https://github.com/orgs/Argonath-Systems/discussions) - Q&A

## 📄 License

MIT License - Copyright © 2025 Argonath Systems. See [LICENSE](../LICENSE) for details.

---

<div align="center">

Part of the [**Argonath Systems**](https://github.com/orgs/Argonath-Systems/) ecosystem

[Documentation](https://argonath-systems.github.io/00-Argonath-Wiki) • [Discord](https://discord.gg/RK3MtpyH) • [GitHub](https://github.com/orgs/Argonath-Systems/)

</div>