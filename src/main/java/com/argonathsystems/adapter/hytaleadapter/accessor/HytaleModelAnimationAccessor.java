package com.argonathsystems.adapter.hytaleadapter.accessor;

import com.argonathsystems.framework.accessorapi.ModelAnimationAccessor;
import com.hypixel.hytale.server.core.HytaleServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Hytale implementation of ModelAnimationAccessor.
 * 
 * <p>Uses the Hytale SDK's ModelAsset system to discover animations
 * associated with NPC/entity models. The SDK provides:</p>
 * <ul>
 *   <li>{@code ModelAsset.getAnimationSetMap()} — Map of animation set name → AnimationSet</li>
 *   <li>{@code ModelAsset.AnimationSet.getAnimations()} — Array of Animation within a set</li>
 *   <li>{@code ModelAsset.Animation.getSoundEventId()} — Built-in sound-animation pairing</li>
 * </ul>
 * 
 * <h2>SDK Integration Status</h2>
 * <p>The SDK ModelAsset classes exist at runtime but the exact access pattern
 * for enumerating loaded model assets depends on the AssetStore registry.
 * This implementation uses {@code HytaleServer.get().getRegistry()} for
 * model asset lookup, with a caching layer to avoid repeated asset lookups.</p>
 * 
 * <h2>Specification Reference</h2>
 * <ul>
 *   <li>SF-NPC-041: NPC Audio Integration</li>
 *   <li>IMPL-PLAN-2026-Q1-NPC-QUEST-ANIMATION: Phase 1</li>
 * </ul>
 * 
 * @author Argonath Systems Team
 * @version 2.0.0
 * @since 2026-01-30
 */
public class HytaleModelAnimationAccessor implements ModelAnimationAccessor {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(HytaleModelAnimationAccessor.class);
    
    // Cache to avoid repeated asset lookups
    private final Map<String, Map<String, AnimationSetInfo>> animationCache = new ConcurrentHashMap<>();
    
    /**
     * Constructs the accessor.
     * 
     * <p>Animation data is lazily loaded from ModelAsset when first requested.
     * The SDK's ModelAsset provides {@code getAnimationSetMap()} which returns
     * all animation sets for a given model.</p>
     */
    public HytaleModelAnimationAccessor() {
        LOGGER.info("HytaleModelAnimationAccessor initialized — uses ModelAsset.getAnimationSetMap()");
    }
    
    @Override
    public Map<String, AnimationSetInfo> getAnimationSets(String modelAssetId) {
        Objects.requireNonNull(modelAssetId, "modelAssetId");
        
        // Check cache first
        Map<String, AnimationSetInfo> cached = animationCache.get(modelAssetId);
        if (cached != null) {
            return cached;
        }
        
        // Attempt to load from SDK ModelAsset registry
        Map<String, AnimationSetInfo> result = loadAnimationSetsFromSdk(modelAssetId);
        if (result != null && !result.isEmpty()) {
            animationCache.put(modelAssetId, result);
            return result;
        }
        
        LOGGER.debug("No animation sets found for model: {}", modelAssetId);
        return Collections.emptyMap();
    }
    
    /**
     * Load animation sets from the SDK ModelAsset system.
     * 
     * <p>Uses reflection-based access to ModelAsset since the exact import
     * path and generic types of AssetStore depend on runtime SDK version.
     * The pattern is: ModelAsset.getAssetStore().getAssetMap().get(modelId)
     * → modelAsset.getAnimationSetMap() → Map&lt;String, AnimationSet&gt;</p>
     * 
     * @param modelAssetId the model asset identifier (e.g., "Kweebec", "Trork")
     * @return map of animation set name to AnimationSetInfo, or empty map
     */
    @SuppressWarnings("unchecked")
    private Map<String, AnimationSetInfo> loadAnimationSetsFromSdk(String modelAssetId) {
        try {
            // SDK Pattern: Access ModelAsset via its AssetStore
            // ModelAsset is at com.hypixel.hytale.server.core.entity.model.ModelAsset
            Class<?> modelAssetClass = Class.forName("com.hypixel.hytale.server.core.entity.model.ModelAsset");
            
            // Get the AssetStore singleton for ModelAsset
            var getAssetStoreMethod = modelAssetClass.getMethod("getAssetStore");
            Object assetStore = getAssetStoreMethod.invoke(null);
            if (assetStore == null) {
                LOGGER.debug("ModelAsset.getAssetStore() returned null");
                return Collections.emptyMap();
            }
            
            // Get the asset map: Map<String, ModelAsset>
            var getAssetMapMethod = assetStore.getClass().getMethod("getAssetMap");
            Map<?, ?> assetMap = (Map<?, ?>) getAssetMapMethod.invoke(assetStore);
            if (assetMap == null || assetMap.isEmpty()) {
                LOGGER.debug("ModelAsset asset map is empty");
                return Collections.emptyMap();
            }
            
            // Find our model by ID
            Object modelAsset = assetMap.get(modelAssetId);
            if (modelAsset == null) {
                LOGGER.debug("Model asset not found: {}", modelAssetId);
                return Collections.emptyMap();
            }
            
            // Get animation sets: Map<String, AnimationSet>
            var getAnimSetMapMethod = modelAsset.getClass().getMethod("getAnimationSetMap");
            Map<String, ?> animSetMap = (Map<String, ?>) getAnimSetMapMethod.invoke(modelAsset);
            if (animSetMap == null || animSetMap.isEmpty()) {
                return Collections.emptyMap();
            }
            
            // Convert SDK AnimationSet objects to our AnimationSetInfo records
            Map<String, AnimationSetInfo> result = new LinkedHashMap<>();
            for (Map.Entry<String, ?> entry : animSetMap.entrySet()) {
                String setName = entry.getKey();
                Object animSet = entry.getValue();
                
                AnimationSetInfo info = convertAnimationSet(setName, animSet);
                if (info != null) {
                    result.put(setName, info);
                }
            }
            
            LOGGER.debug("Loaded {} animation sets for model: {}", result.size(), modelAssetId);
            return result;
            
        } catch (ClassNotFoundException e) {
            LOGGER.debug("ModelAsset class not found — SDK may have changed package structure: {}", e.getMessage());
        } catch (NoSuchMethodException e) {
            LOGGER.debug("Expected method not found on ModelAsset: {}", e.getMessage());
        } catch (Exception e) {
            LOGGER.warn("Error loading animation sets for model {}: {}", modelAssetId, e.getMessage(), e);
        }
        
        return Collections.emptyMap();
    }
    
    /**
     * Convert an SDK AnimationSet object to our AnimationSetInfo record.
     */
    @SuppressWarnings("unchecked")
    private AnimationSetInfo convertAnimationSet(String setName, Object sdkAnimSet) {
        try {
            // AnimationSet.getAnimations() → Animation[]
            var getAnimationsMethod = sdkAnimSet.getClass().getMethod("getAnimations");
            Object[] animations = (Object[]) getAnimationsMethod.invoke(sdkAnimSet);
            
            if (animations == null || animations.length == 0) {
                return new AnimationSetInfo(setName, Collections.emptyList(), 0.0f, 0.0f);
            }
            
            List<AnimationInfo> animInfoList = new ArrayList<>();
            for (Object anim : animations) {
                AnimationInfo info = convertAnimation(anim);
                if (info != null) {
                    animInfoList.add(info);
                }
            }
            
            return new AnimationSetInfo(setName, animInfoList, 0.0f, 0.0f);
            
        } catch (Exception e) {
            LOGGER.debug("Error converting animation set '{}': {}", setName, e.getMessage());
            return new AnimationSetInfo(setName, Collections.emptyList(), 0.0f, 0.0f);
        }
    }
    
    /**
     * Convert an SDK Animation object to our AnimationInfo record.
     * 
     * <p>Extracts: animation name/id, sound event id, speed, looping flag.</p>
     */
    private AnimationInfo convertAnimation(Object sdkAnimation) {
        try {
            // Animation.getAnimation() → String (animation id/name)
            String animId = null;
            try {
                var getAnimation = sdkAnimation.getClass().getMethod("getAnimation");
                animId = (String) getAnimation.invoke(sdkAnimation);
            } catch (NoSuchMethodException e) {
                // Try getName() as fallback
                try {
                    var getName = sdkAnimation.getClass().getMethod("getName");
                    animId = (String) getName.invoke(sdkAnimation);
                } catch (NoSuchMethodException ex) {
                    animId = sdkAnimation.toString();
                }
            }
            
            // Animation.getSoundEventId() → String (may be null)
            String soundEventId = null;
            try {
                var getSoundEventId = sdkAnimation.getClass().getMethod("getSoundEventId");
                soundEventId = (String) getSoundEventId.invoke(sdkAnimation);
            } catch (NoSuchMethodException ignored) {
                // Sound event not available for this animation
            }
            
            // Animation.getSpeed() → float
            float speed = 1.0f;
            try {
                var getSpeed = sdkAnimation.getClass().getMethod("getSpeed");
                speed = ((Number) getSpeed.invoke(sdkAnimation)).floatValue();
            } catch (NoSuchMethodException ignored) {
                // Default speed
            }
            
            // Animation.isLooping() → boolean
            boolean looping = false;
            try {
                var isLooping = sdkAnimation.getClass().getMethod("isLooping");
                looping = (Boolean) isLooping.invoke(sdkAnimation);
            } catch (NoSuchMethodException ignored) {
                // Default non-looping
            }
            
            return new AnimationInfo(
                animId,
                soundEventId,
                speed,
                0.0f,    // blendingDuration — default, not available via reflection
                looping,
                1.0f,    // weight — default
                new int[0] // footstepIntervals — not available via reflection
            );
            
        } catch (Exception e) {
            LOGGER.debug("Error converting animation: {}", e.getMessage());
            return null;
        }
    }
    
    @Override
    public List<AnimationInfo> getAnimations(String modelAssetId, String animationSetName) {
        Objects.requireNonNull(modelAssetId, "modelAssetId");
        Objects.requireNonNull(animationSetName, "animationSetName");
        
        Map<String, AnimationSetInfo> sets = getAnimationSets(modelAssetId);
        AnimationSetInfo set = sets.get(animationSetName);
        
        if (set == null) {
            return Collections.emptyList();
        }
        
        return set.animations();
    }
    
    @Override
    public Optional<AnimationSetInfo> getDefaultAnimationSet(String modelAssetId) {
        Objects.requireNonNull(modelAssetId, "modelAssetId");
        
        Map<String, AnimationSetInfo> sets = getAnimationSets(modelAssetId);
        
        if (sets.isEmpty()) {
            return Optional.empty();
        }
        
        // Try "default" first, then fall back to first entry
        if (sets.containsKey("default")) {
            return Optional.of(sets.get("default"));
        }
        
        return Optional.of(sets.values().iterator().next());
    }
    
    @Override
    public List<AnimationSoundPair> getAnimationsWithSounds(String modelAssetId) {
        Objects.requireNonNull(modelAssetId, "modelAssetId");
        
        // Use the animation sets to find all animations with sound event IDs
        Map<String, AnimationSetInfo> sets = getAnimationSets(modelAssetId);
        List<AnimationSoundPair> pairs = new ArrayList<>();
        
        for (AnimationSetInfo setInfo : sets.values()) {
            for (AnimationInfo anim : setInfo.animations()) {
                if (anim.soundEventId() != null && !anim.soundEventId().isEmpty()) {
                    pairs.add(new AnimationSoundPair(anim.animationId(), anim.soundEventId()));
                }
            }
        }
        
        return pairs;
    }
    
    @Override
    public Optional<AnimationInfo> getAnimation(String modelAssetId, String animationId) {
        Objects.requireNonNull(modelAssetId, "modelAssetId");
        Objects.requireNonNull(animationId, "animationId");
        
        // Search across all animation sets for the given animation ID
        Map<String, AnimationSetInfo> sets = getAnimationSets(modelAssetId);
        for (AnimationSetInfo setInfo : sets.values()) {
            for (AnimationInfo anim : setInfo.animations()) {
                if (animationId.equals(anim.animationId())) {
                    return Optional.of(anim);
                }
            }
        }
        
        return Optional.empty();
    }
    
    /**
     * Clears the animation cache for a specific model or all models.
     * 
     * @param modelAssetId Model ID to clear, or null to clear all
     */
    public void clearCache(String modelAssetId) {
        if (modelAssetId == null) {
            animationCache.clear();
            LOGGER.debug("Cleared all animation cache");
        } else {
            animationCache.remove(modelAssetId);
            LOGGER.debug("Cleared animation cache for: {}", modelAssetId);
        }
    }
}
