package qouteall.imm_ptl.core.compat.iris_compatibility;

import com.mojang.blaze3d.pipeline.RenderTarget;

/**
 * Phase 7.0 compile-only helpers.
 * The old Iris path copied raw GL texture ids that are no longer exposed by
 * Minecraft 26.1. Runtime Iris copy semantics will be rebuilt in a later phase.
 */
public class IPIrisHelper {
    public static void copyDepthStencil(
        RenderTarget from,
        RenderTarget to,
        boolean copyDepth,
        boolean copyStencil
    ) {
        if (copyDepth) {
            to.copyDepthFrom(from);
        }
    }

    public static void newCopyDepthStencil(RenderTarget from, RenderTarget to) {
        to.copyDepthFrom(from);
    }

    public static void copyColor(RenderTarget from, RenderTarget to) {
        // Intentionally no-op in the Iris compile-only profile.
    }
}
