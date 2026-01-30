package com.argonathsystems.adapter.hytaleadapter.converter;

import com.argonathsystems.framework.accessorapi.dto.LocationData;
import com.hypixel.hytale.math.vector.Location;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.math.vector.Transform;

/**
 * Converts between Hytale Location and platform-agnostic LocationData DTO.
 * 
 * <p><b>MIGRATION-001 Status:</b> ✅ IMPLEMENTED</p>
 * 
 * <p>SDK Classes Used:</p>
 * <ul>
 *   <li>{@code com.hypixel.hytale.math.vector.Location} - World + position + rotation</li>
 *   <li>{@code com.hypixel.hytale.math.vector.Vector3d} - Position (x, y, z)</li>
 *   <li>{@code com.hypixel.hytale.math.vector.Vector3f} - Rotation (yaw, pitch, roll)</li>
 *   <li>{@code com.hypixel.hytale.math.vector.Transform} - Full transform data</li>
 * </ul>
 * 
 * @author Argonath Systems Team
 * @version 3.0.0
 * @since MIGRATION-001
 */
public class LocationConverter {
    
    /**
     * Convert Hytale Location to platform-agnostic LocationData.
     * 
     * @param hytaleLocation The Hytale location (must be Location, Vector3d, or Transform)
     * @return LocationData DTO
     * @throws IllegalArgumentException if hytaleLocation is not a supported type
     */
    public static LocationData toDTO(Object hytaleLocation) {
        if (hytaleLocation == null) {
            return null;
        }
        
        if (hytaleLocation instanceof Location location) {
            Vector3d pos = location.getPosition();
            Vector3f rot = location.getRotation();
            return new LocationData(
                location.getWorld(),
                pos.getX(),
                pos.getY(),
                pos.getZ(),
                rot != null ? rot.getX() : 0f, // yaw
                rot != null ? rot.getY() : 0f  // pitch
            );
        }
        
        if (hytaleLocation instanceof Vector3d vec) {
            return new LocationData(
                "world", // Default world name
                vec.getX(),
                vec.getY(),
                vec.getZ(),
                0f,
                0f
            );
        }
        
        if (hytaleLocation instanceof Transform transform) {
            // Transform contains position and rotation
            Location loc = new Location(transform);
            return toDTO(loc);
        }
        
        throw new IllegalArgumentException(
            "LocationConverter.toDTO() expected Location, Vector3d, or Transform but got: " + 
            hytaleLocation.getClass().getName()
        );
    }

    /**
     * Convert platform-agnostic LocationData to Hytale Location.
     * 
     * @param dto The LocationData DTO
     * @return Hytale Location
     */
    public static Location fromDTO(LocationData dto) {
        if (dto == null) {
            return null;
        }
        
        return new Location(
            dto.world(),
            dto.x(),
            dto.y(),
            dto.z(),
            dto.yaw(),
            dto.pitch(),
            0f // roll - not in LocationData
        );
    }
    
    /**
     * Convert LocationData to Hytale Vector3d (position only).
     * 
     * @param dto The LocationData DTO
     * @return Hytale Vector3d with position coordinates
     */
    public static Vector3d toVector3d(LocationData dto) {
        if (dto == null) {
            return null;
        }
        return new Vector3d(dto.x(), dto.y(), dto.z());
    }
    
    /**
     * Convert Hytale Vector3d to LocationData (with default world and rotation).
     * 
     * @param vec The Hytale Vector3d
     * @param worldName The world name to use
     * @return LocationData DTO
     */
    public static LocationData fromVector3d(Vector3d vec, String worldName) {
        if (vec == null) {
            return null;
        }
        return new LocationData(
            worldName != null ? worldName : "world",
            vec.getX(),
            vec.getY(),
            vec.getZ()
        );
    }
}