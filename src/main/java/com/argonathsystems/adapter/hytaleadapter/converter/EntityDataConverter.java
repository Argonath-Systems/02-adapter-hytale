package com.argonathsystems.adapter.hytaleadapter.converter;

import com.argonathsystems.framework.accessorapi.dto.EntityData;
import com.argonathsystems.framework.accessorapi.dto.LocationData;
import com.hytale.api.entity.Entity;
import com.hytale.api.Location;

import java.util.UUID;

/**
 * Converts between Hytale Entity and platform-agnostic EntityData DTO.
 * 
 * <p>Handles:
 * <ul>
 *   <li>Entity identity (UUID, type, custom name)</li>
 *   <li>Location data</li>
 *   <li>Health/max health</li>
 *   <li>ECS component access (when Hytale uses ECS pattern)</li>
 * </ul>
 * 
 * <p><strong>Hytale ECS Pattern:</strong> Hytale uses Entity-Component-System.
 * Real implementations should use:
 * <pre>
 * Entity entity = ...
 * TransformComponent transform = entity.getComponent(TransformComponent.class);
 * HealthComponent health = entity.getComponent(HealthComponent.class);
 * </pre>
 * 
 * @author Argonath Systems Team
 * @version 2.0.0
 * @since 1.0.0
 */
public class EntityDataConverter {
    
    /**
     * Convert Hytale Entity to platform-agnostic EntityData.
     * 
     * @param hytaleEntity The Hytale entity
     * @return EntityData DTO, or null if input is null
     */
    public static EntityData toDTO(Entity hytaleEntity) {
        if (hytaleEntity == null) return null;
        
        // Extract basic identity
        UUID id = hytaleEntity.getUniqueId();
        String type = hytaleEntity.getType();
        String customName = hytaleEntity.getName();
        
        // Convert location
        Location hytaleLocation = hytaleEntity.getLocation();
        LocationData location = LocationConverter.toDTO(hytaleLocation);
        
        // Extract health (cast to int for DTO)
        // TODO: When Hytale ECS is available, use:
        // HealthComponent healthComp = hytaleEntity.getComponent(HealthComponent.class);
        // int health = healthComp != null ? (int) healthComp.getHealth() : 0;
        // int maxHealth = healthComp != null ? (int) healthComp.getMaxHealth() : 0;
        
        int health = (int) hytaleEntity.getHealth();
        int maxHealth = (int) hytaleEntity.getMaxHealth();
        
        return new EntityData(
            id,
            type,
            customName,
            location,
            health,
            maxHealth
        );
    }
    
    /**
     * Convert platform-agnostic EntityData back to Hytale Entity.
     * 
     * <p><strong>Note:</strong> This is typically not possible directly as entities
     * are created by the server. This method would be used for updating an existing
     * entity's mutable properties.
     * 
     * <p><strong>Future Implementation:</strong> Given an existing entity reference,
     * apply the DTO's data to it:
     * <pre>
     * entity.setCustomName(dto.customName());
     * entity.teleport(LocationConverter.fromDTO(dto.location()));
     * entity.setHealth(dto.health());
     * </pre>
     * 
     * @param dto The EntityData DTO
     * @param existingEntity The existing entity to update (cannot create new entities from DTOs)
     */
    public static void applyToEntity(EntityData dto, Entity existingEntity) {
        if (dto == null || existingEntity == null) {
            throw new IllegalArgumentException("Both DTO and existing entity must be non-null");
        }
        
        // Verify entity identity matches (safety check)
        if (!existingEntity.getUniqueId().equals(dto.id())) {
            throw new IllegalArgumentException(
                "Entity UUID mismatch: DTO has " + dto.id() + 
                " but entity has " + existingEntity.getUniqueId()
            );
        }
        
        // Apply mutable properties
        // TODO: When Hytale API supports these setters:
        // if (dto.hasCustomName()) {
        //     existingEntity.setCustomName(dto.customName());
        // }
        // 
        // Location newLocation = LocationConverter.fromDTO(dto.location());
        // existingEntity.teleport(newLocation);
        // 
        // existingEntity.setHealth(dto.health());
        
        // Current stub API only supports setHealth
        existingEntity.setHealth(dto.health());
        
        // Location teleport
        if (dto.location() != null) {
            Location newLocation = LocationConverter.fromDTO(dto.location());
            existingEntity.teleport(newLocation);
        }
    }
    
    /**
     * Safely extract component from entity using ECS pattern.
     * 
     * <p>This is a helper for when Hytale's full ECS API is available.
     * 
     * @param entity The entity
     * @param componentClass The component class
     * @param <T> Component type
     * @return Component instance, or null if not present
     */
    @SuppressWarnings("unused")
    private static <T> T getComponentSafe(Entity entity, Class<T> componentClass) {
        if (entity == null || componentClass == null) return null;
        
        try {
            return entity.getComponent(componentClass);
        } catch (Exception e) {
            // Component not available on this entity type
            System.err.println("Failed to get component " + componentClass.getSimpleName() + 
                               " from entity " + entity.getUniqueId() + ": " + e.getMessage());
            return null;
        }
    }
}