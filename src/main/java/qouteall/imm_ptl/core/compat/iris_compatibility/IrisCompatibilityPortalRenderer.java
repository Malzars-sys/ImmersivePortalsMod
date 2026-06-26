package qouteall.imm_ptl.core.compat.iris_compatibility;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import org.joml.Matrix4f;
import qouteall.imm_ptl.core.portal.Portal;
import qouteall.imm_ptl.core.render.context_management.WorldRenderInfo;
import qouteall.imm_ptl.core.render.renderer.PortalRenderer;

/**
 * Phase 7.0 compile-only facade.
 * The legacy Iris runtime renderer still depends on the old immediate
 * framebuffer/stencil path and is intentionally disabled until Iris runtime work.
 */
public class IrisCompatibilityPortalRenderer extends PortalRenderer {
    public static final IrisCompatibilityPortalRenderer instance =
        new IrisCompatibilityPortalRenderer(false);
    public static final IrisCompatibilityPortalRenderer debugModeInstance =
        new IrisCompatibilityPortalRenderer(true);

    public final boolean isDebugMode;

    public IrisCompatibilityPortalRenderer(boolean isDebugMode) {
        this.isDebugMode = isDebugMode;
    }

    @Override
    public boolean replaceFrameBufferClearing() {
        return false;
    }

    @Override
    public void prepareRendering() {}

    @Override
    public void finishRendering() {}

    @Override
    public void onBeforeTranslucentRendering(Matrix4f modelView) {}

    @Override
    public void onAfterTranslucentRendering(Matrix4f modelView) {}

    @Override
    public void onHandRenderingEnded() {}

    @Override
    public void invokeWorldRendering(WorldRenderInfo worldRenderInfo) {
        super.invokeWorldRendering(worldRenderInfo);
    }

    @Override
    public void renderPortalInEntityRenderer(Portal portal) {}

    @Override
    public void renderPortalInEntityRenderer(
        Portal portal,
        PoseStack poseStack,
        SubmitNodeCollector submitNodeCollector
    ) {}
}
