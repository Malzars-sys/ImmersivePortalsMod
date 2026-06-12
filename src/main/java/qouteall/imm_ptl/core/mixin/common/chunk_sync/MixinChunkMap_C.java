package qouteall.imm_ptl.core.mixin.common.chunk_sync;

import net.minecraft.server.level.ChunkHolder;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ThreadedLevelLightEngine;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import qouteall.imm_ptl.core.ducks.IEChunkMap;

@Mixin(value = ChunkMap.class, priority = 1100)
public abstract class MixinChunkMap_C implements IEChunkMap {
    
    @Shadow
    @Final
    private ServerLevel level;
    
    @Shadow
    protected abstract ChunkHolder getVisibleChunkIfPresent(long long_1);
    
    @Shadow
    @Final
    private ThreadedLevelLightEngine lightEngine;
    
    @Shadow
    abstract int getPlayerViewDistance(ServerPlayer serverPlayer);
    
    @Override
    public int ip_getPlayerViewDistance(ServerPlayer player) {
        return getPlayerViewDistance(player);
    }
    
    @Override
    public ServerLevel ip_getWorld() {
        return level;
    }
    
    @Override
    public ThreadedLevelLightEngine ip_getLightingProvider() {
        return lightEngine;
    }
    
    @Override
    public ChunkHolder ip_getChunkHolder(long chunkPosLong) {
        return getVisibleChunkIfPresent(chunkPosLong);
    }
    
}
