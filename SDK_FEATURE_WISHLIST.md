# Hytale SDK Feature Wishlist

> **Module**: `02-adapter-hytale`  
> **Created**: 2026-01-31  
> **Purpose**: Document SDK features required to complete blocked implementations

---

## 📋 Summary

This document tracks Hytale SDK API features that are **not currently available** but are required to fully implement the Argonath Systems accessor layer.

| Priority | Category | Features Needed | Impact |
|----------|----------|-----------------|--------|
| 🔴 Critical | Camera Control | 6 | Dialogue cinematics blocked |
| 🔴 Critical | Animation Runtime | 4 | NPC animations blocked |
| 🟡 High | Model Modification | 5 | Character customization blocked |
| 🟡 High | Visual Effects | 3 | Combat feedback blocked |
| 🟢 Medium | Hologram Native | 1 | Using workaround |
| 🟢 Medium | Permission System | 1 | Using custom implementation |

---

## 🔴 Critical: Camera Control API

### Current State
- Only `CameraShakeEffect` packet is available
- No API for camera position/rotation control
- No FOV, DoF, letterbox controls

### Required APIs

```java
// Player camera access
interface CameraController {
    // Position/Rotation
    void setPosition(Vector3d position);
    void setRotation(Vector3f rotation);
    Vector3d getPosition();
    Vector3f getRotation();
    
    // Field of View
    void setFOV(float degrees);
    float getFOV();
    void animateFOV(float targetFOV, long durationMs);
    
    // Depth of Field
    void setDepthOfField(float focalDistance, float aperture);
    void clearDepthOfField();
    
    // Letterbox
    void enableLetterbox(float aspectRatio);
    void disableLetterbox();
    
    // Control Mode
    void detachFromPlayer();  // Free camera mode
    void attachToPlayer();    // Return to player control
    void setTarget(Entity entity);  // Follow entity
}

// Access method
PlayerRef.getCameraController();
```

### Use Cases
1. **Dialogue Cinematics** (SF-NPC-044): Focus camera on NPC during dialogue
2. **Quest Cutscenes**: Automated camera movements for story beats
3. **Combat Effects**: FOV changes during abilities
4. **Photo Mode**: Free camera for screenshots

### Workaround
- Currently tracking state internally for testing
- Camera shake works via `CameraShakeEffect` packet

---

## 🔴 Critical: Animation Runtime Control

### Current State
- Animations defined in model assets
- No runtime control over animation playback
- No animation event callbacks

### Required APIs

```java
interface AnimationController {
    // Playback control
    void playAnimation(String animationId);
    void playAnimation(String animationId, boolean loop);
    void playAnimation(String animationId, float speed);
    void stopAnimation(String animationId);
    void stopAllAnimations();
    
    // Query
    boolean isAnimationPlaying(String animationId);
    List<String> getActiveAnimations();
    
    // Events
    void onAnimationComplete(String animationId, Runnable callback);
    void onAnimationEvent(String eventName, Consumer<AnimationEvent> callback);
    
    // Blending
    void crossfade(String fromAnimation, String toAnimation, float duration);
    void setLayerWeight(int layer, float weight);
}

// Access via entity component
AnimationComponent component = entity.getComponent(AnimationComponent.class);
AnimationController controller = component.getController();
```

### Use Cases
1. **NPC Emotes**: Play wave, bow, gesture animations
2. **Combat Animations**: Trigger attack/cast animations
3. **Dialogue Gestures**: Lip sync, hand movements during conversation
4. **Mount Animations**: Mounting/dismounting sequences

### Current Workaround
- Animation triggers defined in NPC role assets
- No runtime control, using predefined animation sets

---

## 🟡 High: Model Modification API

### Current State
- Model assigned at entity spawn via role asset
- No runtime model/skin swapping
- No scale modification

### Required APIs

```java
interface ModelController {
    // Model swapping
    void setModel(String modelAssetId);
    String getModel();
    
    // Skin/Texture
    void setSkin(String skinAssetId);
    String getSkin();
    void setVariant(String variantId);
    
    // Transform
    void setScale(float scale);
    void setScale(Vector3f scale);  // Non-uniform
    float getScale();
    
    // Equipment visual (not inventory)
    void setVisualEquipment(String slot, String itemAssetId);
    void clearVisualEquipment(String slot);
}

// Access via entity component
ModelComponent component = entity.getComponent(ModelComponent.class);
ModelController controller = component.getController();
```

### Use Cases
1. **Character Races**: Apply race-specific models/skins
2. **Cosmetics**: Outfit changes, skin tones
3. **Level Scaling**: Size changes for boss variants
4. **Equipment Preview**: Show equipment without inventory changes

---

## 🟡 High: Visual Effects API

### Current State
- Particles available via `ParticleUtil`
- No glow effects
- No entity outlines

### Required APIs

```java
interface VisualEffectController {
    // Glow/Outline
    void setGlowing(boolean glowing);
    void setGlowColor(Color color);
    boolean isGlowing();
    
    // Outline (separate from glow)
    void setOutline(boolean enabled);
    void setOutlineColor(Color color);
    void setOutlineWidth(float width);
    
    // Tint/Color overlay
    void setTint(Color color);
    void clearTint();
    void pulseTint(Color color, long periodMs);
    
    // Visibility
    void setInvisible(boolean invisible);
    void setTransparency(float alpha);
}

// Access via entity component
VisualEffectComponent component = entity.getComponent(VisualEffectComponent.class);
```

### Use Cases
1. **Target Highlighting**: Glow on quest targets
2. **Team Colors**: Outline for faction identification
3. **Status Effects**: Color tints for buffs/debuffs
4. **Stealth**: Transparency for rogue abilities

---

## 🟢 Medium: Native Hologram Entity

### Current State
- No hologram entity type
- Using workaround with invisible NPC + nameplate

### Required API

```java
interface HologramAPI {
    // Creation
    Hologram createHologram(World world, Vector3d position, List<String> lines);
    
    // Modification
    void setLines(Hologram hologram, List<String> lines);
    void setLine(Hologram hologram, int index, String text);
    void setPosition(Hologram hologram, Vector3d position);
    
    // Visibility
    void setVisibleTo(Hologram hologram, Collection<PlayerRef> players);
    void setVisibleToAll(Hologram hologram);
    
    // Lifecycle
    void remove(Hologram hologram);
}

// Access via World
world.getHologramAPI().createHologram(position, lines);
```

### Use Cases
1. **NPC Names**: Floating name tags with titles
2. **Quest Markers**: Floating text for objectives
3. **Region Labels**: Zone names, landmark indicators
4. **Debug Info**: Developer-visible information

---

## 🟢 Medium: Permission System

### Current State
- No native permission API
- Implementing custom permission storage

### Required API

```java
interface PermissionAPI {
    // Check
    boolean hasPermission(PlayerRef player, String permission);
    boolean hasPermission(PlayerRef player, String permission, String context);
    
    // Modify
    void grantPermission(PlayerRef player, String permission);
    void revokePermission(PlayerRef player, String permission);
    void setPermission(PlayerRef player, String permission, boolean value);
    
    // Groups
    void addToGroup(PlayerRef player, String groupName);
    void removeFromGroup(PlayerRef player, String groupName);
    Set<String> getGroups(PlayerRef player);
    
    // Persistence
    void savePermissions(PlayerRef player);
    void loadPermissions(PlayerRef player);
}

// Access via Universe
Universe.get().getPermissionAPI();
```

### Use Cases
1. **Command Access**: Restrict admin/mod commands
2. **Area Access**: Zone-based permissions
3. **Feature Toggles**: Beta features for specific players
4. **Guild Permissions**: Role-based access within guilds

---

## 📊 Impact Analysis

### Features Blocked Without These APIs

| Feature | Blocked By | Severity |
|---------|------------|----------|
| Dialogue Cinematic Mode | Camera Control | 🔴 High |
| NPC Emote System | Animation Runtime | 🔴 High |
| Character Race System | Model Modification | 🟡 Medium |
| Combat Visual Feedback | Visual Effects | 🟡 Medium |
| Quest Target Highlighting | Visual Effects | 🟡 Medium |
| Equipment Preview UI | Model Modification | 🟢 Low |
| Floating Text Labels | Hologram Native | 🟢 Low (workaround exists) |

### Workarounds in Place

| Missing API | Workaround | Quality |
|-------------|------------|---------|
| Camera Control | Internal state tracking only | ❌ No visual effect |
| Animation Runtime | Predefined animations in role assets | ⚠️ Limited flexibility |
| Model Modification | Define multiple NPC roles per variant | ⚠️ Asset bloat |
| Visual Effects | Particle effects only | ⚠️ Partial coverage |
| Hologram | Invisible NPC + Nameplate | ✅ Functional |
| Permissions | Custom storage framework | ✅ Functional |

---

## 🔄 SDK Version Tracking

| SDK Version | Date | New APIs Added |
|-------------|------|----------------|
| (unknown) | 2026-01 | `CameraShakeEffect`, basic ECS, `ParticleUtil` |

*Update this document as new SDK versions provide additional APIs.*

---

## 📝 Submission Notes

When submitting feature requests to Hypixel Studios:

1. **Prioritize by use case impact**
2. **Provide code examples** showing expected API usage
3. **Reference existing patterns** in the SDK for consistency
4. **Offer to test beta APIs** when available

---

## Change Log

| Date | Changes |
|------|---------|
| 2026-01-31 | Initial wishlist based on accessor audit |
