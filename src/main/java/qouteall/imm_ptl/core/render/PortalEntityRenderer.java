package qouteall.imm_ptl.core.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import qouteall.imm_ptl.core.IPCGlobal;
import qouteall.imm_ptl.core.portal.Portal;

@Environment(EnvType.CLIENT)
public class PortalEntityRenderer<T extends Portal>
    extends EntityRenderer<T, PortalEntityRenderer.PortalRenderState<T>> {

    private static final Logger LOGGER = LoggerFactory.getLogger(PortalEntityRenderer.class);
    private static final int FRAME_COLOR = 0xff22ddff;
    private static final int DIAGONAL_COLOR = 0x9922ddff;
    private static boolean loggedSodiumSubmit;

    public PortalEntityRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public PortalRenderState<T> createRenderState() {
        return new PortalRenderState<>();
    }

    @Override
    public void extractRenderState(T portal, PortalRenderState<T> state, float partialTick) {
        super.extractRenderState(portal, state, partialTick);
        state.portal = portal;
        state.axisW = portal.getAxisW();
        state.axisH = portal.getAxisH();
        state.width = portal.getWidth();
        state.height = portal.getHeight();
    }

    @Override
    public void submit(
        PortalRenderState<T> state,
        PoseStack poseStack,
        SubmitNodeCollector submitNodeCollector,
        CameraRenderState cameraRenderState
    ) {
        if (FabricLoader.getInstance().isModLoaded("sodium") && !loggedSodiumSubmit) {
            loggedSodiumSubmit = true;
            LOGGER.info("PortalEntityRenderer submit called under Sodium: true");
        }
        if (FrontClipping.shouldCullPortalEntityByMinimalClipping(state.portal)) {
            return;
        }
        if (IPCGlobal.useMinimalRecursivePortalRendering) {
            IPCGlobal.rendererUsingFrameBuffer.queueMinimalPortalFromEntityRenderer(state.portal);
            IPCGlobal.rendererUsingFrameBuffer.renderPortalInEntityRenderer(
                state.portal,
                poseStack,
                submitNodeCollector
            );
        }
        else {
            IPCGlobal.renderer.renderPortalInEntityRenderer(
                state.portal,
                poseStack,
                submitNodeCollector
            );
        }
        submitMinimalPortalFrame(state, poseStack, submitNodeCollector);
        super.submit(state, poseStack, submitNodeCollector, cameraRenderState);
    }

    private static void submitMinimalPortalFrame(
        PortalRenderState<?> state,
        PoseStack poseStack,
        SubmitNodeCollector submitNodeCollector
    ) {
        Vec3 halfW = state.axisW.scale(state.width * 0.5);
        Vec3 halfH = state.axisH.scale(state.height * 0.5);
        Vec3[] corners = {
            halfW.add(halfH),
            halfW.subtract(halfH),
            halfW.scale(-1).subtract(halfH),
            halfW.scale(-1).add(halfH)
        };

        submitNodeCollector.submitCustomGeometry(
            poseStack,
            RenderTypes.linesTranslucent(),
            (pose, vertexConsumer) -> {
                for (int i = 0; i < corners.length; i++) {
                    putLine(vertexConsumer, pose, corners[i], corners[(i + 1) % corners.length], FRAME_COLOR);
                }
                putLine(vertexConsumer, pose, corners[0], corners[2], DIAGONAL_COLOR);
                putLine(vertexConsumer, pose, corners[1], corners[3], DIAGONAL_COLOR);
            }
        );
    }

    private static void putLine(
        com.mojang.blaze3d.vertex.VertexConsumer vertexConsumer,
        PoseStack.Pose pose,
        Vec3 from,
        Vec3 to,
        int color
    ) {
        Vec3 normal = to.subtract(from).normalize();
        vertexConsumer.addVertex(pose.pose(), (float) from.x, (float) from.y, (float) from.z)
            .setColor(color)
            .setNormal(pose, (float) normal.x, (float) normal.y, (float) normal.z)
            .setLineWidth(2.0f);
        vertexConsumer.addVertex(pose.pose(), (float) to.x, (float) to.y, (float) to.z)
            .setColor(color)
            .setNormal(pose, (float) normal.x, (float) normal.y, (float) normal.z)
            .setLineWidth(2.0f);
    }

    public static class PortalRenderState<T extends Portal> extends EntityRenderState {
        public T portal;
        public Vec3 axisW = Vec3.ZERO;
        public Vec3 axisH = Vec3.ZERO;
        public double width;
        public double height;
    }
}
