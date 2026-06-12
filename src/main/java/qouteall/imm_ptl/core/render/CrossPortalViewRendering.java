package qouteall.imm_ptl.core.render;

import com.mojang.datafixers.util.Pair;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import qouteall.imm_ptl.core.ClientWorldLoader;
import qouteall.imm_ptl.core.IPCGlobal;
import qouteall.imm_ptl.core.IPGlobal;
import qouteall.imm_ptl.core.ScaleUtils;
import qouteall.imm_ptl.core.commands.PortalCommand;
import qouteall.imm_ptl.core.ducks.IECamera;
import qouteall.imm_ptl.core.portal.Portal;
import qouteall.imm_ptl.core.render.context_management.RenderStates;
import qouteall.imm_ptl.core.render.context_management.WorldRenderInfo;
import qouteall.imm_ptl.core.teleportation.ClientTeleportationManager;

// in third person view it may render cross portal.
// if view bobbing make the camera go through portal before the actual player go through portal,
//  it will also render cross-portal
public class CrossPortalViewRendering {
    public static final Minecraft client = Minecraft.getInstance();
    
    // if rendered, return true
    public static boolean renderCrossPortalView() {
        // Deferred until the 26.1 extracted camera render state can be rebuilt.
        return false;
    }
    
    private static boolean isFrontView() {
        return client.options.getCameraType().isMirrored();
    }
    
    private static boolean isThirdPerson() {
        return !client.options.getCameraType().isFirstPerson();
    }
    
    /**
     * {@link Camera#getMaxZoom}
     */
    @SuppressWarnings("JavadocReference")
    private static Vec3 getThirdPersonCameraPos(Vec3 endPos, Portal portal, Vec3 startPos) {
        Vec3 rtStart = portal.transformPoint(startPos);
        Vec3 rtEnd = portal.transformPoint(endPos);
        Entity cameraEntity = client.getCameraEntity();
        assert cameraEntity != null;
        BlockHitResult blockHitResult = portal.getDestinationWorld().clip(
            new ClipContext(
                rtStart,
                rtEnd,
                ClipContext.Block.VISUAL,
                ClipContext.Fluid.NONE,
                cameraEntity
            )
        );
        
        if (blockHitResult.getType() == HitResult.Type.BLOCK) {
            return rtStart.add(rtEnd.subtract(rtStart).normalize().scale(
                getThirdPersonMaxDistance()
            ));
        }
        
        return blockHitResult.getLocation();
    }
    
    private static double getThirdPersonMaxDistance() {
        return 4.0d * ScaleUtils.computeThirdPersonScale(client.player);
    }
    
    //    private static Vec3d getThirdPersonCameraPos(Portal portalHit, Camera resuableCamera) {
//        return CHelper.withWorldSwitched(
//            client.cameraEntity,
//            portalHit,
//            () -> {
//                World destinationWorld = portalHit.getDestinationWorld();
//                resuableCamera.update(
//                    destinationWorld,
//                    client.cameraEntity,
//                    true,
//                    isInverseView(),
//                    RenderStates.tickDelta
//                );
//                return resuableCamera.getPos();
//            }
//        );
//    }
}
