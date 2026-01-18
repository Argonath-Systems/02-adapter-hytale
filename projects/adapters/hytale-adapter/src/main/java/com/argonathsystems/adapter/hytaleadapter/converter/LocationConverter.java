package com.argonathsystems.adapter.hytaleadapter.converter;

import com.argonathsystems.framework.accessorapi.dto.LocationData;
import com.hytale.api.Location;

public class LocationConverter {
    public static LocationData toDTO(Location hytaleLocation) {
        if (hytaleLocation == null) return null;
        return new LocationData(
            hytaleLocation.getWorldName(),
            hytaleLocation.getX(),
            hytaleLocation.getY(),
            hytaleLocation.getZ(),
            hytaleLocation.getYaw(),
            hytaleLocation.getPitch()
        );
    }

    public static Location fromDTO(LocationData dto) {
        if (dto == null) return null;
        return new Location(
            dto.world(),
            dto.x(),
            dto.y(),
            dto.z(),
            dto.yaw(),
            dto.pitch()
        );
    }
}