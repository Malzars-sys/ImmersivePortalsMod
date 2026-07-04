package qouteall.imm_ptl.api;

import net.minecraft.world.entity.Entity;

public enum PortalRemovalReason {
    COMMAND(Entity.RemovalReason.KILLED),
    DISCARDED(Entity.RemovalReason.DISCARDED),
    KILLED(Entity.RemovalReason.KILLED),
    UNLOADED_TO_CHUNK(Entity.RemovalReason.UNLOADED_TO_CHUNK);
    
    private final Entity.RemovalReason entityRemovalReason;
    
    PortalRemovalReason(Entity.RemovalReason entityRemovalReason) {
        this.entityRemovalReason = entityRemovalReason;
    }
    
    Entity.RemovalReason entityRemovalReason() {
        return entityRemovalReason;
    }
}
