package qouteall.imm_ptl.api;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.Nullable;
import qouteall.imm_ptl.core.portal.Portal;

import java.util.Optional;
import java.util.UUID;

public final class PortalApi {
    private PortalApi() {}
    
    public static void init() {
        PortalPersistentData.init();
    }
    
    public static PortalBuilder builder(MinecraftServer server) {
        return new PortalBuilder(server);
    }
    
    public static Optional<PortalHandle> findPortal(ServerLevel level, UUID entityId) {
        Portal portal = findPortalEntity(level, entityId);
        if (portal == null) {
            return Optional.empty();
        }
        return Optional.of(PortalPersistentData.createHandle(portal));
    }
    
    public static Optional<PortalHandle> findPortal(MinecraftServer server, PortalHandle handle) {
        ServerLevel level = server.getLevel(handle.dimension());
        if (level == null) {
            return Optional.empty();
        }
        return findPortal(level, handle.entityId());
    }
    
    public static boolean removePortal(
        MinecraftServer server,
        PortalHandle handle,
        PortalRemovalReason reason
    ) {
        ServerLevel level = server.getLevel(handle.dimension());
        if (level == null) {
            return false;
        }
        Portal portal = findPortalEntity(level, handle.entityId());
        if (portal == null) {
            return false;
        }
        
        portal.remove(reason.entityRemovalReason());
        return true;
    }
    
    static @Nullable Portal findPortalEntity(ServerLevel level, UUID entityId) {
        Entity entity = level.getEntity(entityId);
        if (entity instanceof Portal portal) {
            return portal;
        }
        return null;
    }
}
