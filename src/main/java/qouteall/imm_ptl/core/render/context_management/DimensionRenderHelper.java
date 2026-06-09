package qouteall.imm_ptl.core.render.context_management;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.Lightmap;
import net.minecraft.world.level.Level;
import qouteall.imm_ptl.core.ducks.IEGameRenderer;
import qouteall.q_misc_util.Helper;

public class DimensionRenderHelper {
    private static final Minecraft client = Minecraft.getInstance();
    public final Level world;
    
    public final Lightmap lightmapTexture;
    
    public DimensionRenderHelper(Level world) {
        this.world = world;
        
        if (client.level == world) {
            lightmapTexture = ((IEGameRenderer) client.gameRenderer).ip_getLightmap();
        }
        else {
            lightmapTexture = new Lightmap();
            Helper.log("Created lightmap texture for " + world.dimension().identifier());
        }
    }
    
    public void tick() {
        // Lightmap state is extracted and rendered by GameRenderer in 26.1.
    }
    
    public void cleanUp() {
        if (lightmapTexture != ((IEGameRenderer) client.gameRenderer).ip_getLightmap()) {
            lightmapTexture.close();
        }
    }
    
}
