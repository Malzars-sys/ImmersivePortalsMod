package qouteall.imm_ptl.core.render.renderer;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.opengl.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Screenshot;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import qouteall.imm_ptl.core.CHelper;
import qouteall.imm_ptl.core.compat.IPPortingLibCompat;
import qouteall.imm_ptl.core.ducks.IECamera;
import qouteall.imm_ptl.core.ducks.IEMinecraftClient;
import qouteall.imm_ptl.core.ducks.IEGameRenderer;
import qouteall.imm_ptl.core.ducks.IEParticleManager;
import qouteall.imm_ptl.core.ducks.IEWorldRenderer;
import qouteall.imm_ptl.core.portal.Portal;
import qouteall.imm_ptl.core.render.FrontClipping;
import qouteall.imm_ptl.core.render.MyGameRenderer;
import qouteall.imm_ptl.core.render.MyRenderHelper;
import qouteall.imm_ptl.core.render.QueryManager;
import qouteall.imm_ptl.core.render.SecondaryFrameBuffer;
import qouteall.imm_ptl.core.render.ViewAreaRenderer;
import qouteall.imm_ptl.core.render.context_management.PortalRendering;
import qouteall.imm_ptl.core.render.context_management.RenderStates;
import qouteall.imm_ptl.core.render.pipeline.IPRenderPipelines;
import qouteall.q_misc_util.my_util.LimitedLogger;

import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class RendererUsingFrameBuffer extends PortalRenderer {
    private static final Logger LOGGER = LoggerFactory.getLogger(RendererUsingFrameBuffer.class);
    SecondaryFrameBuffer secondaryFrameBuffer = new SecondaryFrameBuffer();
    private static final LimitedLogger LIMITED_LOGGER = new LimitedLogger(10);
    private static boolean loggedGameRendererMixinStatus;
    private static boolean loggedMissingLightmapFallback;
    private static boolean renderingQueuedPortal;
    private static boolean framebufferBlitUnavailable;
    private static boolean loggedFramebufferBlitAttempt;
    private static boolean loggedFramebufferBlitSuccess;
    private static boolean loggedQueuedMinimalPortal;
    private static boolean loggedMinimalRecursiveRenderStarted;
    private static boolean capturedMinimalRecursivePortalScreenshot;
    private static boolean loggedSubmitNodeStart;
    private static boolean loggedInvalidScreenshotFilename;
    private static boolean loggedFramebufferTextureAvailable;
    private static boolean loggedSubmitNodeQuadSuccess;
    private static boolean loggedSubmitNodeQuadFallback;
    private static boolean loggedMinimalMaskAvailability;
    private static boolean loggedMinimalMaskAttempt;
    private static boolean loggedMinimalMaskApplied;
    private static boolean loggedMinimalMaskFallback;
    private static boolean pendingMinimalRecursivePortalScreenshot;
    private static long minimalRecursivePortalScreenshotRequestedAt;
    private final List<Portal> queuedMinimalPortals = new ArrayList<>();
    private int lastQueuedFrame = -1;
    private Portal renderedMinimalPortal;
    private int renderedMinimalPortalFrame = -1;

    public void queueMinimalPortalFromEntityRenderer(Portal portal) {
        if (
            portal.getDestDim() != client.level.dimension() ||
            PortalRendering.isRendering()
        ) {
            return;
        }
        if (lastQueuedFrame != RenderStates.frameIndex) {
            lastQueuedFrame = RenderStates.frameIndex;
            queuedMinimalPortals.clear();
        }
        if (queuedMinimalPortals.isEmpty()) {
            queuedMinimalPortals.add(portal);
            if (!loggedQueuedMinimalPortal) {
                loggedQueuedMinimalPortal = true;
                LOGGER.info("Queued minimal recursive portal from PortalEntityRenderer: {}", portal);
            }
        }
    }

    public void renderQueuedMinimalPortalsFromGameRendererHook() {
        if (
            renderingQueuedPortal ||
            PortalRendering.isRendering() ||
            queuedMinimalPortals.isEmpty()
        ) {
            return;
        }
        if (!loggedGameRendererMixinStatus) {
            loggedGameRendererMixinStatus = true;
            LOGGER.info(
                "IEGameRenderer active in vanilla profile: {}",
                client.gameRenderer instanceof IEGameRenderer
            );
            LOGGER.info(
                "IECamera active in vanilla profile: {}",
                client.gameRenderer.getMainCamera() instanceof IECamera
            );
            LOGGER.info(
                "IEWorldRenderer active in vanilla profile: {}",
                client.levelRenderer instanceof IEWorldRenderer
            );
            LOGGER.info(
                "IEParticleManager active in vanilla profile: {}",
                client.particleEngine instanceof IEParticleManager
            );
        }
        if (!(client.gameRenderer instanceof IEGameRenderer)) {
            LIMITED_LOGGER.log(
                "Minimal recursive portal rendering fallback: MixinGameRenderer is isolated in the vanilla profile"
            );
            return;
        }
        if (!(client.gameRenderer.getMainCamera() instanceof IECamera)) {
            LIMITED_LOGGER.log(
                "Minimal recursive portal rendering fallback: MixinCamera is isolated in the vanilla profile"
            );
            return;
        }
        if (!(client.levelRenderer instanceof IEWorldRenderer)) {
            LIMITED_LOGGER.log(
                "Minimal recursive portal rendering fallback: MixinLevelRenderer is isolated in the vanilla profile"
            );
            return;
        }
        if (!(client.particleEngine instanceof IEParticleManager)) {
            LIMITED_LOGGER.log(
                "Minimal recursive portal rendering fallback: MixinParticleEngine is isolated in the vanilla profile"
            );
            return;
        }
        if (((IEGameRenderer) client.gameRenderer).ip_getLightmap() == null) {
            if (!loggedMissingLightmapFallback) {
                loggedMissingLightmapFallback = true;
                LOGGER.info("Minimal recursive portal rendering fallback: GameRenderer lightmap is unavailable");
            }
            return;
        }
        if (RenderStates.originalCamera == null) {
            RenderStates.originalCamera = client.gameRenderer.getMainCamera();
        }
        Portal portal = queuedMinimalPortals.remove(0);
        if (!loggedMinimalRecursiveRenderStarted) {
            loggedMinimalRecursiveRenderStarted = true;
            LOGGER.info(
                "Rendering minimal recursive portal from GameRenderer renderLevel hook: {}",
                portal
            );
        }
        renderingQueuedPortal = true;
        try {
            secondaryFrameBuffer.prepare();
            if (doRenderPortal(portal, new Matrix4f(), false)) {
                renderedMinimalPortal = portal;
                renderedMinimalPortalFrame = RenderStates.frameIndex;
            }
        }
        finally {
            renderingQueuedPortal = false;
        }
    }

    public void capturePendingMinimalRecursivePortalScreenshot() {
        if (!pendingMinimalRecursivePortalScreenshot) {
            return;
        }
        if (!"true".equalsIgnoreCase(System.getenv("IMM_PTL_CAPTURE_MINIMAL_RECURSIVE_PORTAL"))) {
            pendingMinimalRecursivePortalScreenshot = false;
            return;
        }
        long now = System.nanoTime();
        if (minimalRecursivePortalScreenshotRequestedAt == 0) {
            minimalRecursivePortalScreenshotRequestedAt = now;
            return;
        }
        if (now - minimalRecursivePortalScreenshotRequestedAt < 3_000_000_000L) {
            return;
        }
        pendingMinimalRecursivePortalScreenshot = false;
        captureMinimalRecursivePortalScreenshot();
    }

    @Override
    public void onBeforeTranslucentRendering(Matrix4f modelView) {
        renderPortals(modelView);
    }
    
    @Override
    public void onAfterTranslucentRendering(Matrix4f modelView) {
    
    }
    
    @Override
    public void onHandRenderingEnded() {
    
    }
    
    @Override
    public void finishRendering() {
    
    }
    
    @Override
    public void prepareRendering() {
        secondaryFrameBuffer.prepare();
        
        GlStateManager._enableDepthTest();
        
        GL11.glDisable(GL11.GL_STENCIL_TEST);
    
        IPPortingLibCompat.setIsStencilEnabled(client.getMainRenderTarget(), false);
//        ((IEFrameBuffer) client.getMainRenderTarget()).setIsStencilBufferEnabledAndReload(false);
    }
    
    protected void doRenderPortal(
        Portal portal,
        Matrix4f modelView
    ) {
        doRenderPortal(portal, modelView, true);
    }

    protected boolean doRenderPortal(
        Portal portal,
        Matrix4f modelView,
        boolean blitImmediately
    ) {
        if (PortalRendering.isRendering()) {
            //only support one-layer portal
            return false;
        }
        
        if (portal.getDestDim() != client.level.dimension()) {
            return false;
        }

        if (!testShouldRenderPortal(portal, modelView)) {
            return false;
        }
        
        PortalRendering.pushPortalLayer(portal);
        
        RenderTarget oldFrameBuffer = client.getMainRenderTarget();

        try {
            ((IEMinecraftClient) client).ip_setFrameBuffer(secondaryFrameBuffer.fb);
            IPRenderPipelines.clearRenderTarget(secondaryFrameBuffer.fb, 0xFF101020, 1.0);
            GL11.glDisable(GL11.GL_STENCIL_TEST);

            if (qouteall.imm_ptl.core.IPCGlobal.useMinimalRecursivePortalRendering) {
                FrontClipping.beginMinimalCpuClipping();
            }
            renderPortalContent(portal);
        }
        finally {
            FrontClipping.endMinimalCpuClipping();
            ((IEMinecraftClient) client).ip_setFrameBuffer(oldFrameBuffer);
            PortalRendering.popPortalLayer();
        }

        if (MyGameRenderer.lastMinimalRecursiveRenderSucceeded) {
            if (blitImmediately) {
                return tryBlitSecondBufferIntoMainBuffer(portal, modelView);
            }
            return true;
        }

        return false;
    }

    private void captureMinimalRecursivePortalScreenshot() {
        if (
            capturedMinimalRecursivePortalScreenshot ||
            !"true".equalsIgnoreCase(System.getenv("IMM_PTL_CAPTURE_MINIMAL_RECURSIVE_PORTAL"))
        ) {
            return;
        }

        capturedMinimalRecursivePortalScreenshot = true;
        try {
            Screenshot.grab(
                client.gameDirectory,
                getMinimalRecursivePortalScreenshotFilename(),
                client.getMainRenderTarget(),
                1,
                component -> LOGGER.info(
                    "Captured minimal recursive portal screenshot: {}",
                    component.getString()
                )
            );
        }
        catch (Throwable throwable) {
            LIMITED_LOGGER.lErr(
                LOGGER,
                "Failed to capture minimal recursive portal screenshot",
                throwable
            );
        }
    }

    private String getMinimalRecursivePortalScreenshotFilename() {
        String defaultFilename = "imm_ptl-minimal-recursive-portal.png";
        String configuredName = System.getenv("IMM_PTL_MINIMAL_RECURSIVE_PORTAL_SCREENSHOT");
        if (configuredName == null || configuredName.isBlank()) {
            return defaultFilename;
        }
        try {
            String filename = Path.of(configuredName).getFileName().toString();
            return filename.isBlank() ? defaultFilename : filename;
        }
        catch (InvalidPathException exception) {
            if (!loggedInvalidScreenshotFilename) {
                loggedInvalidScreenshotFilename = true;
                LOGGER.info(
                    "Invalid minimal portal screenshot filename; using {}",
                    defaultFilename
                );
            }
            return defaultFilename;
        }
    }
    
    @Override
    public void renderPortalInEntityRenderer(Portal portal) {
        renderPortalInEntityRenderer(portal, null);
    }

    @Override
    public void renderPortalInEntityRenderer(Portal portal, PoseStack poseStack) {
        renderPortalInEntityRenderer(portal, poseStack, null);
    }

    @Override
    public void renderPortalInEntityRenderer(
        Portal portal,
        PoseStack poseStack,
        SubmitNodeCollector submitNodeCollector
    ) {
        if (
            !qouteall.imm_ptl.core.IPCGlobal.useMinimalRecursivePortalRendering ||
            renderedMinimalPortal == null ||
            renderedMinimalPortal.getId() != portal.getId() ||
            Math.abs(RenderStates.frameIndex - renderedMinimalPortalFrame) > 1 ||
            poseStack == null ||
            submitNodeCollector == null
        ) {
            return;
        }

        if (!loggedSubmitNodeStart) {
            loggedSubmitNodeStart = true;
            LOGGER.info("Submitting minimal recursive portal framebuffer from PortalEntityRenderer");
        }

        boolean mainDepthAvailable = client.getMainRenderTarget().getDepthTextureView() != null;
        boolean secondaryDepthAvailable = secondaryFrameBuffer.fb.getDepthTextureView() != null;
        if (!loggedMinimalMaskAvailability) {
            loggedMinimalMaskAvailability = true;
            LOGGER.info(
                "Minimal portal mask attachments: main depth={}, secondary depth={}, usable stencil=false " +
                    "(RenderPipeline DepthStencilState exposes depth only)",
                mainDepthAvailable,
                secondaryDepthAvailable
            );
        }

        if (!loggedMinimalMaskAttempt) {
            loggedMinimalMaskAttempt = true;
            LOGGER.info("Minimal portal depth mask attempted: true");
        }

        RenderType depthMaskRenderType = IPRenderPipelines.getMinimalPortalDepthMaskRenderType();
        RenderType maskedFramebufferRenderType =
            IPRenderPipelines.getMinimalPortalMaskedFramebufferRenderType(secondaryFrameBuffer.fb);
        boolean useDepthMask = depthMaskRenderType != null && maskedFramebufferRenderType != null;
        RenderType renderType = useDepthMask
            ? maskedFramebufferRenderType
            : IPRenderPipelines.getMinimalPortalFramebufferRenderType(secondaryFrameBuffer.fb);
        if (renderType == null) {
            if (!loggedSubmitNodeQuadFallback) {
                loggedSubmitNodeQuadFallback = true;
                LOGGER.info(
                    "Minimal recursive portal SubmitNodeCollector fallback: framebuffer texture unavailable"
                );
            }
            return;
        }

        if (!loggedFramebufferTextureAvailable) {
            loggedFramebufferTextureAvailable = true;
            LOGGER.info(
                "Minimal recursive portal framebuffer texture available: true ({}x{}, portal {})",
                secondaryFrameBuffer.fb.width,
                secondaryFrameBuffer.fb.height,
                portal.getId()
            );
        }

        try {
            if (useDepthMask) {
                MyRenderHelper.submitPortalDepthMask(
                    portal,
                    poseStack,
                    submitNodeCollector.order(0),
                    depthMaskRenderType
                );
                if (!loggedMinimalMaskApplied) {
                    loggedMinimalMaskApplied = true;
                    LOGGER.info(
                        "Minimal portal depth mask applied: true " +
                            "(depth-only rectangle followed by EQUAL textured pass)"
                    );
                }
            }
            else if (!loggedMinimalMaskFallback) {
                loggedMinimalMaskFallback = true;
                LOGGER.info(
                    "Minimal portal depth mask fallback: required depth attachment or pipeline unavailable"
                );
            }
            MyRenderHelper.submitPortalAreaWithFramebuffer(
                portal,
                poseStack,
                useDepthMask ? submitNodeCollector.order(1) : submitNodeCollector,
                renderType
            );
            pendingMinimalRecursivePortalScreenshot = true;
            if (!loggedSubmitNodeQuadSuccess) {
                loggedSubmitNodeQuadSuccess = true;
                LOGGER.info(
                    "Minimal recursive portal textured quad submitted via SubmitNodeCollector: true (portal {})",
                    portal.getId()
                );
            }
        }
        catch (Throwable throwable) {
            if (!loggedSubmitNodeQuadFallback) {
                loggedSubmitNodeQuadFallback = true;
                LIMITED_LOGGER.lErr(
                    LOGGER,
                    "Minimal recursive portal SubmitNodeCollector submission failed; using cyan frame fallback",
                    throwable
                );
            }
        }
    }
    
    @Override
    public boolean replaceFrameBufferClearing() {
        return false;
    }
    
    private boolean testShouldRenderPortal(
        Portal portal,
        Matrix4f modelView
    ) {
        if (qouteall.imm_ptl.core.IPCGlobal.useMinimalRecursivePortalRendering) {
            return true;
        }
        FrontClipping.updateInnerClipping(modelView);
        return QueryManager.renderAndGetDoesAnySamplePass(() -> {
            ViewAreaRenderer.renderPortalArea(
                portal, Vec3.ZERO,
                modelView,
                getProjectionMatrix(),
                true, true,
                true, true
            );
        });
    }
    
    private void renderSecondBufferIntoMainBuffer(Portal portal, Matrix4f modelView) {
        MyRenderHelper.drawPortalAreaWithFramebuffer(
            portal,
            secondaryFrameBuffer.fb,
            modelView,
            getProjectionMatrix()
        );
    }

    private boolean tryBlitSecondBufferIntoMainBuffer(
        Portal portal,
        Matrix4f modelView
    ) {
        try {
            CHelper.enableDepthClamp();
            if (!loggedFramebufferBlitAttempt) {
                loggedFramebufferBlitAttempt = true;
                LOGGER.info("Attempting minimal recursive portal framebuffer blit");
            }
            renderSecondBufferIntoMainBuffer(portal, modelView);
            if (!loggedFramebufferBlitSuccess) {
                loggedFramebufferBlitSuccess = true;
                LOGGER.info("Minimal recursive portal framebuffer blit succeeded");
            }
            captureMinimalRecursivePortalScreenshot();
            MyRenderHelper.debugFramebufferDepth();
            return true;
        }
        catch (Throwable throwable) {
            MyGameRenderer.lastMinimalRecursiveRenderSucceeded = false;
            framebufferBlitUnavailable = true;
            LIMITED_LOGGER.lErr(
                LOGGER,
                "Minimal recursive portal framebuffer blit failed; using cyan frame fallback",
                throwable
            );
            return false;
        }
        finally {
            CHelper.disableDepthClamp();
        }
    }
    
    protected void renderPortals(Matrix4f modelView) {
        List<Portal> portalsToRender = getPortalsToRender(modelView);
    
        for (Portal portal : portalsToRender) {
            doRenderPortal(portal, modelView);
        }
    }
}
