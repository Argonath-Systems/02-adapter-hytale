package com.argonathsystems.adapter.hytale.accessor;

import au.ellie.hyui.builders.HudBuilder;
import au.ellie.hyui.builders.HyUIHud;
import com.argonathsystems.framework.accessorapi.CameraAccessor;
import com.hypixel.hytale.protocol.AccumulationMode;
import com.hypixel.hytale.protocol.ApplyLookType;
import com.hypixel.hytale.protocol.ClientCameraView;
import com.hypixel.hytale.protocol.Direction;
import com.hypixel.hytale.protocol.Position;
import com.hypixel.hytale.protocol.PositionDistanceOffsetType;
import com.hypixel.hytale.protocol.PositionType;
import com.hypixel.hytale.protocol.RotationType;
import com.hypixel.hytale.protocol.ServerCameraSettings;
import com.hypixel.hytale.protocol.packets.camera.CameraShakeEffect;
import com.hypixel.hytale.protocol.packets.camera.SetServerCamera;
import com.hypixel.hytale.server.core.io.PacketHandler;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Hytale implementation of CameraAccessor using the {@code SetServerCamera} packet API.
 *
 * <p>Provides server-side camera control via {@link ServerCameraSettings} and
 * {@link SetServerCamera} packets. The SDK exposes extensive camera manipulation
 * including position, rotation, distance, perspective, locking, and smooth follow.</p>
 *
 * <h2>SDK Integration (2026-02-07)</h2>
 * <p>Based on the Hytale SDK camera API documented at
 * <a href="https://hytalemodding.dev/en/docs/guides/plugin/customizing-camera-controls">
 * hytalemodding.dev — Customizing Camera Controls</a>.</p>
 *
 * <h2>SDK Pattern</h2>
 * <pre>{@code
 * ServerCameraSettings settings = new ServerCameraSettings();
 * settings.distance = 10.0f;
 * settings.rotationType = RotationType.Custom;
 * settings.rotation = new Direction(yaw, pitch, roll); // radians
 * settings.positionLerpSpeed = 0.2f;
 * playerRef.getPacketHandler().writeNoCache(
 *     new SetServerCamera(ClientCameraView.Custom, true, settings));
 * }</pre>
 *
 * <h2>Implemented Features</h2>
 * <ul>
 *   <li>✅ Camera position (via {@code ServerCameraSettings.position})</li>
 *   <li>✅ Camera rotation (via {@code ServerCameraSettings.rotation} + {@code Direction})</li>
 *   <li>✅ Camera distance/zoom (via {@code ServerCameraSettings.distance})</li>
 *   <li>✅ Force perspective (via {@code ClientCameraView.FirstPerson/ThirdPerson})</li>
 *   <li>✅ Camera shake (via {@code CameraShakeEffect} packet)</li>
 *   <li>✅ Letterbox (via HyUI HUD overlay)</li>
 *   <li>✅ Cinematic mode (compose: custom rotation + distance + lock + letterbox)</li>
 *   <li>✅ Smooth follow (via {@code positionLerpSpeed}, {@code rotationLerpSpeed})</li>
 *   <li>✅ Wall clip prevention (via {@code PositionDistanceOffsetType.DistanceOffsetRaycast})</li>
 * </ul>
 *
 * <h2>SDK Limitations (Still Blocked)</h2>
 * <ul>
 *   <li>❌ FOV control — no field in {@code ServerCameraSettings}</li>
 *   <li>❌ Depth of field — no post-processing API in server SDK</li>
 * </ul>
 *
 * <h2>Specification Reference</h2>
 * <ul>
 *   <li>SF-NPC-044: Dialogue Cinematic Camera</li>
 *   <li>IMPL-ADAPTER-PLAN-001: CAM-001 through CAM-006</li>
 * </ul>
 *
 * @author Argonath Systems Team
 * @version 2.0.0
 * @since 2026-01-31
 * @see ServerCameraSettings
 * @see SetServerCamera
 * @see CameraShakeEffect
 */
public class HytaleCameraAccessor implements CameraAccessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(HytaleCameraAccessor.class);
    private static final float DEFAULT_FOV = 70.0f;
    private static final float DEFAULT_DOF_DISTANCE = 10.0f;
    private static final float DEFAULT_DOF_APERTURE = 1.0f;

    /** Default smooth follow speed for position (0.0 = instant, 1.0 = max smoothing). */
    private static final float DEFAULT_POSITION_LERP_SPEED = 0.15f;

    /** Default smooth follow speed for rotation. */
    private static final float DEFAULT_ROTATION_LERP_SPEED = 0.15f;

    /** Default third-person camera distance from the player. */
    private static final float DEFAULT_CAMERA_DISTANCE = 6.0f;

    /** Cinematic mode camera distance (closer for dialogue framing). */
    private static final float CINEMATIC_CAMERA_DISTANCE = 4.0f;

    // Internal state tracking for each player
    private final Map<UUID, InternalCameraState> playerCameraStates = new ConcurrentHashMap<>();

    // Letterbox overlay HUDs per player (for HyUI-based letterbox workaround)
    private final Map<UUID, HyUIHud> letterboxHuds = new ConcurrentHashMap<>();

    /** HyUIML template for letterbox overlay — two black bars at top and bottom */
    private static final String LETTERBOX_TEMPLATE = """
        <div id="letterbox-overlay" style="width: 100%%; height: 100%%; position: absolute;">
            <div id="letterbox-top" style="position: absolute; top: 0; left: 0; width: 100%%; height: 12%%; background-color: #000000;"></div>
            <div id="letterbox-bottom" style="position: absolute; bottom: 0; left: 0; width: 100%%; height: 12%%; background-color: #000000;"></div>
        </div>
        """;

    /**
     * Constructs the camera accessor with SDK camera control support.
     */
    public HytaleCameraAccessor() {
        LOGGER.info("HytaleCameraAccessor initialized with SDK camera control " +
            "(SetServerCamera + ServerCameraSettings)");
    }

    // ========================================================================
    // State Management
    // ========================================================================

    @Override
    public CameraState getCameraState(UUID playerId) {
        InternalCameraState internal = getOrCreateState(playerId);
        return new CameraState(
            internal.position,
            internal.rotation,
            internal.fov,
            internal.letterboxEnabled,
            internal.dofFocalDistance,
            internal.dofAperture
        );
    }

    @Override
    public void setCameraState(UUID playerId, CameraState state) {
        InternalCameraState internal = getOrCreateState(playerId);
        internal.position = state.position();
        internal.rotation = state.rotation();
        internal.fov = state.fov();
        internal.letterboxEnabled = state.letterbox();
        internal.dofFocalDistance = state.dofFocalDistance();
        internal.dofAperture = state.dofAperture();

        // Apply position + rotation + letterbox to client via SDK
        PlayerRef playerRef = getPlayerRef(playerId);
        if (playerRef != null) {
            applyCameraSettings(playerRef, internal);

            if (state.letterbox()) {
                showLetterboxOverlay(playerRef);
            } else {
                hideLetterboxOverlay(playerId);
            }
        }

        LOGGER.debug("Applied camera state for player {}: pos={}, rot={}, fov={}",
            playerId, state.position(), state.rotation(), state.fov());
    }

    // ========================================================================
    // Position and Rotation — CAM-001, CAM-002
    // ========================================================================

    @Override
    public void setCameraPosition(UUID playerId, Vector3 position) {
        InternalCameraState state = getOrCreateState(playerId);
        state.position = position;

        PlayerRef playerRef = getPlayerRef(playerId);
        if (playerRef == null) {
            return;
        }

        // Build ServerCameraSettings with custom absolute position
        ServerCameraSettings settings = buildBaseSettings(state);
        settings.positionType = PositionType.Custom;
        settings.position = new Position(position.x(), position.y(), position.z());

        sendCameraPacket(playerRef, settings, state.cameraLocked);
        LOGGER.debug("Set camera position for player {}: ({}, {}, {})",
            playerId, position.x(), position.y(), position.z());
    }

    @Override
    public void setCameraRotation(UUID playerId, Vector2 rotation) {
        InternalCameraState state = getOrCreateState(playerId);
        state.rotation = rotation;

        PlayerRef playerRef = getPlayerRef(playerId);
        if (playerRef == null) {
            return;
        }

        // Build ServerCameraSettings with custom rotation
        // Vector2 = (pitch, yaw) in degrees → Direction(yaw, pitch, roll) in radians
        ServerCameraSettings settings = buildBaseSettings(state);
        settings.rotationType = RotationType.Custom;
        settings.rotation = degreesToDirection(rotation);
        // Force server-controlled rotation (don't let player mouse override)
        settings.applyLookType = ApplyLookType.Rotation;

        sendCameraPacket(playerRef, settings, state.cameraLocked);
        LOGGER.debug("Set camera rotation for player {}: pitch={}, yaw={}",
            playerId, rotation.x(), rotation.y());
    }

    // ========================================================================
    // Visual Effects — CAM-003 (letterbox), CAM-004 (shake)
    // ========================================================================

    @Override
    public void applyShake(UUID playerId, float intensity, long durationMs) {
        InternalCameraState state = getOrCreateState(playerId);
        state.shakeIntensity = intensity;
        state.shakeDurationMs = durationMs;
        state.shakeStartTime = System.currentTimeMillis();

        PlayerRef playerRef = getPlayerRef(playerId);
        if (playerRef == null) {
            return;
        }

        // CameraShakeEffect: (cameraShakeId, intensity, accumulationMode)
        CameraShakeEffect shakePacket = new CameraShakeEffect(
            0, // Default shake asset ID
            intensity,
            AccumulationMode.Sum
        );

        PacketHandler handler = playerRef.getPacketHandler();
        if (handler != null) {
            handler.write(shakePacket);
            LOGGER.debug("Applied camera shake for player {} (intensity: {}, duration: {}ms)",
                playerId, intensity, durationMs);
        } else {
            LOGGER.warn("Cannot apply camera shake: no packet handler for player {}", playerId);
        }
    }

    @Override
    public void setLetterboxMode(UUID playerId, boolean enabled) {
        InternalCameraState state = getOrCreateState(playerId);
        state.letterboxEnabled = enabled;

        // WORKAROUND: Letterbox via HyUI HUD overlay with black bars.
        // The SDK has no native letterbox API, so we render opaque HUD panels
        // at the top and bottom of the screen to create a cinematic effect.
        PlayerRef playerRef = getPlayerRef(playerId);
        if (playerRef == null) {
            return;
        }

        if (enabled) {
            showLetterboxOverlay(playerRef);
        } else {
            hideLetterboxOverlay(playerId);
        }

        LOGGER.debug("Set letterbox mode for player {}: {}", playerId, enabled);
    }

    @Override
    public boolean isLetterboxEnabled(UUID playerId) {
        return getOrCreateState(playerId).letterboxEnabled;
    }

    @Override
    public void setDepthOfField(UUID playerId, float focalDistance, float aperture) {
        InternalCameraState state = getOrCreateState(playerId);
        state.dofFocalDistance = focalDistance;
        state.dofAperture = aperture;

        // SDK LIMITATION: Hytale server SDK does not expose post-processing or
        // depth of field control. No field in ServerCameraSettings for DoF.
        // State is tracked internally for future SDK support.

        LOGGER.debug("Set DoF for player {}: focal={}, aperture={} [SDK-blocked: no post-processing API]",
            playerId, focalDistance, aperture);
    }

    @Override
    public void resetDepthOfField(UUID playerId) {
        setDepthOfField(playerId, DEFAULT_DOF_DISTANCE, DEFAULT_DOF_APERTURE);
    }

    @Override
    public void setFieldOfView(UUID playerId, float fov) {
        InternalCameraState state = getOrCreateState(playerId);
        state.fov = fov;

        // SDK LIMITATION: No FOV field in ServerCameraSettings.
        // FOV is managed client-side. State is tracked internally.

        LOGGER.trace("Set FOV for player {}: {} [SDK-blocked: no FOV field in ServerCameraSettings]",
            playerId, fov);
    }

    @Override
    public void resetFieldOfView(UUID playerId) {
        setFieldOfView(playerId, DEFAULT_FOV);
    }

    // ========================================================================
    // Cinematic Mode — CAM-005 (perspective), CAM-006 (cinematic)
    // ========================================================================

    @Override
    public void enableCinematicMode(UUID playerId) {
        InternalCameraState state = getOrCreateState(playerId);

        if (!state.cinematicMode) {
            // Save current state for restoration on disable
            state.savedPosition = state.position;
            state.savedRotation = state.rotation;
            state.savedDistance = state.distance;
            state.cinematicMode = true;
            state.cameraLocked = true;

            PlayerRef playerRef = getPlayerRef(playerId);
            if (playerRef == null) {
                return;
            }

            // Compose cinematic camera: custom rotation + closer distance + locked + letterbox
            ServerCameraSettings settings = new ServerCameraSettings();
            settings.positionLerpSpeed = DEFAULT_POSITION_LERP_SPEED;
            settings.rotationLerpSpeed = DEFAULT_ROTATION_LERP_SPEED;
            settings.distance = CINEMATIC_CAMERA_DISTANCE;
            settings.isFirstPerson = false;
            settings.eyeOffset = true;
            settings.positionDistanceOffsetType = PositionDistanceOffsetType.DistanceOffsetRaycast;

            // If custom rotation was set, apply it; otherwise use attached (follow player look)
            if (state.rotation.x() != 0 || state.rotation.y() != 0) {
                settings.rotationType = RotationType.Custom;
                settings.rotation = degreesToDirection(state.rotation);
                settings.applyLookType = ApplyLookType.Rotation;
            }

            // If custom position was set, use it; otherwise follow player
            if (state.position.x() != 0 || state.position.y() != 0 || state.position.z() != 0) {
                settings.positionType = PositionType.Custom;
                settings.position = new Position(state.position.x(), state.position.y(), state.position.z());
            }

            // Lock camera so player can't rotate it
            sendCameraPacket(playerRef, settings, true);

            // Enable letterbox for cinematic feel
            showLetterboxOverlay(playerRef);
            state.letterboxEnabled = true;

            LOGGER.debug("Enabled cinematic mode for player {} (distance={}, locked=true, letterbox=true)",
                playerId, CINEMATIC_CAMERA_DISTANCE);
        }
    }

    @Override
    public void disableCinematicMode(UUID playerId) {
        InternalCameraState state = getOrCreateState(playerId);

        if (state.cinematicMode) {
            // Restore saved state
            if (state.savedPosition != null) {
                state.position = state.savedPosition;
            }
            if (state.savedRotation != null) {
                state.rotation = state.savedRotation;
            }
            state.distance = state.savedDistance;
            state.cinematicMode = false;
            state.cameraLocked = false;
            state.letterboxEnabled = false;

            PlayerRef playerRef = getPlayerRef(playerId);
            if (playerRef != null) {
                // Reset camera to default by sending null settings
                // Pattern from hytalemodding.dev: new SetServerCamera(Custom, false, null)
                resetCameraToDefault(playerRef);

                // Remove letterbox overlay
                hideLetterboxOverlay(playerId);
            }

            LOGGER.debug("Disabled cinematic mode for player {} (restored defaults)", playerId);
        }
    }

    @Override
    public boolean isInCinematicMode(UUID playerId) {
        return getOrCreateState(playerId).cinematicMode;
    }

    // ========================================================================
    // Extended Camera Control (new SDK-powered methods)
    // ========================================================================

    /**
     * Sets the camera distance from the player (zoom level).
     *
     * <p>Uses {@code ServerCameraSettings.distance} to control how far
     * the third-person camera is from the player. Higher values zoom out.</p>
     *
     * @param playerId the player UUID
     * @param distance camera distance (typical range: 2.0–30.0)
     */
    public void setCameraDistance(UUID playerId, float distance) {
        InternalCameraState state = getOrCreateState(playerId);
        state.distance = distance;

        PlayerRef playerRef = getPlayerRef(playerId);
        if (playerRef == null) {
            return;
        }

        ServerCameraSettings settings = buildBaseSettings(state);
        settings.distance = distance;

        sendCameraPacket(playerRef, settings, state.cameraLocked);
        LOGGER.debug("Set camera distance for player {}: {}", playerId, distance);
    }

    /**
     * Forces first-person or third-person perspective.
     *
     * <p>Uses {@code ClientCameraView.FirstPerson} or {@code ThirdPerson}
     * to switch the player's camera perspective.</p>
     *
     * @param playerId the player UUID
     * @param firstPerson true for first-person, false for third-person
     */
    public void forcePerspective(UUID playerId, boolean firstPerson) {
        InternalCameraState state = getOrCreateState(playerId);
        state.forcedFirstPerson = firstPerson;

        PlayerRef playerRef = getPlayerRef(playerId);
        if (playerRef == null) {
            return;
        }

        ClientCameraView view = firstPerson
            ? ClientCameraView.FirstPerson
            : ClientCameraView.ThirdPerson;

        // Send perspective change with no custom settings (use defaults for that perspective)
        SetServerCamera packet = new SetServerCamera(view, state.cameraLocked, null);
        PacketHandler handler = playerRef.getPacketHandler();
        if (handler != null) {
            handler.writeNoCache(packet);
            LOGGER.debug("Forced {} perspective for player {}",
                firstPerson ? "first-person" : "third-person", playerId);
        }
    }

    /**
     * Locks or unlocks the camera, preventing the player from changing it.
     *
     * <p>Uses the {@code isLocked} field on {@link SetServerCamera} to prevent
     * the player from rotating or zooming the camera.</p>
     *
     * @param playerId the player UUID
     * @param locked true to lock, false to unlock
     */
    public void setCameraLocked(UUID playerId, boolean locked) {
        InternalCameraState state = getOrCreateState(playerId);
        state.cameraLocked = locked;

        PlayerRef playerRef = getPlayerRef(playerId);
        if (playerRef == null) {
            return;
        }

        // Re-send current camera settings with updated lock state
        ServerCameraSettings settings = buildBaseSettings(state);
        sendCameraPacket(playerRef, settings, locked);
        LOGGER.debug("Camera {} for player {}", locked ? "locked" : "unlocked", playerId);
    }

    /**
     * Resets camera to default Hytale camera for the given player.
     *
     * <p>Sends {@code SetServerCamera(Custom, false, null)} which resets
     * to the default client camera behavior as documented in the SDK guide.</p>
     *
     * @param playerId the player UUID
     */
    public void resetCamera(UUID playerId) {
        InternalCameraState state = getOrCreateState(playerId);
        state.cameraLocked = false;
        state.forcedFirstPerson = false;
        state.distance = DEFAULT_CAMERA_DISTANCE;

        PlayerRef playerRef = getPlayerRef(playerId);
        if (playerRef != null) {
            resetCameraToDefault(playerRef);
            LOGGER.debug("Reset camera to default for player {}", playerId);
        }
    }

    // ========================================================================
    // Internal Helpers
    // ========================================================================

    /**
     * Gets a valid PlayerRef from the Universe, or null if player is offline.
     */
    private PlayerRef getPlayerRef(UUID playerId) {
        PlayerRef playerRef = Universe.get().getPlayer(playerId);
        if (playerRef == null || !playerRef.isValid()) {
            LOGGER.debug("Player {} not found or invalid", playerId);
            return null;
        }
        return playerRef;
    }

    /**
     * Builds a {@link ServerCameraSettings} from the current internal state.
     *
     * <p>Sets common fields like lerp speeds, distance, perspective,
     * wall clip prevention. Callers can then override specific fields
     * before sending.</p>
     */
    private ServerCameraSettings buildBaseSettings(InternalCameraState state) {
        ServerCameraSettings settings = new ServerCameraSettings();
        settings.positionLerpSpeed = DEFAULT_POSITION_LERP_SPEED;
        settings.rotationLerpSpeed = DEFAULT_ROTATION_LERP_SPEED;
        settings.distance = state.distance;
        settings.isFirstPerson = state.forcedFirstPerson;
        settings.eyeOffset = true;
        // Use raycast offset to prevent camera clipping through walls
        settings.positionDistanceOffsetType = PositionDistanceOffsetType.DistanceOffsetRaycast;
        return settings;
    }

    /**
     * Sends a {@link SetServerCamera} packet to the player.
     *
     * @param playerRef the player reference
     * @param settings the camera settings to apply
     * @param locked whether the camera should be locked from player changes
     */
    private void sendCameraPacket(PlayerRef playerRef, ServerCameraSettings settings, boolean locked) {
        SetServerCamera packet = new SetServerCamera(ClientCameraView.Custom, locked, settings);
        PacketHandler handler = playerRef.getPacketHandler();
        if (handler != null) {
            handler.writeNoCache(packet);
        } else {
            LOGGER.warn("Cannot send camera packet: no packet handler for player {}", playerRef.getUuid());
        }
    }

    /**
     * Resets camera to default client behavior.
     *
     * <p>Pattern from hytalemodding.dev:
     * {@code new SetServerCamera(ClientCameraView.Custom, false, null)}</p>
     */
    private void resetCameraToDefault(PlayerRef playerRef) {
        SetServerCamera resetPacket = new SetServerCamera(ClientCameraView.Custom, false, null);
        PacketHandler handler = playerRef.getPacketHandler();
        if (handler != null) {
            handler.writeNoCache(resetPacket);
        }
    }

    /**
     * Converts a framework {@code Vector2(pitch, yaw)} in degrees to an SDK
     * {@link Direction}(yaw, pitch, roll) in radians.
     *
     * <p>The SDK's {@code Direction} uses radians with fields ordered
     * {@code (yaw, pitch, roll)}, while the framework uses degrees with
     * fields ordered {@code (pitch, yaw)}.</p>
     */
    private Direction degreesToDirection(Vector2 rotation) {
        float pitchRadians = (float) Math.toRadians(rotation.x());
        float yawRadians = (float) Math.toRadians(rotation.y());
        return new Direction(yawRadians, pitchRadians, 0.0f);
    }

    /**
     * Applies the full camera state to a player via SDK.
     *
     * <p>Called internally when {@link #setCameraState} is used to apply
     * position, rotation, and distance in a single packet.</p>
     */
    private void applyCameraSettings(PlayerRef playerRef, InternalCameraState state) {
        ServerCameraSettings settings = buildBaseSettings(state);

        // Apply custom position if non-zero
        if (state.position.x() != 0 || state.position.y() != 0 || state.position.z() != 0) {
            settings.positionType = PositionType.Custom;
            settings.position = new Position(state.position.x(), state.position.y(), state.position.z());
        }

        // Apply custom rotation if non-zero
        if (state.rotation.x() != 0 || state.rotation.y() != 0) {
            settings.rotationType = RotationType.Custom;
            settings.rotation = degreesToDirection(state.rotation);
            settings.applyLookType = ApplyLookType.Rotation;
        }

        sendCameraPacket(playerRef, settings, state.cameraLocked);
    }

    private InternalCameraState getOrCreateState(UUID playerId) {
        return playerCameraStates.computeIfAbsent(playerId, id -> new InternalCameraState());
    }

    /**
     * Cleans up all camera state for a player (call on disconnect).
     *
     * <p>Removes internal state tracking and any active letterbox overlay.</p>
     *
     * @param playerId The player UUID
     */
    public void cleanupPlayer(UUID playerId) {
        playerCameraStates.remove(playerId);
        hideLetterboxOverlay(playerId);
        LOGGER.debug("Cleaned up camera state for player {}", playerId);
    }

    // ========================================================================
    // Letterbox Overlay (HyUI Workaround) — CAM-003
    // ========================================================================

    /**
     * Shows a letterbox overlay HUD for the player using HyUI.
     *
     * <p>Since the Hytale SDK has no native letterbox API, this workaround
     * renders two opaque black bars at the top and bottom of the screen
     * via a HyUI HUD overlay, creating a cinematic letterbox effect.</p>
     *
     * @param playerRef the player to show the letterbox to
     */
    private void showLetterboxOverlay(PlayerRef playerRef) {
        UUID playerId = playerRef.getUuid();

        // Don't create duplicates
        if (letterboxHuds.containsKey(playerId)) {
            return;
        }

        try {
            HyUIHud hud = HudBuilder.hudForPlayer(playerRef)
                .fromHtml(LETTERBOX_TEMPLATE)
                .show();

            letterboxHuds.put(playerId, hud);
            LOGGER.debug("Showing letterbox overlay for player {}", playerId);
        } catch (Exception e) {
            LOGGER.error("Failed to show letterbox overlay for player {}: {}",
                playerId, e.getMessage(), e);
        }
    }

    /**
     * Hides the letterbox overlay HUD for the player.
     *
     * @param playerId the player UUID
     */
    private void hideLetterboxOverlay(UUID playerId) {
        HyUIHud hud = letterboxHuds.remove(playerId);
        if (hud != null) {
            try {
                hud.remove();
                LOGGER.debug("Hidden letterbox overlay for player {}", playerId);
            } catch (Exception e) {
                LOGGER.error("Failed to hide letterbox overlay for player {}: {}",
                    playerId, e.getMessage(), e);
            }
        }
    }

    // ========================================================================
    // Internal State Class
    // ========================================================================

    private static class InternalCameraState {
        Vector3 position = new Vector3(0, 0, 0);
        Vector2 rotation = new Vector2(0, 0);
        float fov = DEFAULT_FOV;
        float distance = DEFAULT_CAMERA_DISTANCE;
        boolean cinematicMode = false;
        boolean letterboxEnabled = false;
        boolean cameraLocked = false;
        boolean forcedFirstPerson = false;
        float dofFocalDistance = DEFAULT_DOF_DISTANCE;
        float dofAperture = DEFAULT_DOF_APERTURE;

        // Shake state tracking
        float shakeIntensity = 0;
        long shakeDurationMs = 0;
        long shakeStartTime = 0;

        // Saved state for cinematic mode restoration
        Vector3 savedPosition;
        Vector2 savedRotation;
        float savedDistance = DEFAULT_CAMERA_DISTANCE;

        /**
         * Checks if shake effect is currently active.
         */
        boolean isShakeActive() {
            if (shakeIntensity <= 0 || shakeDurationMs <= 0) {
                return false;
            }
            return System.currentTimeMillis() - shakeStartTime < shakeDurationMs;
        }
    }
}
