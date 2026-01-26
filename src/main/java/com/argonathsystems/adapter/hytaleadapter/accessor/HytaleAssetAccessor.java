package com.argonathsystems.adapter.hytaleadapter.accessor;

import com.argonathsystems.framework.accessorapi.AssetAccessor;
import com.hytale.api.Server;
import java.io.InputStream;
import java.util.Optional;

/**
 * Hytale implementation of AssetAccessor.
 */
public class HytaleAssetAccessor implements AssetAccessor {
    
    private final Server server;

    public HytaleAssetAccessor(Server server) {
        this.server = server;
    }

    @Override
    public Optional<InputStream> getAsset(String path) {
        // In a real implementation, we would use Hytale's AssetManager.
        // Since the API is not fully known/exposed in this context, we fall back to ClassLoader.
        // Example API usage: return Optional.ofNullable(server.getAssetManager().getAsset(path));
        
        InputStream stream = getClass().getClassLoader().getResourceAsStream(path);
        if (stream == null && !path.startsWith("/")) {
             stream = getClass().getClassLoader().getResourceAsStream("/" + path);
        }
        return Optional.ofNullable(stream);
    }
}
