package com.argonathsystems.adapter.hytaleadapter.converter;

import com.argonathsystems.framework.accessorapi.dto.EntityData;
// import com.hytale.api.entity.Entity;

public class EntityDataConverter {
    public static EntityData toDTO(Object hytaleEntity) {
        if (hytaleEntity == null) return null;
        // Conversion logic here
        return new EntityData(java.util.UUID.randomUUID(), "entity_type", "name", null, 20, 20);
    }
}