package com.argonathsystems.adapter.hytaleadapter.converter;

import com.argonathsystems.framework.accessorapi.dto.LocationData;
import com.hytale.api.Location;

/**
 * Converts between Hytale Location and platform-agnostic LocationData DTO.
 * 
 * <p>Handles complete bidirectional conversion of:
 * <ul>
 *   <li>World name</li>
 *   <li>Coordinates (x, y, z)</li>
 *   <li>Rotation (yaw, pitch)</li>
 * </ul>
 * 
 * @author Argonath Systems Team
 * @version 2.0.0
 * @since 1.0.0
 */
public class LocationConverter {
    
    /**
     * Convert Hytale Location to platform-agnostic LocationData.
     * 
     * @param hytaleLocation The Hytale location
     * @return LocationData DTO, or null if input is null
     */
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

    /**
     * Convert platform-agnostic LocationData to Hytale Location.
     * 
     * @param dto The LocationData DTO
     * @return Hytale Location, or null if input is null
     */
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