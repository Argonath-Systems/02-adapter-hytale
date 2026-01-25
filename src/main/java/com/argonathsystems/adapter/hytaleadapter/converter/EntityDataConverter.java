package com.argonathsystems.adapter.hytaleadapter.converter;

import com.argonathsystems.framework.accessorapi.dto.EntityData;
import com.hytale.api.entity.Entity;

public class EntityDataConverter {
    public static EntityData toDTO(Entity hytaleEntity) {
        if (hytaleEntity == null) return null;
        return new EntityData(
            hytaleEntity.getUniqueId(),
            hytaleEntity.getType(),
            hytaleEntity.getName(),
            LocationConverter.toDTO(hytaleEntity.getLocation()),
            (int) hytaleEntity.getHealth(),
            (int) hytaleEntity.getMaxHealth()
        );
    }
}