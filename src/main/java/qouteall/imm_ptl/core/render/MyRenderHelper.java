package qouteall.imm_ptl.core.render;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.opengl.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.OrderedSubmitNodeCollector;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import qouteall.imm_ptl.core.CHelper;
import qouteall.imm_ptl.core.ClientWorldLoader;
import qouteall.imm_ptl.core.miscellaneous.IPVanillaCopy;
import qouteall.imm_ptl.core.portal.Portal;
import qouteall.imm_ptl.core.render.context_management.PortalRendering;
import qouteall.imm_ptl.core.render.context_management.RenderStates;
import qouteall.imm_ptl.core.render.context_management.WorldRenderInfo;
import qouteall.imm_ptl.core.render.pipeline.IPRenderPipelines;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.awt.image.Raster;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.stream.IntStream;

import static org.lwjgl.opengl.GL11.GL_BACK;
import static org.lwjgl.opengl.GL11.GL_DEPTH_COMPONENT;
import static org.lwjgl.opengl.GL11.GL_FLOAT;
import static org.lwjgl.opengl.GL11.GL_FRONT;
import static org.lwjgl.opengl.GL11.GL_RED;
import static org.lwjgl.opengl.GL11.glCullFace;
import static org.lwjgl.opengl.GL11.glReadPixels;

public class MyRenderHelper {
    
    public static final Minecraft client = Minecraft.getInstance();
    private static boolean loggedMinimalFramebufferQuadInfo;
    
    public static void init() {
        IPRenderPipelines.init();
    }
    
    public static void drawPortalAreaWithFramebuffer(
        Portal portal,
        RenderTarget textureProvider,
        Matrix4f modelViewMatrix,
        Matrix4f projectionMatrix
    ) {
        // Legacy compatibility path. The vanilla minimal renderer submits through SubmitNodeCollector.
        Vec3 cameraPos = CHelper.getCurrentCameraPos();
        Vec3 center = portal.getOriginPos().subtract(cameraPos);
        Vec3 halfW = portal.getAxisW().scale(portal.getWidth() * 0.5);
        Vec3 halfH = portal.getAxisH().scale(portal.getHeight() * 0.5);
        Vec3 p0 = center.add(halfW).add(halfH);
        Vec3 p1 = center.subtract(halfW).add(halfH);
        Vec3 p2 = center.subtract(halfW).subtract(halfH);
        Vec3 p3 = center.add(halfW).subtract(halfH);

        drawPortalAreaWithFramebuffer(textureProvider, p0, p1, p2, p3);
    }

    private static void drawPortalAreaWithFramebuffer(
        RenderTarget textureProvider,
        Vec3 p0,
        Vec3 p1,
        Vec3 p2,
        Vec3 p3
    ) {
        if (!loggedMinimalFramebufferQuadInfo) {
            loggedMinimalFramebufferQuadInfo = true;
            qouteall.q_misc_util.Helper.log(
                "Minimal recursive portal framebuffer quad vertices=%s %s %s %s fb=%dx%d window=%dx%d".formatted(
                    p0, p1, p2, p3,
                    textureProvider.width, textureProvider.height,
                    client.getWindow().getWidth(), client.getWindow().getHeight()
                )
            );
        }

        BufferBuilder bufferBuilder = Tesselator.getInstance()
            .begin(VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION_TEX);
        putTexturedVertex(bufferBuilder, p0, 1, 1);
        putTexturedVertex(bufferBuilder, p1, 0, 1);
        putTexturedVertex(bufferBuilder, p2, 0, 0);
        putTexturedVertex(bufferBuilder, p0, 1, 1);
        putTexturedVertex(bufferBuilder, p2, 0, 0);
        putTexturedVertex(bufferBuilder, p3, 1, 0);

        IPRenderPipelines.drawTexturedMesh(
            IPRenderPipelines.Slot.DRAW_FRAMEBUFFER_IN_AREA,
            bufferBuilder.buildOrThrow(),
            textureProvider.getColorTextureView()
        );
    }

    private static void putTexturedVertex(BufferBuilder builder, Vec3 pos, float u, float v) {
        builder.addVertex((float) pos.x, (float) pos.y, (float) pos.z).setUv(u, v);
    }

    public static void submitPortalAreaWithFramebuffer(
        Portal portal,
        com.mojang.blaze3d.vertex.PoseStack poseStack,
        OrderedSubmitNodeCollector submitNodeCollector,
        RenderType renderType
    ) {
        // Render-graph path used by the consolidated vanilla minimal renderer.
        Vec3 halfW = portal.getAxisW().scale(portal.getWidth() * 0.5);
        Vec3 halfH = portal.getAxisH().scale(portal.getHeight() * 0.5);
        Vec3 p0 = halfW.add(halfH);
        Vec3 p1 = halfW.scale(-1).add(halfH);
        Vec3 p2 = halfW.scale(-1).subtract(halfH);
        Vec3 p3 = halfW.subtract(halfH);

        submitNodeCollector.submitCustomGeometry(
            poseStack,
            renderType,
            (pose, vertexConsumer) -> {
                putSubmittedTexturedVertex(vertexConsumer, pose, p0, 1, 1);
                putSubmittedTexturedVertex(vertexConsumer, pose, p1, 0, 1);
                putSubmittedTexturedVertex(vertexConsumer, pose, p2, 0, 0);
                putSubmittedTexturedVertex(vertexConsumer, pose, p0, 1, 1);
                putSubmittedTexturedVertex(vertexConsumer, pose, p2, 0, 0);
                putSubmittedTexturedVertex(vertexConsumer, pose, p3, 1, 0);
            }
        );
    }

    public static void submitPortalDepthMask(
        Portal portal,
        com.mojang.blaze3d.vertex.PoseStack poseStack,
        OrderedSubmitNodeCollector submitNodeCollector,
        RenderType renderType
    ) {
        Vec3 halfW = portal.getAxisW().scale(portal.getWidth() * 0.5);
        Vec3 halfH = portal.getAxisH().scale(portal.getHeight() * 0.5);
        Vec3 p0 = halfW.add(halfH);
        Vec3 p1 = halfW.scale(-1).add(halfH);
        Vec3 p2 = halfW.scale(-1).subtract(halfH);
        Vec3 p3 = halfW.subtract(halfH);

        submitNodeCollector.submitCustomGeometry(
            poseStack,
            renderType,
            (pose, vertexConsumer) -> {
                putSubmittedDepthVertex(vertexConsumer, pose, p0);
                putSubmittedDepthVertex(vertexConsumer, pose, p1);
                putSubmittedDepthVertex(vertexConsumer, pose, p2);
                putSubmittedDepthVertex(vertexConsumer, pose, p0);
                putSubmittedDepthVertex(vertexConsumer, pose, p2);
                putSubmittedDepthVertex(vertexConsumer, pose, p3);
            }
        );
    }

    private static void putSubmittedTexturedVertex(
        com.mojang.blaze3d.vertex.VertexConsumer vertexConsumer,
        com.mojang.blaze3d.vertex.PoseStack.Pose pose,
        Vec3 pos,
        float u,
        float v
    ) {
        vertexConsumer.addVertex(pose.pose(), (float) pos.x, (float) pos.y, (float) pos.z)
            .setUv(u, v);
    }

    private static void putSubmittedDepthVertex(
        com.mojang.blaze3d.vertex.VertexConsumer vertexConsumer,
        com.mojang.blaze3d.vertex.PoseStack.Pose pose,
        Vec3 pos
    ) {
        vertexConsumer.addVertex(pose.pose(), (float) pos.x, (float) pos.y, (float) pos.z)
            .setColor(0xffffffff);
    }
    
    public static void renderScreenTriangle() {
        renderScreenTriangle(255, 255, 255, 255);
    }
    
    public static void renderScreenTriangle(Vec3 color) {
        renderScreenTriangle(
            (int) (color.x * 255),
            (int) (color.y * 255),
            (int) (color.z * 255),
            255
        );
    }
    
    public static void testOneTriangle(int r, int g, int b, int a) {
        Tesselator tessellator = Tesselator.getInstance();
        BufferBuilder bufferBuilder = tessellator
            .begin(VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION_COLOR);
        
        // upper triangle
//        bufferBuilder.addVertex(1, -1, 0).setColor(r, g, b, a)
//            ;
//        bufferBuilder.addVertex(1, 1, 0).setColor(r, g, b, a)
//            ;
//        bufferBuilder.addVertex(-1, 1, 0).setColor(r, g, b, a)
//            ;
        
        // down triangle
        bufferBuilder.addVertex(-1, 1, 0).setColor(r, g, b, a);
        bufferBuilder.addVertex(-1, -1, 0).setColor(r, g, b, a);
        bufferBuilder.addVertex(1, -1, 0).setColor(r, g, b, a);
        
        bufferBuilder.addVertex(1, 0, 0).setColor(r, g, b, a);
        bufferBuilder.addVertex(0, 1, 0).setColor(r, g, b, a);
        bufferBuilder.addVertex(-1, 0, 0).setColor(r, g, b, a);
        
        IPRenderPipelines.drawMesh(
            IPRenderPipelines.Slot.SCREEN_TRIANGLE,
            bufferBuilder.buildOrThrow()
        );
    }
    
    /**
     * {@link RenderTarget#blitToScreen(int, int)}
     */
    @IPVanillaCopy
    public static void renderScreenTriangle(int r, int g, int b, int a) {
        Tesselator tessellator = Tesselator.getInstance();
        BufferBuilder bufferBuilder = tessellator.
            begin(VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION_COLOR);
        
        bufferBuilder.addVertex(1, -1, 0).setColor(r, g, b, a);
        bufferBuilder.addVertex(1, 1, 0).setColor(r, g, b, a);
        bufferBuilder.addVertex(-1, 1, 0).setColor(r, g, b, a);
        
        bufferBuilder.addVertex(-1, 1, 0).setColor(r, g, b, a);
        bufferBuilder.addVertex(-1, -1, 0).setColor(r, g, b, a);
        bufferBuilder.addVertex(1, -1, 0).setColor(r, g, b, a);
        
        IPRenderPipelines.drawMesh(
            IPRenderPipelines.Slot.SCREEN_TRIANGLE,
            bufferBuilder.buildOrThrow()
        );
    }
    
    /**
     * {@link RenderTarget#blitToScreen(int, int)}
     */
    public static void drawScreenFrameBuffer(
        RenderTarget textureProvider,
        boolean doUseAlphaBlend,
        boolean doEnableModifyAlpha
    ) {
        int x = 0;
        int y = 0;
        
        int viewportWidth = textureProvider.width;
        int viewportHeight = textureProvider.height;
        
        drawFramebufferWithCoordinatesAndDimensions(
            textureProvider, doUseAlphaBlend, doEnableModifyAlpha,
            x, y, viewportWidth, viewportHeight
        );
    }

    public static void drawFramebuffer(
            RenderTarget textureProvider, boolean doUseAlphaBlend, boolean doEnableModifyAlpha,
            float xMin, float xMax, float yMin, float yMax
    ) {
        drawFramebufferWithCoordinatesAndDimensions(
                textureProvider,
                doUseAlphaBlend, doEnableModifyAlpha,
                0, 0,
                client.getWindow().getWidth(),
                client.getWindow().getHeight()
        );
    }

    public static void drawFramebufferWithViewport(
            RenderTarget textureProvider, boolean doUseAlphaBlend, boolean doEnableModifyAlpha,
            float left, float right, float bottom, float up,
            int viewportWidth, int viewportHeight
    ) {
        drawFramebufferWithCoordinatesAndDimensions(
                textureProvider,
                doUseAlphaBlend, doEnableModifyAlpha,
                0, 0,
                viewportWidth, viewportHeight
        );
    }
    
    public static void drawFramebufferWithBounds(
        RenderTarget textureProvider, boolean doUseAlphaBlend, boolean doEnableModifyAlpha,
        int xMin, int xMax, int yMin, int yMax
    ) {

        drawFramebufferWithCoordinatesAndDimensions(
            textureProvider,
            doUseAlphaBlend, doEnableModifyAlpha,
            xMin, yMin,
            Mth.abs(xMax - xMin),
            Mth.abs(yMax - yMin)
        );
    }
    
    /**
     * {@link RenderTarget#blitToScreen(int, int)}
     */
    @IPVanillaCopy
    public static void drawFramebufferWithCoordinatesAndDimensions(
        RenderTarget textureProvider, boolean doUseAlphaBlend, boolean doEnableModifyAlpha,
        int x, int y, int viewportWidth, int viewportHeight
    ) {
        // Textured framebuffer blits are deferred until their dedicated pipeline is registered.
    }
    
    // it will remove the light sections that are marked to be removed
    // if not, light data will cause minor memory leak
    // and wrongly remove the light data when the chunks get reloaded to client
    // this should not run before world rendering or the smooth lighting may become abnormal in section edge
    public static void lateUpdateLight() {
        if (!ClientWorldLoader.getIsInitialized()) {
            return;
        }
        
        ClientWorldLoader.getClientWorlds().forEach(world -> {
            if (!RenderStates.isDimensionRendered(world.dimension())) {
                world.getChunkSource().getLightEngine().runLightUpdates();
            }
        });
    }
    
    /**
     * If we don't do this
     * the future created in {@link SectionRenderDispatcher#uploadSectionLayer}
     * may never complete
     */
    public static void earlyRemoteUpload() {
        if (!ClientWorldLoader.getIsInitialized()) {
            return;
        }
        
        // Section uploads are handled by the 26.1 renderer.
    }
    
    public static void applyMirrorFaceCulling() {
        glCullFace(GL_FRONT);
    }
    
    public static void recoverFaceCulling() {
        glCullFace(GL_BACK);
    }
    
    public static void clearAlphaTo1(RenderTarget mcFrameBuffer) {
        // Alpha-only clears require a dedicated 26.1 pipeline.
    }
    
    public static void restoreViewPort() {
        Minecraft client = Minecraft.getInstance();
        GlStateManager._viewport(
            0,
            0,
            client.getWindow().getWidth(),
            client.getWindow().getHeight()
        );
    }
    
    public static float transformFogDistance(float value) {
        if (!WorldRenderInfo.isFogEnabled()) {
            return value * 23333;
        }
        
        // just disable fog for fuse-view portals for now
        if (PortalRendering.isRendering()) {
            Portal renderingPortal = PortalRendering.getRenderingPortal();
            
            if (renderingPortal.isFuseView()) {
                return value * 23333;
            }
        }
        
        // as non-fuse-view portals does not apply scale transformation to modelview,
        // there is no need to transform fog distance (both with and without sodium)
        
        return value;
    }
    
    private static boolean debugEnabled = false;
    
    public static void debugFramebufferDepth() {
        if (!debugEnabled) {
            return;
        }
        debugEnabled = false;
        
        int width = client.getMainRenderTarget().width;
        int height = client.getMainRenderTarget().height;
        
        
        ByteBuffer directBuffer = ByteBuffer.allocateDirect(width * height * 4).order(ByteOrder.LITTLE_ENDIAN);
        
        FloatBuffer floatBuffer = directBuffer.asFloatBuffer();
        
        glReadPixels(
            0, 0, width, height,
            GL_DEPTH_COMPONENT, GL_FLOAT, floatBuffer
        );
        
        float[] data = new float[width * height];
        
        floatBuffer.rewind();
        floatBuffer.get(data);
        
        float maxValue = (float) IntStream.range(0, data.length)
            .mapToDouble(i -> data[i]).max().getAsDouble();
        float minValue = (float) IntStream.range(0, data.length)
            .mapToDouble(i -> data[i]).min().getAsDouble();
        
        byte[] grayData = new byte[width * height];
        for (int i = 0; i < data.length; i++) {
            float datum = data[i];
            
            datum = (datum - minValue) / (maxValue - minValue);
            
            grayData[i] = (byte) (datum * 255);
        }
        
        BufferedImage bufferedImage =
            new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);
        
        bufferedImage.setData(
            Raster.createRaster(
                bufferedImage.getSampleModel(),
                new DataBufferByte(grayData, grayData.length), new Point()
            )
        );
        
        System.out.println("oops");
    }
    
    public static void debugFramebufferColorRed() {
        if (!debugEnabled) {
            return;
        }
        debugEnabled = false;
        
        int width = client.getMainRenderTarget().width;
        int height = client.getMainRenderTarget().height;
        
        
        ByteBuffer directBuffer = ByteBuffer.allocateDirect(width * height * 4).order(ByteOrder.LITTLE_ENDIAN);
        
        FloatBuffer floatBuffer = directBuffer.asFloatBuffer();
        
        glReadPixels(
            0, 0, width, height,
            GL_RED, GL_FLOAT, floatBuffer
        );
        
        float[] data = new float[width * height];
        
        floatBuffer.rewind();
        floatBuffer.get(data);
        
        float maxValue = (float) IntStream.range(0, data.length)
            .mapToDouble(i -> data[i]).max().getAsDouble();
        float minValue = (float) IntStream.range(0, data.length)
            .mapToDouble(i -> data[i]).min().getAsDouble();
        
        byte[] grayData = new byte[width * height];
        for (int i = 0; i < data.length; i++) {
            float datum = data[i];
            
            datum = (datum - minValue) / (maxValue - minValue);
            
            grayData[i] = (byte) (datum * 255);
        }
        
        BufferedImage bufferedImage =
            new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);
        
        bufferedImage.setData(
            Raster.createRaster(
                bufferedImage.getSampleModel(),
                new DataBufferByte(grayData, grayData.length), new Point()
            )
        );
        
        System.out.println("oops");
    }
}
