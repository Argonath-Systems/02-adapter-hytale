package com.argonathsystems.adapter.hytaleadapter.accessor;

import com.argonathsystems.framework.accessorapi.ModelAnimationAccessor;
import com.hypixel.hytale.server.core.asset.AssetStoreManager;
import com.hypixel.hytale.server.core.asset.type.model.config.Model;
import com.hypixel.hytale.server.core.asset.type.model.config.ModelAsset;
import com.hypixel.hytale.server.core.asset.type.model.config.ModelAsset.Animation;
import com.hypixel.hytale.server.core.asset.type.model.config.ModelAsset.AnimationSet;
import com.hypixel.hytale.protocol.Rangef;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Hytale implementation of ModelAnimationAccessor.
 * 
 * <p>Queries Hytale's ModelAsset API to discover animations and their
 * associated sound events for NPC models.
 * 
 * <h2>Hytale SDK Integration</h2>
 * <ul>
 *   <li>{@code ModelAsset.getAnimationSetMap()} → Map of animation sets</li>
 *   <li>{@code AnimationSet.getAnimations()} → Array of animations</li>
 *   <li>{@code Animation.getSoundEventId()} → Native sound event pairing</li>
 * </ul>
 * 
 * <h2>Specification Reference</h2>
 * <ul>
 *   <li>SF-NPC-041: NPC Audio Integration</li>
 *   <li>IMPL-PLAN-2026-Q1-NPC-QUEST-ANIMATION: Phase 1</li>
 * </ul>
 * 
 * @author Argonath Systems Team
 * @version 1.0.0
 * @since 2026-01-30
 */
public class HytaleModelAnimationAccessor implements ModelAnimationAccessor {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(HytaleModelAnimationAccessor.class);
    
    private final AssetStoreManager assetStoreManager;
    
    // Cache to avoid repeated asset lookups
    private final Map<String, Map<String, AnimationSetInfo>> animationCache = new ConcurrentHashMap<>();
    
    /**
     * Constructs the accessor with the Hytale asset store manager.
     * 
     * @param assetStoreManager Hytale's asset store manager
     */
    public HytaleModelAnimationAccessor(AssetStoreManager assetStoreManager) {
        this.assetStoreManager = Objects.requireNonNull(assetStoreManager, "assetStoreManager");
        LOGGER.info("HytaleModelAnimationAccessor initialized");
    }
    
    @Override
    public Map<String, AnimationSetInfo> getAnimationSets(String modelAssetId) {
        Objects.requireNonNull(modelAssetId, "modelAssetId");
        
        // Check cache first
        Map<String, AnimationSetInfo> cached = animationCache.get(modelAssetId);
        if (cached != null) {
            return cached;
        }
        
        // Query the asset store
        try {
            Model model = getModel(modelAssetId);
            if (model == null) {
                LOGGER.debug("Model not found: {}", modelAssetId);
                return Collections.emptyMap();
            }
            
            Map<String, AnimationSet> setMap = model.getAnimationSetMap();
            if (setMap == null || setMap.isEmpty()) {
                LOGGER.debug("No animation sets for model: {}", modelAssetId);
                return Collections.emptyMap();
            }
            
            Map<String, AnimationSetInfo> result = setMap.entrySet().stream()
                .collect(Collectors.toMap(
                    Map.Entry::getKey,
                    e -> convertAnimationSet(e.getKey(), e.getValue())
                ));
            
            // Cache the result
            animationCache.put(modelAssetId, Collections.unmodifiableMap(result));
            LOGGER.debug("Discovered {} animation sets for model: {}", result.size(), modelAssetId);
            
            return result;
            
        } catch (Exception e) {
            LOGGER.error("Failed to query animations for model: {}", modelAssetId, e);
            return Collections.emptyMap();
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
        
        return getAnimationSets(modelAssetId).values().stream()
            .flatMap(set -> set.animations().stream())
            .filter(anim -> anim.soundEventId() != null && !anim.soundEventId().isEmpty())
            .map(anim -> new AnimationSoundPair(anim.animationId(), anim.soundEventId()))
            .collect(Collectors.toList());
    }
    
    @Override
    public Optional<AnimationInfo> getAnimation(String modelAssetId, String animationId) {
        Objects.requireNonNull(modelAssetId, "modelAssetId");
        Objects.requireNonNull(animationId, "animationId");
        
        return getAnimationSets(modelAssetId).values().stream()
            .flatMap(set -> set.animations().stream())
            .filter(anim -> anim.animationId().equals(animationId))
            .findFirst();
    }
    
    /**
     * Gets the Model from the asset store.
     */
    private Model getModel(String modelAssetId) {
        try {
            // The model asset ID may be a full path or just an identifier
            // Try ModelAsset first, then Model interface
            Object asset = assetStoreManager.getAsset(modelAssetId);
            
            if (asset instanceof Model model) {
                return model;
            }
            
            if (asset instanceof ModelAsset modelAsset) {
                // ModelAsset implements Model
                return modelAsset;
            }
            
            LOGGER.debug("Asset is not a Model type: {} ({})", 
                modelAssetId, asset != null ? asset.getClass().getName() : "null");
            return null;
            
        } catch (Exception e) {
            LOGGER.debug("Failed to get model asset: {}", modelAssetId, e);
            return null;
        }
    }
    
    /**
     * Converts a Hytale AnimationSet to our AnimationSetInfo record.
     */
    private AnimationSetInfo convertAnimationSet(String name, AnimationSet set) {
        Animation[] hytaleAnimations = set.getAnimations();
        
        List<AnimationInfo> animations;
        if (hytaleAnimations == null || hytaleAnimations.length == 0) {
            animations = Collections.emptyList();
        } else {
            animations = Arrays.stream(hytaleAnimations)
                .map(this::convertAnimation)
                .collect(Collectors.toList());
        }
        
        Rangef delay = set.getNextAnimationDelay();
        float delayMin = delay != null ? delay.min() : 0f;
        float delayMax = delay != null ? delay.max() : 0f;
        
        return new AnimationSetInfo(name, animations, delayMin, delayMax);
    }
    
    /**
     * Converts a Hytale Animation to our AnimationInfo record.
     */
    private AnimationInfo convertAnimation(Animation anim) {
        return new AnimationInfo(
            anim.getAnimation(),
            anim.getSoundEventId(),
            anim.getSpeed(),
            anim.getBlendingDuration(),
            anim.isLooping(),
            (float) anim.getWeight(),
            null // footstepIntervals are internal to the animation
        );
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
