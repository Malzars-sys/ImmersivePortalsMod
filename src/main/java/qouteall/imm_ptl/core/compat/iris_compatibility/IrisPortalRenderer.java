package qouteall.imm_ptl.core.compat.iris_compatibility;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import org.joml.Matrix4f;
import qouteall.imm_ptl.core.portal.Portal;
import qouteall.imm_ptl.core.render.context_management.WorldRenderInfo;
import qouteall.imm_ptl.core.render.renderer.PortalRenderer;

/**
 * Phase 7.0 compile-only facade for the old Iris deferred renderer.
 * Runtime Iris rendering remains disabled; this class only keeps the source set
 * compilable while the dedicated Iris profile is opened safely.
 */
public class IrisPortalRenderer extends PortalRenderer {
    public static final IrisPortalRenderer instance = new IrisPortalRenderer();

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
