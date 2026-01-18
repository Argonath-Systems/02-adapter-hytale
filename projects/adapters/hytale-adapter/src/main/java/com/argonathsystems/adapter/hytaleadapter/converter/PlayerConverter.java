package com.argonathsystems.adapter.hytaleadapter.converter;

import com.hytale.api.entity.Player;
import com.argonathsystems.framework.accessorapi.dto.PlayerData;

public class PlayerConverter {
    public static PlayerData toDTO(Player player) {
        // Validation: Hytale imports ONLY in this module
        if (player == null) return null;
        
        return new PlayerData(
            player.getUniqueId(),
            player.getName(),
            player.getHealth(),
            player.getMaxHealth()
        );
    }
}