package qouteall.imm_ptl.api;

import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Stable public handle for a portal created or tracked through the public API.
 * Do not store raw Portal entity references across ticks or save/reload.
 */
public record PortalHandle(
    UUID entityId,
    ResourceKey<Level> dimension,
    Identifier owner,
    @Nullable String sourceAnchorId,
    @Nullable String targetAnchorId
) {}
