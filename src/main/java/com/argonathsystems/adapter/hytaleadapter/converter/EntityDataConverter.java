package com.argonathsystems.adapter.hytaleadapter.converter;

import com.argonathsystems.framework.accessorapi.dto.EntityData;

/**
 * <p><b>MIGRATION-001 Status:</b> BLOCKED - Requires Official Hytale SDK</p>
 * 
 * <p>Converter for Entity (Hytale SDK) to EntityData (Platform-agnostic DTO).</p>
 * <p>Implementation requires the official Hytale SDK (com.hypixel.hytale.*)
 * which is not available in the development environment.</p>
 * 
 * <p>All methods throw UnsupportedOperationException until the SDK is available.</p>
 */
public class EntityDataConverter {
    
    /**
     * Convert from Hytale Entity to platform-agnostic EntityData.
     * @param entity Hytale SDK Entity object
     * @return EntityData DTO
     * @throws UnsupportedOperationException Always - requires Hytale SDK
     */
    public static EntityData toDTO(Object entity) {
        throw new UnsupportedOperationException(
            "EntityDataConverter.toDTO() requires official Hytale SDK (com.hypixel.hytale.server.core.entity.Entity). " +
            "Expected pattern: new EntityData(entity.getUniqueId(), entity.getType(), entity.getLocation())"
        );
    }
    
    /**
     * Convert from platform-agnostic EntityData to Hytale Entity.
     * @param dto EntityData DTO
     * @return Hytale SDK Entity object
     * @throws UnsupportedOperationException Always - requires Hytale SDK
     */
    public static Object fromDTO(EntityData dto) {
        throw new UnsupportedOperationException(
            "EntityDataConverter.fromDTO() requires official Hytale SDK (com.hypixel.hytale.server.core.entity.Entity). " +
            "Expected pattern: world.spawnEntity(dto.getEntityType(), dto.getLocation())"
        );
    }
}
