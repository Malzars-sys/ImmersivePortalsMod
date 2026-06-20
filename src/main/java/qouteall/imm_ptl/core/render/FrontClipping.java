package qouteall.imm_ptl.core.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import org.lwjgl.opengl.GL11;
import org.slf4j.Logger;
import qouteall.imm_ptl.core.CHelper;
import qouteall.imm_ptl.core.IPCGlobal;
import qouteall.imm_ptl.core.IPGlobal;
import qouteall.imm_ptl.core.portal.Portal;
import qouteall.imm_ptl.core.render.context_management.PortalRendering;
import qouteall.q_misc_util.my_util.Plane;

public class FrontClipping {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Minecraft client = Minecraft.getInstance();
    private static double[] activeClipPlaneEquationBeforeModelView;
    private static double[] activeClipPlaneAfterModelView;
    
    public static boolean isClippingEnabled = false;
    
    public static final double ADJUSTMENT = 0.01;
    private static Plane activeMinimalCpuClippingPlane;
    private static boolean loggedMinimalClippingAttempt;
    private static boolean loggedMinimalClippingPlan;
    private static boolean loggedMinimalClippingApplied;
    private static boolean loggedMinimalClippingFallback;
    private static boolean loggedMinimalPortalCulled;

    public static boolean beginMinimalCpuClipping() {
        if (!loggedMinimalClippingAttempt) {
            loggedMinimalClippingAttempt = true;
            LOGGER.info("Minimal destination clipping attempted: true");
        }

        Plane clippingPlane = PortalRendering.getActiveClippingPlane();
        if (clippingPlane == null) {
            activeMinimalCpuClippingPlane = null;
            if (!loggedMinimalClippingFallback) {
                loggedMinimalClippingFallback = true;
                LOGGER.info("Minimal destination clipping fallback: active portal clipping plane is unavailable");
            }
            return false;
        }

        activeMinimalCpuClippingPlane = clippingPlane.move(-ADJUSTMENT);
        if (!loggedMinimalClippingPlan) {
            loggedMinimalClippingPlan = true;
            LOGGER.info("Minimal destination clipping plane calculated: true ({})", clippingPlane);
        }
        if (!loggedMinimalClippingApplied) {
            loggedMinimalClippingApplied = true;
            LOGGER.info(
                "Minimal destination clipping applied: CPU portal-entity prefilter only; " +
                    "vanilla 26.1 shaders do not expose a global clip plane"
            );
        }
        return true;
    }

    public static void endMinimalCpuClipping() {
        activeMinimalCpuClippingPlane = null;
    }

    public static boolean shouldCullPortalEntityByMinimalClipping(Portal portal) {
        Plane clippingPlane = activeMinimalCpuClippingPlane;
        if (clippingPlane == null) {
            return false;
        }

        Vec3 origin = portal.getOriginPos();
        if (clippingPlane.getDistanceTo(origin) >= 0) {
            return false;
        }
        for (Vec3 localVertex : portal.getFourVerticesLocal(0)) {
            if (clippingPlane.getDistanceTo(origin.add(localVertex)) >= 0) {
                return false;
            }
        }

        if (!loggedMinimalPortalCulled) {
            loggedMinimalPortalCulled = true;
            LOGGER.info(
                "Minimal destination clipping culled portal entity {} behind the active plane",
                portal.getId()
            );
        }
        return true;
    }
    
    public static void disableClipping() {
        if (IPGlobal.enableClippingMechanism) {
            if (isClippingEnabled) {
                GL11.glDisable(GL11.GL_CLIP_PLANE0);
                isClippingEnabled = false;
            }
        }
    }
    
    private static void enableClipping() {
        if (IPGlobal.enableClippingMechanism) {
            if (!isClippingEnabled) {
                GL11.glEnable(GL11.GL_CLIP_PLANE0);
                isClippingEnabled = true;
            }
        }
    }
    
    public static void updateInnerClipping(PoseStack matrixStack) {
        Matrix4f modelView = matrixStack.last().pose();
        updateInnerClipping(modelView);
    }
    
    public static void updateInnerClipping(Matrix4f modelView) {
        if (PortalRendering.isRendering()) {
            setupInnerClipping(
                PortalRendering.getActiveClippingPlane(),
                modelView, 0
            );
        }
        else {
            disableClipping();
        }
    }
    
    // NOTE the actual clipping plane is related to current model view matrix
    public static void setupInnerClipping(
        Plane clipping, Matrix4f modelView, double adjustment
    ) {
        if (!IPCGlobal.useFrontClipping) {
            return;
        }
        
        // Note: the normal of plane points to the non-clipped side
        
        if (clipping != null) {
            activeClipPlaneEquationBeforeModelView =
                getClipEquationInner(clipping.pos(), clipping.normal(), adjustment);
            activeClipPlaneAfterModelView =
                transformClipEquation(activeClipPlaneEquationBeforeModelView, modelView);
            
            enableClipping();
        }
        else {
            activeClipPlaneEquationBeforeModelView = null;
            disableClipping();
        }
    }
    
    private static double[] transformClipEquation(
        double[] equation, Matrix4f modelView
    ) {
        Vector4f eq =
            new Vector4f((float) equation[0], (float) equation[1], (float) equation[2], (float) equation[3]);
        Matrix4f m = new Matrix4f(modelView);
        m.invert();
        m.transpose();
        m.transform(eq);
        return new double[]{eq.x(), eq.y(), eq.z(), eq.w()};
    }
    
    private static double[] getClipEquationInner(
        Vec3 clippingPoint, Vec3 clippingDirection, double correction
    ) {
        Vec3 cameraPos = CHelper.getCurrentCameraPos();
        
        Vec3 planeNormal = clippingDirection;
        
        Vec3 portalPos = clippingPoint
            .add(planeNormal.scale(correction))
            .subtract(cameraPos);
        
        //equation: planeNormal * p + c > 0
        //-planeNormal * portalCenter = c
        double c = planeNormal.scale(-1).dot(portalPos);
        
        return new double[]{
            planeNormal.x, planeNormal.y, planeNormal.z, c
        };
    }
    
    public static void setupOuterClipping(PoseStack matrixStack, Portal portal) {
        if (!IPCGlobal.useFrontClipping) {
            return;
        }
        
        double[] clipEquationOuter = getClipEquationOuter(portal);
        
        if (clipEquationOuter != null) {
            activeClipPlaneEquationBeforeModelView = clipEquationOuter;
            activeClipPlaneAfterModelView = transformClipEquation(
                activeClipPlaneEquationBeforeModelView, matrixStack.last().pose()
            );
            enableClipping();
        }
        else {
            activeClipPlaneEquationBeforeModelView = null;
            disableClipping();
        }
    }
    
    // "double @Nullable []" is weird...
    private static double @Nullable [] getClipEquationOuter(Portal portal) {
        @Nullable Plane outerClipping = portal.getPortalShape()
            .getOuterClipping(portal.getThisSideState());
        
        if (outerClipping == null) {
            return null;
        }
        
        Vec3 planeNormal = outerClipping.normal();
        
        Vec3 cameraPos = client.gameRenderer.getMainCamera().position();
        
        Vec3 portalPos = outerClipping.pos()
            .subtract(cameraPos);
        
        //equation: planeNormal * p + c > 0
        //-planeNormal * portalCenter = c
        double c = planeNormal.scale(-1).dot(portalPos);
        
        return new double[]{
            planeNormal.x, planeNormal.y, planeNormal.z, c
        };
    }
    
    public static double[] getActiveClipPlaneEquationBeforeModelView() {
        return activeClipPlaneEquationBeforeModelView;
    }
    
    public static double[] getActiveClipPlaneEquationAfterModelView() {
        return activeClipPlaneAfterModelView;
    }
    
    public static void updateClippingEquationUniformForCurrentShader(
        boolean isRenderingEntities
    ) {
        // Deferred until portal pipelines expose a clipping-plane uniform.
    }
    
    public static void unsetClippingUniform() {
        // Deferred until portal pipelines expose a clipping-plane uniform.
    }
}
