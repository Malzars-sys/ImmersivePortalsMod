package qouteall.imm_ptl.core.mixin.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.client.renderer.RenderBuffers;
import net.minecraft.client.renderer.ViewArea;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import qouteall.imm_ptl.core.ducks.IEWorldRenderer;

/**
 * Minimal vanilla-profile bridge for the state required by one-layer portal rendering.
 *
 * <p>Legacy LevelRenderer injections, terrain replacement, clipping, weather and
 * translucent sorting intentionally remain isolated.</p>
 */
@Mixin(LevelRenderer.class)
public abstract class MixinLevelRenderer implements IEWorldRenderer {
    @Shadow
    @Final
    private EntityRenderDispatcher entityRenderDispatcher;

    @Shadow
    private ViewArea viewArea;

    @Shadow
    @Final
    private RenderBuffers renderBuffers;

    @Shadow
    @Final
    private ObjectArrayList<SectionRenderDispatcher.RenderSection> visibleSections;

    @Unique
    private @Nullable PostChain ip_transparencyChain;

    @Unique
    private @Nullable RenderBuffers ip_renderBuffersOverride;

    @Unique
    private @Nullable Frustum ip_frustum;

    @Unique
    private @Nullable ObjectArrayList<SectionRenderDispatcher.RenderSection> ip_visibleSectionsOverride;

    @Override
    public EntityRenderDispatcher ip_getEntityRenderDispatcher() {
        return entityRenderDispatcher;
    }

    @Override
    public ViewArea ip_getBuiltChunkStorage() {
        return viewArea;
    }

    @Override
    public void ip_myRenderEntity(
        Entity entity,
        double cameraX,
        double cameraY,
        double cameraZ,
        float partialTick,
        PoseStack matrixStack,
        MultiBufferSource vertexConsumerProvider
    ) {
        // Entity submission is owned by the extracted 26.1 render state.
    }

    @Override
    public PostChain portal_getTransparencyShader() {
        return ip_transparencyChain;
    }

    @Override
    public void portal_setTransparencyShader(PostChain chain) {
        ip_transparencyChain = chain;
    }

    @Override
    public RenderBuffers ip_getRenderBuffers() {
        return ip_renderBuffersOverride != null ? ip_renderBuffersOverride : renderBuffers;
    }

    @Override
    public void ip_setRenderBuffers(RenderBuffers buffers) {
        ip_renderBuffersOverride = buffers == renderBuffers ? null : buffers;
    }

    @Override
    public Frustum portal_getFrustum() {
        return ip_frustum;
    }

    @Override
    public void portal_setFrustum(Frustum frustum) {
        ip_frustum = frustum;
    }

    @Override
    public void portal_fullyDispose() {
        ip_transparencyChain = null;
        ip_renderBuffersOverride = null;
        ip_frustum = null;
        ip_visibleSectionsOverride = null;
    }

    @Override
    public void portal_setChunkInfoList(ObjectArrayList<SectionRenderDispatcher.RenderSection> sections) {
        ip_visibleSectionsOverride = sections == visibleSections ? null : sections;
    }

    @Override
    public ObjectArrayList<SectionRenderDispatcher.RenderSection> portal_getChunkInfoList() {
        return ip_visibleSectionsOverride != null ? ip_visibleSectionsOverride : visibleSections;
    }
}
