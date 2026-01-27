# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added
- **CRITICAL**: Implemented `HytaleInventoryAccessor.countItem()` - counts items by ID across inventory
- **CRITICAL**: Implemented `HytaleInventoryAccessor.getMainHandItem()` - retrieves main hand item
- **CRITICAL**: Implemented `HytaleInventoryAccessor.getEmptySlots()` - counts empty inventory slots
- **CRITICAL**: Implemented `HytaleConfigAccessor` with Jackson JSON support
  - Config loading from JSON files
  - Config saving with pretty-print formatting
  - Automatic config directory creation
- **CRITICAL**: Implemented `BsonConverter` utility class
  - BSON Document to Map conversion with recursive nested document support
  - Map to BSON Document conversion
  - Proper type handling and error messages
- **CRITICAL**: Implemented `HytaleEntityAccessor.spawnEntity()` with world detection
  - Spawns entities at specified locations
  - World auto-detection from location or fallback to first world
  - Proper error handling and logging
- **MEDIUM**: Quest format converter objective configuration extraction from node data
- **MEDIUM**: Quest format converter reward conversion with multiple reward types (item, currency, xp)
- **MEDIUM**: HyQuest API service metadata-based filtering (min/max level, category filtering support)
- **NEW**: Nitrado WebServer adapter implementation
  - `NitradoWebServerAdapter` implements `WebServerAccessor` interface
  - Path parameter extraction from servlet request attributes (supports standard servlet patterns)
- **NEW**: HyQuest API Controller complete implementation
  - Quest reload functionality integrating with Quest Framework
  - Quest export using `QuestFormatConverter` (graceful degradation when converter unavailable)
- **NEW**: HyPrefab API Controller service integration
  - Service availability checks for all endpoints
  - Constructor with optional `PrefabService` injection
  - Proper 503 Service Unavailable responses when service not wired
  - Bridges framework abstractions to Nitrado WebServer Plugin
  - Servlet wrappers for route handlers
  - Jakarta Servlet to framework request/response conversion
- **NEW**: HyQuest API controller with RESTful endpoints
  - List, get, create, update, delete quests
  - Quest reload and export operations
  - Permission-based authorization (argonath.hyquest.web.*)
- **NEW**: HyPrefab API controller with RESTful endpoints
  - List, get, create, update, delete prefabs
  - Prefab spawning operations
  - Permission-based authorization (argonath.hyprefab.web.*)

### Changed
- `HytaleInventoryAccessor` now fully implements all InventoryAccessor methods
- `HytaleConfigAccessor` uses Jackson ObjectMapper for JSON serialization
- `BsonConverter` validates input types and throws clear exceptions
- Quest objective references now extract `required` and `targetCount` from node data
- Quest rewards now properly convert from HyQuest graph nodes with type-based fields
- Removed Javalin dependency in favor of Nitrado WebServer Plugin integration
- Removed embedded HTTP server approach - now uses shared server

### Fixed
- Quest format converter no longer returns empty objectives/rewards from graph nodes
- HTTP API integration now follows platform-agnostic architecture (Zero Hytale Imports)

### Fixed
- **CRITICAL**: Inventory item counting now functional for quest objectives
- **CRITICAL**: Config persistence now works for mod settings
- **CRITICAL**: Entity spawning now operational for dynamic content
- BSON data conversion no longer returns empty maps

### Security

## [1.0.0] - 2026-01-25

### Added
- Initial release
- Core functionality implemented
- Documentation and examples
- Build system configured

[Unreleased]: https://github.com/Argonath-Systems/02-adapter-hytale/compare/v1.0.0...HEAD
[1.0.0]: https://github.com/Argonath-Systems/02-adapter-hytale/releases/tag/v1.0.0
