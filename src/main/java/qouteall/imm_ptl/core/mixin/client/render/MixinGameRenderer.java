package qouteall.imm_ptl.core.mixin.client.render;

import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.Lightmap;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import qouteall.imm_ptl.core.IPCGlobal;
import qouteall.imm_ptl.core.ducks.IEGameRenderer;
import qouteall.imm_ptl.core.render.context_management.PortalRendering;

/**
 * Minimal vanilla-profile bridge used by the controlled recursive-rendering path.
 *
 * <p>The legacy render injections intentionally remain isolated until the
 * corresponding 26.1 rendering stages are migrated.</p>
 */
@Mixin(GameRenderer.class)
public abstract class MixinGameRenderer implements IEGameRenderer {
    @Shadow
    @Final
    @Mutable
    private Lightmap lightmap;

    @Shadow
    @Final
    @Mutable
    private Camera mainCamera;

    @Inject(method = "renderLevel", at = @At("HEAD"))
    private void ip_renderQueuedMinimalPortal(DeltaTracker deltaTracker, CallbackInfo ci) {
        if (
            IPCGlobal.useMinimalRecursivePortalRendering &&
            IPCGlobal.rendererUsingFrameBuffer != null &&
            !PortalRendering.isRendering()
        ) {
            IPCGlobal.rendererUsingFrameBuffer.renderQueuedMinimalPortalsFromGameRendererHook();
        }
    }

    @Inject(method = "renderLevel", at = @At("TAIL"))
    private void ip_captureMinimalPortalFrame(DeltaTracker deltaTracker, CallbackInfo ci) {
        if (
            IPCGlobal.useMinimalRecursivePortalRendering &&
            IPCGlobal.rendererUsingFrameBuffer != null &&
            !PortalRendering.isRendering()
        ) {
            IPCGlobal.rendererUsingFrameBuffer.capturePendingMinimalRecursivePortalScreenshot();
        }
    }

    @Override
    public Lightmap ip_getLightmap() {
        return lightmap;
    }

    @Override
    public void ip_setLightmapTextureManager(Lightmap manager) {
        lightmap = manager;
    }

    @Override
    public boolean ip_getDoRenderHand() {
        return false;
    }

    @Override
    public void ip_setCamera(Camera camera) {
        mainCamera = camera;
    }

    @Override
    public void ip_setIsRenderingPanorama(boolean condition) {
        // The 26.1 vanilla renderer no longer exposes the legacy panorama flag.
    }
}
