package com.argonathsystems.adapter.hytaleadapter.util;

import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.entity.Entity;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import java.util.Optional;

/**
 * Helper for ECS component access in Hytale's Entity-Component-System architecture.
 * 
 * <p><b>MIGRATION-001 Status:</b> ✅ IMPLEMENTED</p>
 * 
 * <p>Hytale uses an ECS pattern where entities are composed of components.
 * This helper provides safe, null-checked component access via the Store.</p>
 * 
 * <h2>SDK Pattern</h2>
 * <pre>{@code
 * // Direct SDK usage via Store:
 * Ref<EntityStore> ref = entity.getReference();
 * Store<EntityStore> store = ref.getStore();
 * Component comp = store.getComponent(ref, componentType);
 * if (comp != null) {
 *     // use component
 * }
 * 
 * // Using this helper:
 * Optional<Component> comp = ComponentHelper.getComponent(entity, componentType);
 * }</pre>
 * 
 * @author Argonath Systems Team
 * @version 1.2.0
 * @since MIGRATION-001
 */
public class ComponentHelper {
    
    /**
     * Gets a component from an entity using the ECS pattern.
     * 
     * @param <T> The component type (must extend Component)
     * @param entity The entity to get the component from
     * @param componentType The ComponentType descriptor
     * @return Optional containing the component, or empty if not present
     */
    public static <T extends Component<EntityStore>> Optional<T> getComponent(
            Entity entity, 
            ComponentType<EntityStore, T> componentType) {
        if (entity == null || componentType == null) {
            return Optional.empty();
        }
        
        try {
            Ref<EntityStore> ref = entity.getReference();
            if (ref == null || !ref.isValid()) {
                return Optional.empty();
            }
            
            Store<EntityStore> store = ref.getStore();
            if (store == null) {
                return Optional.empty();
            }
            
            T component = store.getComponent(ref, componentType);
            return Optional.ofNullable(component);
        } catch (Exception e) {
            // Component access can fail if entity is being removed
            return Optional.empty();
        }
    }
    
    /**
     * Gets a component from an entity reference using the Store.
     * 
     * @param <T> The component type (must extend Component)
     * @param entityRef The entity reference
     * @param componentType The ComponentType descriptor
     * @return Optional containing the component, or empty if not present
     */
    public static <T extends Component<EntityStore>> Optional<T> getComponent(
            Ref<EntityStore> entityRef, 
            ComponentType<EntityStore, T> componentType) {
        if (entityRef == null || componentType == null || !entityRef.isValid()) {
            return Optional.empty();
        }
        
        try {
            Store<EntityStore> store = entityRef.getStore();
            if (store == null) {
                return Optional.empty();
            }
            
            T component = store.getComponent(entityRef, componentType);
            return Optional.ofNullable(component);
        } catch (Exception e) {
            return Optional.empty();
        }
    }
    
    /**
     * Checks if an entity has a specific component.
     * 
     * @param <T> The component type (must extend Component)
     * @param entity The entity to check
     * @param componentType The ComponentType descriptor
     * @return true if the entity has the component
     */
    public static <T extends Component<EntityStore>> boolean hasComponent(
            Entity entity, 
            ComponentType<EntityStore, T> componentType) {
        return getComponent(entity, componentType).isPresent();
    }
    
    /**
     * Gets a component or throws if not present.
     * Use when component is required and absence is an error.
     * 
     * @param <T> The component type (must extend Component)
     * @param entity The entity
     * @param componentType The ComponentType descriptor
     * @return The component (never null)
     * @throws IllegalStateException if component is not present
     */
    public static <T extends Component<EntityStore>> T requireComponent(
            Entity entity, 
            ComponentType<EntityStore, T> componentType) {
        return getComponent(entity, componentType)
            .orElseThrow(() -> new IllegalStateException(
                "Required component " + componentType + " not found on entity " + entity
            ));
    }
}