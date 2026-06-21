package qouteall.imm_ptl.core.compat.mixin.sodium;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.world.entity.Entity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import qouteall.imm_ptl.core.portal.Portal;

/** Allows portal entities in sections for which Sodium has no terrain mesh. */
@Mixin(LevelRenderer.class)
public class MixinSodiumPortalLevelRenderer {
    @Unique
    private static final Logger IP_LOGGER =
        LoggerFactory.getLogger(MixinSodiumPortalLevelRenderer.class);
    @Unique
    private static boolean ip_loggedPortalSectionBypass;

    @ModifyExpressionValue(
        method = "extractVisibleEntities",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/LevelRenderer;" +
                "isSectionCompiledAndVisible(Lnet/minecraft/core/BlockPos;)Z"
        )
    )
    private boolean ip_allowPortalInUncompiledSection(
        boolean original,
        @Local Entity entity
    ) {
        if (entity instanceof Portal) {
            if (!ip_loggedPortalSectionBypass) {
                ip_loggedPortalSectionBypass = true;
                IP_LOGGER.info("Sodium portal section visibility bypass active: true");
            }
            return true;
        }
        return original;
    }
}
