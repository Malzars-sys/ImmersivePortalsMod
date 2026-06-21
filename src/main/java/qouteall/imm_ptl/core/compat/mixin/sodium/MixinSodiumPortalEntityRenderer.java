package qouteall.imm_ptl.core.compat.mixin.sodium;

import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.world.entity.Entity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import qouteall.imm_ptl.core.portal.Portal;

/**
 * Sodium rejects zero-volume portal entities before their renderer can submit
 * the minimal frame. Keep this bypass scoped to the Sodium runtime profile.
 */
@Mixin(EntityRenderer.class)
public class MixinSodiumPortalEntityRenderer {
    @Unique
    private static final Logger IP_LOGGER =
        LoggerFactory.getLogger(MixinSodiumPortalEntityRenderer.class);
    @Unique
    private static boolean ip_loggedPortalFrustumBypass;

    @Inject(method = "shouldRender", at = @At("HEAD"), cancellable = true)
    private void ip_allowPortalEntitySubmission(
        Entity entity,
        Frustum frustum,
        double cameraX,
        double cameraY,
        double cameraZ,
        CallbackInfoReturnable<Boolean> cir
    ) {
        if (entity instanceof Portal) {
            cir.setReturnValue(true);
            if (!ip_loggedPortalFrustumBypass) {
                ip_loggedPortalFrustumBypass = true;
                IP_LOGGER.info("Sodium portal entity frustum bypass active: true");
            }
        }
    }
}
