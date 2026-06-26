package qouteall.imm_ptl.core.compat.iris_compatibility;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.fabricmc.fabric.api.client.renderer.v1.Renderer;
import net.fabricmc.fabric.api.client.renderer.v1.mesh.Mesh;
import net.fabricmc.fabric.api.client.renderer.v1.mesh.MutableMesh;
import net.fabricmc.fabric.api.client.renderer.v1.mesh.MutableQuadView;
import net.fabricmc.fabric.api.client.renderer.v1.mesh.QuadAtlas;
import net.fabricmc.fabric.api.client.renderer.v1.mesh.QuadEmitter;
import net.fabricmc.fabric.api.client.renderer.v1.mesh.QuadTransform;
import net.fabricmc.fabric.api.client.renderer.v1.mesh.QuadView;
import net.fabricmc.fabric.api.client.renderer.v1.mesh.ShadeMode;
import net.fabricmc.fabric.api.client.renderer.v1.render.AltModelBlockRenderer;
import net.fabricmc.fabric.api.util.TriState;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.color.block.BlockColors;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.ARGB;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Sodium 0.8.7 is the newest runtime Sodium accepted by Iris 1.10.8 on 26.1,
 * but that Sodium jar declares a Fabric renderer without registering one.
 * This fallback only prevents Fabric Renderer API world hooks from crashing
 * in the Iris no-shaderpack profile. It is intentionally not a full renderer.
 */
public final class IrisSodiumFrapiFallbackRenderer implements Renderer {
    private static final Logger LOGGER = LoggerFactory.getLogger("IrisSodiumFrapiFallbackRenderer");
    private static boolean attemptedRegistration;

    private IrisSodiumFrapiFallbackRenderer() {}

    public static void registerIfNeeded() {
        if (attemptedRegistration) {
            return;
        }
        attemptedRegistration = true;

        FabricLoader loader = FabricLoader.getInstance();
        if (!loader.isDevelopmentEnvironment() || !loader.isModLoaded("iris") || !loader.isModLoaded("sodium")) {
            return;
        }

        try {
            Renderer.get();
            LOGGER.info("Fabric Renderer API provider already active; Iris/Sodium fallback not needed");
        }
        catch (UnsupportedOperationException e) {
            Renderer.register(new IrisSodiumFrapiFallbackRenderer());
            LOGGER.info("Registered minimal Fabric Renderer API fallback for Iris with Sodium 0.8.7");
        }
    }

    @Override
    public QuadEmitter quadEmitter(Consumer<? super MutableQuadView> consumer) {
        return new FallbackQuadEmitter(consumer);
    }

    @Override
    public MutableMesh mutableMesh() {
        return new FallbackMutableMesh();
    }

    @Override
    public AltModelBlockRenderer altModelBlockRenderer(
        boolean ambientOcclusion, boolean cull, BlockColors blockColors
    ) {
        return (output, x, y, z, level, pos, blockState, model, seed) -> {
            RandomSource random = RandomSource.create(seed);
            output.clear();
            output.pushTransform(quad -> {
                quad.translate(x, y, z);
                return true;
            });
            try {
                model.emitQuads(output, level, pos, blockState, random, cull ? direction -> false : direction -> false);
            }
            finally {
                output.popTransform();
            }
        };
    }

    private static class FallbackMutableMesh implements MutableMesh {
        private final List<FallbackQuad> quads = new ArrayList<>();
        private final FallbackQuadEmitter emitter = new FallbackQuadEmitter(quad -> quads.add(new FallbackQuad(quad)));

        @Override
        public QuadEmitter emitter() {
            return emitter;
        }

        @Override
        public void forEachMutable(Consumer<? super MutableQuadView> action) {
            quads.forEach(action);
        }

        @Override
        public Mesh immutableCopy() {
            List<FallbackQuad> copy = new ArrayList<>();
            for (FallbackQuad quad : quads) {
                copy.add(new FallbackQuad(quad));
            }
            return new FallbackMesh(copy);
        }

        @Override
        public void clear() {
            quads.clear();
            emitter.clear();
        }

        @Override
        public int size() {
            return quads.size();
        }

        @Override
        public void forEach(Consumer<? super QuadView> action) {
            quads.forEach(action);
        }

        @Override
        public void outputTo(QuadEmitter emitter) {
            for (FallbackQuad quad : quads) {
                emitter.copyFrom(quad).emit();
            }
        }
    }

    private record FallbackMesh(List<FallbackQuad> quads) implements Mesh {
        @Override
        public int size() {
            return quads.size();
        }

        @Override
        public void forEach(Consumer<? super QuadView> action) {
            quads.forEach(action);
        }

        @Override
        public void outputTo(QuadEmitter emitter) {
            for (FallbackQuad quad : quads) {
                emitter.copyFrom(quad).emit();
            }
        }
    }

    private static final class FallbackQuadEmitter extends FallbackQuad implements QuadEmitter {
        private final Consumer<? super MutableQuadView> consumer;
        private final ArrayDeque<QuadTransform> transforms = new ArrayDeque<>();

        FallbackQuadEmitter(Consumer<? super MutableQuadView> consumer) {
            this.consumer = consumer;
        }

        @Override
        public void pushTransform(QuadTransform transform) {
            transforms.push(transform);
        }

        @Override
        public void popTransform() {
            transforms.pop();
        }

        @Override
        public QuadEmitter emit() {
            for (QuadTransform transform : transforms) {
                if (!transform.transform(this)) {
                    return this;
                }
            }
            consumer.accept(this);
            clear();
            return this;
        }

        @Override
        public QuadEmitter pos(int vertexIndex, float x, float y, float z) {
            super.pos(vertexIndex, x, y, z);
            return this;
        }

        @Override
        public QuadEmitter color(int vertexIndex, int color) {
            super.color(vertexIndex, color);
            return this;
        }

        @Override
        public QuadEmitter uv(int vertexIndex, float u, float v) {
            super.uv(vertexIndex, u, v);
            return this;
        }

        @Override
        public QuadEmitter lightmap(int vertexIndex, int lightmap) {
            super.lightmap(vertexIndex, lightmap);
            return this;
        }

        @Override
        public QuadEmitter normal(int vertexIndex, float x, float y, float z) {
            super.normal(vertexIndex, x, y, z);
            return this;
        }

        @Override
        public QuadEmitter nominalFace(Direction face) {
            super.nominalFace(face);
            return this;
        }

        @Override
        public QuadEmitter cullFace(Direction face) {
            super.cullFace(face);
            return this;
        }

        @Override
        public QuadEmitter atlas(QuadAtlas quadAtlas) {
            super.atlas(quadAtlas);
            return this;
        }

        @Override
        public QuadEmitter chunkLayer(ChunkSectionLayer layer) {
            super.chunkLayer(layer);
            return this;
        }

        @Override
        public QuadEmitter itemRenderType(RenderType renderType) {
            super.itemRenderType(renderType);
            return this;
        }

        @Override
        public QuadEmitter emissive(boolean emissive) {
            super.emissive(emissive);
            return this;
        }

        @Override
        public QuadEmitter diffuseShade(boolean shade) {
            super.diffuseShade(shade);
            return this;
        }

        @Override
        public QuadEmitter ambientOcclusion(TriState ao) {
            super.ambientOcclusion(ao);
            return this;
        }

        @Override
        public QuadEmitter foilType(ItemStackRenderState.FoilType foilType) {
            super.foilType(foilType);
            return this;
        }

        @Override
        public QuadEmitter shadeMode(ShadeMode mode) {
            super.shadeMode(mode);
            return this;
        }

        @Override
        public QuadEmitter animated(boolean animated) {
            super.animated(animated);
            return this;
        }

        @Override
        public QuadEmitter tintIndex(int tintIndex) {
            super.tintIndex(tintIndex);
            return this;
        }

        @Override
        public QuadEmitter tag(int tag) {
            super.tag(tag);
            return this;
        }

        @Override
        public QuadEmitter copyFrom(QuadView quad) {
            super.copyFrom(quad);
            return this;
        }

        @Override
        public QuadEmitter fromBakedQuad(BakedQuad quad) {
            super.fromBakedQuad(quad);
            return this;
        }

        @Override
        public QuadEmitter clear() {
            super.clear();
            return this;
        }
    }

    private static class FallbackQuad implements MutableQuadView {
        private final Vector3f[] pos = {new Vector3f(), new Vector3f(), new Vector3f(), new Vector3f()};
        private final Vector2f[] uv = {new Vector2f(), new Vector2f(), new Vector2f(), new Vector2f()};
        private final int[] color = new int[4];
        private final int[] lightmap = new int[4];
        private final Vector3f[] normal = new Vector3f[4];
        private Direction nominalFace;
        private Direction cullFace;
        private QuadAtlas atlas;
        private ChunkSectionLayer chunkLayer;
        private RenderType itemRenderType;
        private boolean emissive;
        private boolean diffuseShade;
        private TriState ambientOcclusion;
        private ItemStackRenderState.FoilType foilType;
        private ShadeMode shadeMode;
        private boolean animated;
        private int tintIndex;
        private int tag;
        private final Vector3f faceNormal = new Vector3f(0, 1, 0);

        FallbackQuad() {
            clear();
        }

        FallbackQuad(QuadView source) {
            copyFrom(source);
        }

        @Override
        public MutableQuadView pos(int vertexIndex, float x, float y, float z) {
            pos[vertexIndex].set(x, y, z);
            updateFaceNormal();
            return this;
        }

        @Override
        public MutableQuadView color(int vertexIndex, int color) {
            this.color[vertexIndex] = color;
            return this;
        }

        @Override
        public MutableQuadView uv(int vertexIndex, float u, float v) {
            uv[vertexIndex].set(u, v);
            return this;
        }

        @Override
        public MutableQuadView lightmap(int vertexIndex, int lightmap) {
            this.lightmap[vertexIndex] = lightmap;
            return this;
        }

        @Override
        public MutableQuadView normal(int vertexIndex, float x, float y, float z) {
            normal[vertexIndex] = new Vector3f(x, y, z);
            return this;
        }

        @Override
        public MutableQuadView nominalFace(Direction face) {
            nominalFace = face;
            return this;
        }

        @Override
        public MutableQuadView cullFace(Direction face) {
            cullFace = face;
            nominalFace = face;
            return this;
        }

        @Override
        public MutableQuadView atlas(QuadAtlas quadAtlas) {
            atlas = quadAtlas;
            return this;
        }

        @Override
        public MutableQuadView chunkLayer(ChunkSectionLayer layer) {
            chunkLayer = layer;
            return this;
        }

        @Override
        public MutableQuadView itemRenderType(RenderType renderType) {
            itemRenderType = renderType;
            return this;
        }

        @Override
        public MutableQuadView emissive(boolean emissive) {
            this.emissive = emissive;
            return this;
        }

        @Override
        public MutableQuadView diffuseShade(boolean shade) {
            diffuseShade = shade;
            return this;
        }

        @Override
        public MutableQuadView ambientOcclusion(TriState ao) {
            ambientOcclusion = ao;
            return this;
        }

        @Override
        public MutableQuadView foilType(ItemStackRenderState.FoilType foilType) {
            this.foilType = foilType;
            return this;
        }

        @Override
        public MutableQuadView shadeMode(ShadeMode mode) {
            shadeMode = mode;
            return this;
        }

        @Override
        public MutableQuadView animated(boolean animated) {
            this.animated = animated;
            return this;
        }

        @Override
        public MutableQuadView tintIndex(int tintIndex) {
            this.tintIndex = tintIndex;
            return this;
        }

        @Override
        public MutableQuadView tag(int tag) {
            this.tag = tag;
            return this;
        }

        @Override
        public MutableQuadView copyFrom(QuadView quad) {
            for (int i = 0; i < 4; i++) {
                pos[i].set(quad.x(i), quad.y(i), quad.z(i));
                uv[i].set(quad.u(i), quad.v(i));
                color[i] = quad.color(i);
                lightmap[i] = quad.lightmap(i);
                normal[i] = quad.hasNormal(i) ? new Vector3f(quad.normalX(i), quad.normalY(i), quad.normalZ(i)) : null;
            }
            nominalFace = quad.nominalFace();
            cullFace = quad.cullFace();
            atlas = quad.atlas();
            chunkLayer = quad.chunkLayer();
            itemRenderType = quad.itemRenderType();
            emissive = quad.emissive();
            diffuseShade = quad.diffuseShade();
            ambientOcclusion = quad.ambientOcclusion();
            foilType = quad.foilType();
            shadeMode = quad.shadeMode();
            animated = quad.animated();
            tintIndex = quad.tintIndex();
            tag = quad.tag();
            updateFaceNormal();
            return this;
        }

        @Override
        public MutableQuadView fromBakedQuad(BakedQuad quad) {
            for (int i = 0; i < 4; i++) {
                Vector3fc p = quad.position(i);
                pos[i].set(p);
                long packedUv = quad.packedUV(i);
                uv[i].set(Float.intBitsToFloat((int) (packedUv >> 32)), Float.intBitsToFloat((int) packedUv));
                color[i] = 0xFFFFFFFF;
                lightmap[i] = 0;
                normal[i] = null;
            }
            nominalFace = quad.direction();
            cullFace = null;
            tintIndex = quad.materialInfo().tintIndex();
            diffuseShade = quad.materialInfo().shade();
            chunkLayer = quad.materialInfo().layer();
            itemRenderType = quad.materialInfo().itemRenderType();
            updateFaceNormal();
            return this;
        }

        @Override
        public MutableQuadView clear() {
            for (int i = 0; i < 4; i++) {
                pos[i].zero();
                uv[i].zero();
                color[i] = 0xFFFFFFFF;
                lightmap[i] = 0;
                normal[i] = null;
            }
            nominalFace = null;
            cullFace = null;
            atlas = QuadAtlas.BLOCK;
            chunkLayer = ChunkSectionLayer.CUTOUT;
            itemRenderType = Sheets.cutoutBlockItemSheet();
            emissive = false;
            diffuseShade = true;
            ambientOcclusion = TriState.DEFAULT;
            foilType = null;
            shadeMode = ShadeMode.ENHANCED;
            animated = false;
            tintIndex = -1;
            tag = 0;
            faceNormal.set(0, 1, 0);
            return this;
        }

        @Override public float x(int vertexIndex) { return pos[vertexIndex].x; }
        @Override public float y(int vertexIndex) { return pos[vertexIndex].y; }
        @Override public float z(int vertexIndex) { return pos[vertexIndex].z; }
        @Override public float posByIndex(int vertexIndex, int coordinateIndex) { return pos[vertexIndex].get(coordinateIndex); }
        @Override public Vector3f copyPos(int vertexIndex, Vector3f target) { return (target == null ? new Vector3f() : target).set(pos[vertexIndex]); }
        @Override public int color(int vertexIndex) { return color[vertexIndex]; }
        @Override public float u(int vertexIndex) { return uv[vertexIndex].x; }
        @Override public float v(int vertexIndex) { return uv[vertexIndex].y; }
        @Override public Vector2f copyUv(int vertexIndex, Vector2f target) { return (target == null ? new Vector2f() : target).set(uv[vertexIndex]); }
        @Override public int lightmap(int vertexIndex) { return lightmap[vertexIndex]; }
        @Override public boolean hasNormal(int vertexIndex) { return normal[vertexIndex] != null; }
        @Override public float normalX(int vertexIndex) { return hasNormal(vertexIndex) ? normal[vertexIndex].x : Float.NaN; }
        @Override public float normalY(int vertexIndex) { return hasNormal(vertexIndex) ? normal[vertexIndex].y : Float.NaN; }
        @Override public float normalZ(int vertexIndex) { return hasNormal(vertexIndex) ? normal[vertexIndex].z : Float.NaN; }
        @Override public Vector3f copyNormal(int vertexIndex, Vector3f target) {
            if (!hasNormal(vertexIndex)) {
                return null;
            }
            return (target == null ? new Vector3f() : target).set(normal[vertexIndex]);
        }
        @Override public Vector3fc faceNormal() { return faceNormal; }
        @Override public Direction lightFace() { return Direction.getApproximateNearest(faceNormal.x, faceNormal.y, faceNormal.z); }
        @Override public Direction nominalFace() { return nominalFace; }
        @Override public Direction cullFace() { return cullFace; }
        @Override public QuadAtlas atlas() { return atlas; }
        @Override public ChunkSectionLayer chunkLayer() { return chunkLayer; }
        @Override public RenderType itemRenderType() { return itemRenderType; }
        @Override public boolean emissive() { return emissive; }
        @Override public boolean diffuseShade() { return diffuseShade; }
        @Override public TriState ambientOcclusion() { return ambientOcclusion; }
        @Override public ItemStackRenderState.FoilType foilType() { return foilType; }
        @Override public ShadeMode shadeMode() { return shadeMode; }
        @Override public boolean animated() { return animated; }
        @Override public int tintIndex() { return tintIndex; }
        @Override public int tag() { return tag; }

        @Override
        public void buffer(int overlayCoords, VertexConsumer vertexConsumer) {
            for (int i = 0; i < 4; i++) {
                Vector3f n = normal[i] != null ? normal[i] : faceNormal;
                vertexConsumer
                    .addVertex(pos[i].x, pos[i].y, pos[i].z)
                    .setColor(color[i])
                    .setUv(uv[i].x, uv[i].y)
                    .setOverlay(overlayCoords)
                    .setLight(lightmap[i])
                    .setNormal(n.x, n.y, n.z);
            }
        }

        @Override
        public void buffer(int overlayCoords, PoseStack.Pose pose, VertexConsumer vertexConsumer) {
            for (int i = 0; i < 4; i++) {
                Vector3f n = normal[i] != null ? normal[i] : faceNormal;
                vertexConsumer
                    .addVertex(pose, pos[i].x, pos[i].y, pos[i].z)
                    .setColor(color[i])
                    .setUv(uv[i].x, uv[i].y)
                    .setOverlay(overlayCoords)
                    .setLight(lightmap[i])
                    .setNormal(pose, n.x, n.y, n.z);
            }
        }

        private void updateFaceNormal() {
            Vector3f a = new Vector3f(pos[1]).sub(pos[0]);
            Vector3f b = new Vector3f(pos[2]).sub(pos[0]);
            a.cross(b, faceNormal);
            if (faceNormal.lengthSquared() < 1.0E-6F) {
                faceNormal.set(0, 1, 0);
            }
            else {
                faceNormal.normalize();
            }
        }
    }
}
