package qouteall.imm_ptl.core.render.pipeline;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderPipelines;
import org.jetbrains.annotations.Nullable;

import java.util.EnumMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.OptionalDouble;
import java.util.OptionalInt;

/**
 * Transition point between the removed ShaderInstance path and the 26.1 pipeline API.
 */
public final class IPRenderPipelines {
    public static final String CLIPPING_EQUATION_UNIFORM = "iportal_ClippingEquation";

    public enum Slot {
        DRAW_FRAMEBUFFER_IN_AREA,
        PORTAL_AREA,
        BLIT_SCREEN_NO_BLEND,
        SCREEN_TRIANGLE
    }

    private static final Map<Slot, RenderPipeline> PIPELINES = new EnumMap<>(Slot.class);

    private IPRenderPipelines() {
    }

    public static void init() {
        register(
            Slot.SCREEN_TRIANGLE,
            RenderPipelines.register(
                RenderPipeline.builder(RenderPipelines.MATRICES_PROJECTION_SNIPPET)
                    .withLocation("pipeline/imm_ptl_screen_triangle")
                    .withVertexShader("core/position_color")
                    .withFragmentShader("core/position_color")
                    .withCull(false)
                    .withVertexFormat(DefaultVertexFormat.POSITION_COLOR, VertexFormat.Mode.TRIANGLES)
                    .build()
            )
        );
    }

    public static void register(Slot slot, RenderPipeline pipeline) {
        PIPELINES.put(slot, pipeline);
    }

    public static @Nullable RenderPipeline get(Slot slot) {
        return PIPELINES.get(slot);
    }

    public static boolean isReady(Slot slot) {
        return PIPELINES.containsKey(slot);
    }

    public static boolean preparePass(RenderPass pass, Slot slot, Consumer<RenderPass> setup) {
        RenderPipeline pipeline = PIPELINES.get(slot);
        if (pipeline == null) {
            return false;
        }

        pass.setPipeline(pipeline);
        setup.accept(pass);
        return true;
    }

    public static boolean drawMesh(Slot slot, MeshData mesh) {
        RenderPipeline pipeline = PIPELINES.get(slot);
        if (pipeline == null) {
            mesh.close();
            return false;
        }

        MeshData.DrawState drawState = mesh.drawState();
        RenderTarget target = Minecraft.getInstance().getMainRenderTarget();
        GpuBuffer vertexBuffer = drawState.format().uploadImmediateVertexBuffer(mesh.vertexBuffer());
        GpuBuffer indexBuffer = mesh.indexBuffer() == null
            ? null
            : drawState.format().uploadImmediateIndexBuffer(mesh.indexBuffer());

        try (
            mesh;
            vertexBuffer;
            indexBuffer
        ) {
            RenderPass pass;
            if (target.getDepthTextureView() != null) {
                pass = RenderSystem.getDevice().createCommandEncoder().createRenderPass(
                    () -> "imm_ptl_" + slot.name().toLowerCase(),
                    target.getColorTextureView(),
                    OptionalInt.empty(),
                    target.getDepthTextureView(),
                    OptionalDouble.empty()
                );
            }
            else {
                pass = RenderSystem.getDevice().createCommandEncoder().createRenderPass(
                    () -> "imm_ptl_" + slot.name().toLowerCase(),
                    target.getColorTextureView(),
                    OptionalInt.empty()
                );
            }

            try (pass) {
                pass.setPipeline(pipeline);
                RenderSystem.bindDefaultUniforms(pass);
                pass.setVertexBuffer(0, vertexBuffer);

                if (indexBuffer != null) {
                    pass.setIndexBuffer(indexBuffer, drawState.indexType());
                    pass.drawIndexed(0, 0, drawState.indexCount(), 1);
                }
                else {
                    pass.draw(0, drawState.vertexCount());
                }
            }
        }

        return true;
    }

    public static void clearRenderTarget(RenderTarget target, int color, double depth) {
        if (target.getDepthTexture() != null) {
            RenderSystem.getDevice().createCommandEncoder().clearColorAndDepthTextures(
                target.getColorTexture(), color,
                target.getDepthTexture(), depth
            );
        }
        else {
            RenderSystem.getDevice().createCommandEncoder().clearColorTexture(
                target.getColorTexture(), color
            );
        }
    }

    public static void clear() {
        PIPELINES.clear();
    }
}
