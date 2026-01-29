# Phase 3 Accessor Implementation - Remaining Work

## Status: 🟡 PARTIAL PROGRESS

**Date**: 2026-01-29
**Progress**: Old SDK imports removed from 25 files, backups created, migration headers added
**Blocking**: 19 files require manual method-by-method implementation with UnsupportedOperationException

---

## Completed Work

### ✅ Converters (4 files)
All converter files successfully updated with UnsupportedOperationException:
- `PlayerConverter.java` - ✅ Complete
- `LocationConverter.java` - ✅ Complete
- `ItemDataConverter.java` - ✅ Complete
- `EntityDataConverter.java` - ✅ Complete

### ✅ Player Accessor (1 file)
- `HytalePlayerAccessor.java` - ✅ Complete with detailed exception messages

### ✅ PlayerRefCache Utilities (2 files)
- `hytaleadapter/util/PlayerRefCache.java` - ✅ Complete
- `hytale/util/PlayerRefCache.java` - ✅ Complete

### ✅ SDK Import Cleanup (25 files)
- All `com.hytale.api.*` imports removed
- Backups created in `docs/migration-001/accessor-backups-20260129-175937/`
- MIGRATION-001 headers added to accessor files

---

## Remaining Work (19 files)

These files have complex method implementations that reference the deleted SDK. Each file requires:
1. Manual review of all method signatures
2. Implementation of UnsupportedOperationException for each method
3. Detailed error messages explaining SDK requirements

### 🔴 CRITICAL Priority Accessors (6 files)

#### `HytaleEventAccessor.java`
- **Methods**: `register()`, `unregister()`, internal event dispatch logic
- **Complexity**: HIGH - Event system with listeners and registration
- **SDK Requirement**: EventRegistry, Event types from com.hypixel.hytale.*

#### `HytaleSchedulerAccessor.java`
- **Methods**: `runTask()`, `runTaskAsync()`, `runTaskLater()`, `cancelTask()`
- **Complexity**: MEDIUM - Task scheduling and cancellation
- **SDK Requirement**: Scheduler API from com.hypixel.hytale.*

#### `HytaleWorldAccessor.java`
- **Methods**: World manipulation, block access, entity queries
- **Complexity**: HIGH - Complex world interaction APIs
- **SDK Requirement**: World, Block, EntityStore from com.hypixel.hytale.*

#### `HytaleUIAccessor.java`
- **Methods**: `registerUI()`, `openUI()`, `closeUI()`, `addHud()`, `removeHud()`
- **Complexity**: HIGH - UI/HUD management with HyUI integration
- **SDK Requirement**: HyUI classes, PlayerRef

#### `HytaleItemAccessor.java`
- **Methods**: Item creation, modification, registry access
- **Complexity**: MEDIUM - Item stack manipulation
- **SDK Requirement**: ItemStack, Registry from com.hypixel.hytale.*

#### `HytaleNPCEntityAccessor.java`
- **Methods**: Entity spawning, querying, pathfinding, metadata
- **Complexity**: HIGH - Complex entity management
- **SDK Requirement**: EntityRef, Entity components, ECS patterns
- **Current Status**: Partially fixed, still has compilation errors

### 🟠 HIGH Priority Support Files (5 files)

#### `HytaleAdapterProvider.java`
- **Role**: Factory for creating accessor instances
- **Issue**: References old SDK Server type
- **Fix Required**: Replace Server parameter with JavaPlugin or Object

#### `HytalePlatform.java`
- **Role**: Platform abstraction layer
- **Issue**: Server type references
- **Fix Required**: Update constructor and accessor factory calls

#### `HytalePluginBridge.java`
- **Role**: Bridge between JavaPlugin and ArgonathPlugin
- **Issue**: Accessor instantiation with old types
- **Fix Required**: Update accessor constructor calls

#### `HytalePluginContextImpl.java`
- **Role**: Context implementation for plugin lifecycle
- **Issue**: Accessor management
- **Fix Required**: Update accessor initialization

#### `HytaleAdapterEventListener.java`
- **Role**: Event listener for Hytale events
- **Issue**: Event type references from old SDK
- **Fix Required**: Comment out event handling or use Object types

### 🟡 MEDIUM Priority UI Adapters (5 files)

#### `ActionBarAdapter.java`
- **Issue**: PlayerRef and CustomUIHud references
- **Fix Required**: Replace with Object, add UnsupportedOperationException

#### `CombatFramesAdapter.java`
- **Issue**: HyUI integration with old SDK
- **Fix Required**: Replace HyUI types with Object

#### `DialoguePageAdapter.java`
- **Issue**: Page creation logic
- **Fix Required**: Stub page creation methods

#### `QuestBookPageAdapter.java`
- **Issue**: Quest book UI integration
- **Fix Required**: Stub UI methods

#### `VendorPageAdapter.java`
- **Issue**: Vendor UI logic
- **Fix Required**: Stub vendor methods

### 🟢 LOW Priority Executor (1 file)

#### `HytaleWorldExecutor.java`
- **Role**: Thread-safe world execution context
- **Issue**: World type references
- **Fix Required**: Replace World with Object, add exception throws

---

## Implementation Strategy

### Recommended Approach per File:

1. **Read the interface contract** from the accessor interface in `02-framework-accessor`
2. **For each method**:
   ```java
   @Override
   public ReturnType methodName(ParamType param) {
       throw new UnsupportedOperationException(
           "ClassName.methodName() not yet implemented: Requires official Hytale SDK. " +
           "Expected pattern: [describe SDK usage]. " +
           "See docs/migration-001/PHASE-3-IMPLEMENTATION-STATUS.md for details."
       );
   }
   ```
3. **Remove all method body code** that references deleted SDK types
4. **Test compilation** after each file
5. **Commit incrementally** for each file or small group

### Automated Script Approach (Not Recommended):
The complexity of method signatures, generics, and Java syntax makes automated replacement error-prone. Manual review ensures:
- Correct exception messages
- Proper method signature preservation
- No syntax errors from comment-based type replacements

---

## Estimated Effort

- **Per File**: 15-30 minutes (depending on complexity)
- **Total for 19 files**: 5-10 hours of focused work
- **Recommended**: 2-3 work sessions with incremental commits

---

## Next Steps

1. **Choose implementation order**: Start with CRITICAL files (EventAccessor, SchedulerAccessor)
2. **Implement one file at a time**: Full method-by-method review
3. **Test after each file**: `mvn clean compile`
4. **Commit incrementally**: Git commits after each successful file
5. **Update this document**: Mark files as complete

---

## References

- **Migration Spec**: `00-Argonath-Specifications/MIGRATION-001-sdk-ecs-alignment.md`
- **Accessor Interfaces**: `02-framework-accessor/src/main/java/com/argonathsystems/framework/accessorapi/`
- **Backups**: `docs/migration-001/accessor-backups-20260129-175937/`
- **Phase 3 Original Status**: `docs/migration-001/PHASE-3-IMPLEMENTATION-STATUS.md`

---

**Last Updated**: 2026-01-29 18:05
**Status**: Ready for manual implementation
**Blocker**: Official Hytale SDK (com.hypixel.hytale.*) not available in development environment
