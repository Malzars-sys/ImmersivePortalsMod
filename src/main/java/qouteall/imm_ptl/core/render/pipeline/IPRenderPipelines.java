package qouteall.imm_ptl.core.render.pipeline;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.pipeline.DepthStencilState;
import com.mojang.blaze3d.platform.CompareOp;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuTextureView;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.EnumMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.Optional;

/**
 * Transition point between the removed ShaderInstance path and the 26.1 pipeline API.
 */
public final class IPRenderPipelines {
    private static final Logger LOGGER = LoggerFactory.getLogger("IPRenderPipelines");
    public static final String CLIPPING_EQUATION_UNIFORM = "iportal_ClippingEquation";

    public enum Slot {
        DRAW_FRAMEBUFFER_IN_AREA,
        DRAW_FRAMEBUFFER_IN_AREA_DEPTH_MASKED,
        PORTAL_DEPTH_MASK,
        PORTAL_AREA,
        BLIT_SCREEN_NO_BLEND,
        SCREEN_TRIANGLE
    }

    private static final Map<Slot, RenderPipeline> PIPELINES = new EnumMap<>(Slot.class);
    private static final Identifier MINIMAL_PORTAL_FRAMEBUFFER_TEXTURE =
        Identifier.fromNamespaceAndPath("imm_ptl", "minimal_portal_framebuffer");
    private static FramebufferTextureAlias minimalPortalFramebufferTexture;
    private static RenderType minimalPortalFramebufferRenderType;
    private static RenderType minimalPortalMaskedFramebufferRenderType;
    private static RenderType minimalPortalDepthMaskRenderType;

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
        register(
            Slot.DRAW_FRAMEBUFFER_IN_AREA,
            RenderPipelines.register(
                RenderPipeline.builder(RenderPipelines.MATRICES_PROJECTION_SNIPPET)
                    .withLocation("pipeline/imm_ptl_draw_framebuffer_in_area")
                    .withVertexShader("core/position_tex")
                    .withFragmentShader("core/position_tex")
                    .withSampler("Sampler0")
                    .withCull(false)
                    .withVertexFormat(DefaultVertexFormat.POSITION_TEX, VertexFormat.Mode.TRIANGLES)
                    .build()
                )
        );
        register(
            Slot.DRAW_FRAMEBUFFER_IN_AREA_DEPTH_MASKED,
            RenderPipelines.register(
                RenderPipeline.builder(RenderPipelines.MATRICES_PROJECTION_SNIPPET)
                    .withLocation("pipeline/imm_ptl_draw_framebuffer_in_area_depth_masked")
                    .withVertexShader("core/position_tex")
                    .withFragmentShader("core/position_tex")
                    .withSampler("Sampler0")
                    .withCull(false)
                    .withDepthStencilState(new DepthStencilState(CompareOp.EQUAL, false))
                    .withVertexFormat(DefaultVertexFormat.POSITION_TEX, VertexFormat.Mode.TRIANGLES)
                    .build()
            )
        );
        register(
            Slot.PORTAL_DEPTH_MASK,
            RenderPipelines.register(
                RenderPipeline.builder(RenderPipelines.MATRICES_PROJECTION_SNIPPET)
                    .withLocation("pipeline/imm_ptl_portal_depth_mask")
                    .withVertexShader("core/position_color")
                    .withFragmentShader("core/position_color")
                    .withCull(false)
                    .withColorTargetState(new ColorTargetState(Optional.empty(), ColorTargetState.WRITE_NONE))
                    .withDepthStencilState(new DepthStencilState(CompareOp.LESS_THAN_OR_EQUAL, true))
                    .withVertexFormat(DefaultVertexFormat.POSITION_COLOR, VertexFormat.Mode.TRIANGLES)
                    .build()
            )
        );

        registerIrisShaderpackFallbacks();
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
        return drawMesh(slot, mesh, null);
    }

    public static boolean drawTexturedMesh(Slot slot, MeshData mesh, GpuTextureView textureView) {
        return drawMesh(slot, mesh, textureView);
    }

    public static @Nullable RenderType getMinimalPortalFramebufferRenderType(RenderTarget framebuffer) {
        RenderPipeline pipeline = PIPELINES.get(Slot.DRAW_FRAMEBUFFER_IN_AREA);
        if (pipeline == null || framebuffer.getColorTextureView() == null) {
            return null;
        }

        Minecraft client = Minecraft.getInstance();
        if (minimalPortalFramebufferTexture == null) {
            minimalPortalFramebufferTexture = new FramebufferTextureAlias();
            client.getTextureManager().register(
                MINIMAL_PORTAL_FRAMEBUFFER_TEXTURE,
                minimalPortalFramebufferTexture
            );
        }
        minimalPortalFramebufferTexture.bind(framebuffer);

        if (minimalPortalFramebufferRenderType == null) {
            RenderSetup setup = RenderSetup.builder(pipeline)
                .withTexture(
                    "Sampler0",
                    MINIMAL_PORTAL_FRAMEBUFFER_TEXTURE,
                    () -> RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR)
                )
                .createRenderSetup();
            minimalPortalFramebufferRenderType = RenderType.create(
                "imm_ptl_minimal_portal_framebuffer",
                setup
            );
        }

        return minimalPortalFramebufferRenderType;
    }

    public static @Nullable RenderType getMinimalPortalDepthMaskRenderType() {
        if (Minecraft.getInstance().getMainRenderTarget().getDepthTextureView() == null) {
            return null;
        }
        RenderPipeline pipeline = PIPELINES.get(Slot.PORTAL_DEPTH_MASK);
        if (pipeline == null) {
            return null;
        }
        if (minimalPortalDepthMaskRenderType == null) {
            minimalPortalDepthMaskRenderType = RenderType.create(
                "imm_ptl_minimal_portal_depth_mask",
                RenderSetup.builder(pipeline).createRenderSetup()
            );
        }
        return minimalPortalDepthMaskRenderType;
    }

    public static @Nullable RenderType getMinimalPortalMaskedFramebufferRenderType(
        RenderTarget framebuffer
    ) {
        if (Minecraft.getInstance().getMainRenderTarget().getDepthTextureView() == null) {
            return null;
        }
        RenderPipeline pipeline = PIPELINES.get(Slot.DRAW_FRAMEBUFFER_IN_AREA_DEPTH_MASKED);
        if (pipeline == null || framebuffer.getColorTextureView() == null) {
            return null;
        }

        getMinimalPortalFramebufferRenderType(framebuffer);
        if (minimalPortalFramebufferTexture == null) {
            return null;
        }
        if (minimalPortalMaskedFramebufferRenderType == null) {
            RenderSetup setup = RenderSetup.builder(pipeline)
                .withTexture(
                    "Sampler0",
                    MINIMAL_PORTAL_FRAMEBUFFER_TEXTURE,
                    () -> RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR)
                )
                .createRenderSetup();
            minimalPortalMaskedFramebufferRenderType = RenderType.create(
                "imm_ptl_minimal_portal_framebuffer_depth_masked",
                setup
            );
        }
        return minimalPortalMaskedFramebufferRenderType;
    }

    private static boolean drawMesh(Slot slot, MeshData mesh, @Nullable GpuTextureView textureView) {
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

        try (mesh) {
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
                if (textureView != null) {
                    pass.bindTexture(
                        "Sampler0",
                        textureView,
                        RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR)
                    );
                }
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
        minimalPortalFramebufferRenderType = null;
        minimalPortalMaskedFramebufferRenderType = null;
        minimalPortalDepthMaskRenderType = null;
    }

    private static void registerIrisShaderpackFallbacks() {
        try {
            Class<?> irisPipelinesClass = Class.forName("net.irisshaders.iris.pipeline.IrisPipelines");
            Class<?> shaderKeyClass = Class.forName("net.irisshaders.iris.pipeline.programs.ShaderKey");
            java.lang.reflect.Method assignPipeline = irisPipelinesClass.getMethod(
                "assignPipeline",
                RenderPipeline.class,
                shaderKeyClass
            );

            assignIrisPipeline(
                assignPipeline,
                shaderKeyClass,
                PIPELINES.get(Slot.PORTAL_DEPTH_MASK),
                "BASIC_COLOR"
            );
            assignIrisPipeline(
                assignPipeline,
                shaderKeyClass,
                PIPELINES.get(Slot.DRAW_FRAMEBUFFER_IN_AREA),
                "TEXTURED"
            );
            assignIrisPipeline(
                assignPipeline,
                shaderKeyClass,
                PIPELINES.get(Slot.DRAW_FRAMEBUFFER_IN_AREA_DEPTH_MASKED),
                "TEXTURED"
            );

            LOGGER.info(
                "Registered Iris shaderpack fallback mappings for minimal portal pipelines"
            );
        }
        catch (ClassNotFoundException ignored) {
            // Iris is not present in the vanilla or Sodium-only profile.
        }
        catch (Throwable throwable) {
            LOGGER.warn(
                "Unable to register Iris shaderpack fallback mappings for minimal portal pipelines",
                throwable
            );
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void assignIrisPipeline(
        java.lang.reflect.Method assignPipeline,
        Class<?> shaderKeyClass,
        @Nullable RenderPipeline pipeline,
        String shaderKeyName
    ) throws ReflectiveOperationException {
        if (pipeline == null) {
            return;
        }
        Object shaderKey = Enum.valueOf((Class<? extends Enum>) shaderKeyClass, shaderKeyName);
        assignPipeline.invoke(null, pipeline, shaderKey);
    }

    private static final class FramebufferTextureAlias extends AbstractTexture {
        private void bind(RenderTarget framebuffer) {
            texture = framebuffer.getColorTexture();
            textureView = framebuffer.getColorTextureView();
        }

        @Override
        public void close() {
            // The RenderTarget owns these GPU resources. This alias must never close them.
            texture = null;
            textureView = null;
        }
    }
}
