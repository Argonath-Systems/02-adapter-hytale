
## Phase 7: Hytale Adapter (SA-01)

> **Spec:** [SA-01-hytale-adapter.md](specs/standalone-adapter/SA-01-hytale-adapter.md)  
> **Maven Artifact:** `com.argonathsystems.adapter:hytale-adapter`  
> **Type:** Platform Adapter (The ONLY module allowing Hytale imports)

### 7.1 Setup & Dependencies

| Task | Status | Notes |
|------|--------|-------|
| Project Scaffolding | ‚úÖ | Created via generator |
| **Hytale SDK Stub** | ‚úÖ | Local module `projects/hytale-sdk` mimicking official API |
| Pom Configuration | ‚úÖ | Imports `HytaleServer-parent` (stub) |
| Java 25 Compatibility | ‚ö†Ô∏è | `maven-shade-plugin` disabled (incompatible with class v69) |

### 7.2 Core Adapter Logic

| Component | Class | Status | Notes |
|-----------|-------|--------|-------|
| Plugin Entry Point | `HytaleAdapterPlugin` | ‚úÖ | Lifecycle management, Event Bus registration |
| Provider Implementation | `HytaleAdapterAdapterProvider` | ‚úÖ | Lazy initialization of accessors |
| Event Listener Bridge | `HytaleAdapterEventListener` | ‚úÖ | Relays `com.hytale` events to `EventAccessor` |

### 7.3 Accessor Implementations

| Interface | Implementation | Status | Notes |
|-----------|----------------|--------|-------|
| `PlayerAccessor` | `HytalePlayerAccessor` | ‚úÖ | |
| `EntityAccessor` | `HytaleEntityAccessor` | ‚úÖ | |
| `EventAccessor` | `HytaleEventAccessor` | ‚úÖ | Implemented `emit()` & `register()` |
| `ItemAccessor` | `HytaleItemAccessor` | ‚úÖ | |
| `WorldAccessor` | `HytaleWorldAccessor` | ‚úÖ | |
| *(Remaining 8)* | *(Various)* | ‚úÖ | Skeletal implementations created |

### 7.4 Converters (Hytale <-> Framework)

| Converter | Status | Notes |
|-----------|--------|-------|
| `PlayerConverter` | ‚úÖ | Implemented `toDTO` with name/health/maxHealth |
| `ItemDataConverter` | Ì¥≤ | Skeletal |
| `EntityDataConverter` | Ì¥≤ | Skeletal |
| `LocationConverter` | Ì¥≤ | Skeletal |

---

## Phase 8: Integration Testing

> **Goal:** Verify that the Adapter correctly bridges the Framework and the (Stubbed) Hytale API.

### 8.1 Testing Infrastructure

| Task | Status | Notes |
|------|--------|-------|
| Mocking Framework | ‚úÖ | JUnit 5 + Mockito 5 configured |
| Test Coverage | Ì¥µ | Initial flow tests created |

### 8.2 Scenario Tests

| Scenario | Status | Notes |
|----------|--------|-------|
| **Event Bridge Flow** | ‚úÖ | `HytaleAdapterIntegrationTest` simulates PlayerJoin -> Framework Event |
| Accessor Delegation | Ì¥≤ | Verify accessor methods call Hytale API |
| Converter Accuracy | Ì¥≤ | Verify DTO mapping correctness |

