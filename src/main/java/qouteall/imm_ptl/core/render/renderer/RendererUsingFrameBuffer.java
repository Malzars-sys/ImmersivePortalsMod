package qouteall.imm_ptl.core.render.renderer;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.opengl.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Screenshot;
import net.minecraft.client.renderer.OrderedSubmitNodeCollector;
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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

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
    private static boolean loggedNoDepthMaskExperiment;
    private static boolean loggedFramebufferPipelineMode;
    private static boolean loggedFramebufferOrderMode;
    private static boolean loggedActiveShaderpack;
    private static boolean pendingMinimalRecursivePortalScreenshot;
    private static long minimalRecursivePortalScreenshotRequestedAt;
    private static final boolean FORCE_FRAMEBUFFER_NO_DEPTH_MASK =
        "true".equalsIgnoreCase(System.getenv("IMM_PTL_FORCE_FRAMEBUFFER_NO_DEPTH_MASK"));
    private static final FramebufferDepthModeSelection FRAMEBUFFER_DEPTH_MODE_SELECTION =
        resolveFramebufferDepthMode();
    private static final FramebufferDepthMode FRAMEBUFFER_DEPTH_MODE =
        FRAMEBUFFER_DEPTH_MODE_SELECTION.mode();
    private static final FramebufferOrderModeSelection FRAMEBUFFER_ORDER_MODE_SELECTION =
        resolveFramebufferOrderMode();
    private static final FramebufferOrderMode FRAMEBUFFER_ORDER_MODE =
        FRAMEBUFFER_ORDER_MODE_SELECTION.mode();
    private final List<Portal> queuedMinimalPortals = new ArrayList<>();
    private int lastQueuedFrame = -1;
    private Portal renderedMinimalPortal;
    private int renderedMinimalPortalFrame = -1;

    private enum FramebufferDepthMode {
        DEFAULT("default", true, "EQUAL", "depth-masked-equal"),
        NO_DEPTH("no_depth", false, "none", "non-depth-masked"),
        LEQUAL("lequal", true, "LEQUAL", "depth-masked-lequal"),
        ALWAYS("always", true, "ALWAYS", "depth-masked-always");

        private final String id;
        private final boolean usesDepthMask;
        private final String depthTestName;
        private final String pipelineModeName;

        FramebufferDepthMode(
            String id,
            boolean usesDepthMask,
            String depthTestName,
            String pipelineModeName
        ) {
            this.id = id;
            this.usesDepthMask = usesDepthMask;
            this.depthTestName = depthTestName;
            this.pipelineModeName = pipelineModeName;
        }

        private RenderType getFramebufferRenderType(RenderTarget framebuffer) {
            return switch (this) {
                case DEFAULT -> IPRenderPipelines.getMinimalPortalMaskedFramebufferRenderType(framebuffer);
                case NO_DEPTH -> IPRenderPipelines.getMinimalPortalFramebufferRenderType(framebuffer);
                case LEQUAL -> IPRenderPipelines.getMinimalPortalLequalFramebufferRenderType(framebuffer);
                case ALWAYS -> IPRenderPipelines.getMinimalPortalAlwaysFramebufferRenderType(framebuffer);
            };
        }
    }

    private record FramebufferDepthModeSelection(
        FramebufferDepthMode mode,
        String source,
        String invalidValue
    ) {}

    private enum FramebufferOrderMode {
        DEFAULT("default", true, false, "current depth-mask-before-quad order"),
        MASK_FIRST_EXPLICIT("mask_first_explicit", true, false, "explicit depth-mask-before-quad order"),
        QUAD_FIRST("quad_first", true, true, "textured quad submitted before depth mask"),
        NO_MASK_REFERENCE("no_mask_reference", false, false, "textured quad submitted without depth mask");

        private final String id;
        private final boolean allowDepthMask;
        private final boolean quadFirst;
        private final String description;

        FramebufferOrderMode(
            String id,
            boolean allowDepthMask,
            boolean quadFirst,
            String description
        ) {
            this.id = id;
            this.allowDepthMask = allowDepthMask;
            this.quadFirst = quadFirst;
            this.description = description;
        }
    }

    private record FramebufferOrderModeSelection(
        FramebufferOrderMode mode,
        String source,
        String invalidValue
    ) {}

    private static FramebufferDepthModeSelection resolveFramebufferDepthMode() {
        String rawMode = System.getenv("IMM_PTL_FRAMEBUFFER_DEPTH_MODE");
        if (rawMode == null || rawMode.isBlank()) {
            if (FORCE_FRAMEBUFFER_NO_DEPTH_MASK) {
                return new FramebufferDepthModeSelection(
                    FramebufferDepthMode.NO_DEPTH,
                    "IMM_PTL_FORCE_FRAMEBUFFER_NO_DEPTH_MASK",
                    null
                );
            }
            return new FramebufferDepthModeSelection(
                FramebufferDepthMode.DEFAULT,
                "default implicit",
                null
            );
        }

        String normalized = rawMode.trim().toLowerCase(Locale.ROOT).replace('-', '_');
        FramebufferDepthMode mode = switch (normalized) {
            case "default", "depth_mask", "depth_masked", "equal" -> FramebufferDepthMode.DEFAULT;
            case "no_depth", "none", "disabled", "off" -> FramebufferDepthMode.NO_DEPTH;
            case "lequal", "less_equal", "less_or_equal" -> FramebufferDepthMode.LEQUAL;
            case "always", "always_depth" -> FramebufferDepthMode.ALWAYS;
            default -> null;
        };

        if (mode != null) {
            return new FramebufferDepthModeSelection(
                mode,
                "IMM_PTL_FRAMEBUFFER_DEPTH_MODE",
                null
            );
        }

        return new FramebufferDepthModeSelection(
            FramebufferDepthMode.DEFAULT,
            "invalid IMM_PTL_FRAMEBUFFER_DEPTH_MODE fallback",
            rawMode
        );
    }

    private static FramebufferOrderModeSelection resolveFramebufferOrderMode() {
        String rawMode = System.getenv("IMM_PTL_FRAMEBUFFER_ORDER_MODE");
        if (rawMode == null || rawMode.isBlank()) {
            return new FramebufferOrderModeSelection(
                FramebufferOrderMode.DEFAULT,
                "default implicit",
                null
            );
        }

        String normalized = rawMode.trim().toLowerCase(Locale.ROOT).replace('-', '_');
        FramebufferOrderMode mode = switch (normalized) {
            case "default" -> FramebufferOrderMode.DEFAULT;
            case "mask_first", "mask_first_explicit", "depth_first" ->
                FramebufferOrderMode.MASK_FIRST_EXPLICIT;
            case "quad_first", "framebuffer_first", "texture_first" ->
                FramebufferOrderMode.QUAD_FIRST;
            case "no_mask", "no_mask_reference", "quad_only" ->
                FramebufferOrderMode.NO_MASK_REFERENCE;
            default -> null;
        };

        if (mode != null) {
            return new FramebufferOrderModeSelection(
                mode,
                "IMM_PTL_FRAMEBUFFER_ORDER_MODE",
                null
            );
        }

        return new FramebufferOrderModeSelection(
            FramebufferOrderMode.DEFAULT,
            "invalid IMM_PTL_FRAMEBUFFER_ORDER_MODE fallback",
            rawMode
        );
    }

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

        if (!loggedNoDepthMaskExperiment) {
            loggedNoDepthMaskExperiment = true;
            LOGGER.info(
                "Minimal recursive portal legacy no-depth-mask experiment active: {}",
                FORCE_FRAMEBUFFER_NO_DEPTH_MASK
            );
            if (FRAMEBUFFER_DEPTH_MODE_SELECTION.invalidValue() != null) {
                LOGGER.warn(
                    "Unknown IMM_PTL_FRAMEBUFFER_DEPTH_MODE '{}'; falling back to default depth-masked-equal",
                    FRAMEBUFFER_DEPTH_MODE_SELECTION.invalidValue()
                );
            }
            LOGGER.info(
                "Minimal recursive portal framebuffer depth mode: {} (depth test: {}, source: {})",
                FRAMEBUFFER_DEPTH_MODE.id,
                FRAMEBUFFER_DEPTH_MODE.depthTestName,
                FRAMEBUFFER_DEPTH_MODE_SELECTION.source()
            );
            if (FRAMEBUFFER_DEPTH_MODE == FramebufferDepthMode.NO_DEPTH) {
                LOGGER.warn(
                    "Minimal recursive portal no_depth is a compatibility fallback and may reduce portal occlusion."
                );
            }
        }

        if (!loggedFramebufferOrderMode) {
            loggedFramebufferOrderMode = true;
            if (FRAMEBUFFER_ORDER_MODE_SELECTION.invalidValue() != null) {
                LOGGER.warn(
                    "Unknown IMM_PTL_FRAMEBUFFER_ORDER_MODE '{}'; falling back to default mask-first order",
                    FRAMEBUFFER_ORDER_MODE_SELECTION.invalidValue()
                );
            }
            LOGGER.info(
                "Minimal recursive portal framebuffer order mode: {} ({}, source: {})",
                FRAMEBUFFER_ORDER_MODE.id,
                FRAMEBUFFER_ORDER_MODE.description,
                FRAMEBUFFER_ORDER_MODE_SELECTION.source()
            );
        }

        if (!loggedActiveShaderpack) {
            loggedActiveShaderpack = true;
            LOGGER.info(
                "Minimal recursive portal detected Iris shaderpack: {}",
                getConfiguredIrisShaderpack()
            );
        }

        if (
            FRAMEBUFFER_DEPTH_MODE.usesDepthMask &&
            FRAMEBUFFER_ORDER_MODE.allowDepthMask &&
            !loggedMinimalMaskAttempt
        ) {
            loggedMinimalMaskAttempt = true;
            LOGGER.info("Minimal portal depth mask attempted: true");
        }

        RenderType depthMaskRenderType = IPRenderPipelines.getMinimalPortalDepthMaskRenderType();
        RenderType maskedFramebufferRenderType =
            FRAMEBUFFER_DEPTH_MODE.getFramebufferRenderType(secondaryFrameBuffer.fb);
        boolean useDepthMask =
            FRAMEBUFFER_DEPTH_MODE.usesDepthMask &&
                FRAMEBUFFER_ORDER_MODE.allowDepthMask &&
                depthMaskRenderType != null &&
                maskedFramebufferRenderType != null;
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

        if (!loggedFramebufferPipelineMode) {
            loggedFramebufferPipelineMode = true;
            LOGGER.info(
                "Minimal recursive portal framebuffer pipeline mode: {}",
                useDepthMask ? FRAMEBUFFER_DEPTH_MODE.pipelineModeName : "non-depth-masked"
            );
        }

        try {
            if (useDepthMask) {
                if (!FRAMEBUFFER_ORDER_MODE.quadFirst) {
                    submitPortalDepthMask(portal, poseStack, submitNodeCollector, depthMaskRenderType, 0);
                }
                if (!loggedMinimalMaskApplied) {
                    loggedMinimalMaskApplied = true;
                    LOGGER.info(
                        "Minimal portal depth mask applied: true " +
                            "({} order, {} textured pass)",
                        FRAMEBUFFER_ORDER_MODE.id,
                        FRAMEBUFFER_DEPTH_MODE.depthTestName
                    );
                }
            }
            else if (!loggedMinimalMaskFallback) {
                loggedMinimalMaskFallback = true;
                if (FRAMEBUFFER_DEPTH_MODE == FramebufferDepthMode.NO_DEPTH) {
                    LOGGER.info("Minimal portal depth mask intentionally disabled by framebuffer depth mode");
                }
                else {
                    LOGGER.info(
                        "Minimal portal depth mask fallback: required depth attachment or pipeline unavailable"
                    );
                }
            }
            MyRenderHelper.submitPortalAreaWithFramebuffer(
                portal,
                poseStack,
                getFramebufferQuadCollector(submitNodeCollector, useDepthMask),
                renderType
            );
            if (useDepthMask && FRAMEBUFFER_ORDER_MODE.quadFirst) {
                submitPortalDepthMask(portal, poseStack, submitNodeCollector, depthMaskRenderType, 1);
            }
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

    private OrderedSubmitNodeCollector getFramebufferQuadCollector(
        SubmitNodeCollector submitNodeCollector,
        boolean useDepthMask
    ) {
        if (!useDepthMask) {
            return submitNodeCollector.order(0);
        }
        return FRAMEBUFFER_ORDER_MODE.quadFirst
            ? submitNodeCollector.order(0)
            : submitNodeCollector.order(1);
    }

    private void submitPortalDepthMask(
        Portal portal,
        PoseStack poseStack,
        SubmitNodeCollector submitNodeCollector,
        RenderType depthMaskRenderType,
        int order
    ) {
        LOGGER.debug("Submitting minimal portal depth mask at SubmitNodeCollector order {}", order);
        MyRenderHelper.submitPortalDepthMask(
            portal,
            poseStack,
            submitNodeCollector.order(order),
            depthMaskRenderType
        );
    }

    private String getConfiguredIrisShaderpack() {
        Path irisProperties = client.gameDirectory.toPath().resolve("config").resolve("iris.properties");
        if (!Files.isRegularFile(irisProperties)) {
            return "unavailable";
        }
        try {
            for (String line : Files.readAllLines(irisProperties)) {
                String trimmed = line.trim();
                if (trimmed.startsWith("shaderPack=")) {
                    String shaderpack = trimmed.substring("shaderPack=".length()).trim();
                    return shaderpack.isEmpty() ? "disabled" : shaderpack;
                }
            }
        }
        catch (IOException exception) {
            return "unavailable: " + exception.getClass().getSimpleName();
        }
        return "not configured";
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
