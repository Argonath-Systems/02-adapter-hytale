package com.argonathsystems.adapter.hytaleadapter.converter;

import com.argonathsystems.framework.accessorapi.dto.LocationData;
// import com.hytale.api.component.TransformComponent;

public class LocationConverter {
    public static LocationData toDTO(Object hytaleLocation) {
        if (hytaleLocation == null) return null;
        // Conversion logic here
        return new LocationData("world", 0, 0, 0, 0, 0);
    }

    public static Object fromDTO(LocationData dto) {
        if (dto == null) return null;
        // Conversion logic here
        return null;
    }
}