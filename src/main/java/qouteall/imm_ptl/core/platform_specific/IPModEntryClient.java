package qouteall.imm_ptl.core.platform_specific;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EntityType;
import qouteall.imm_ptl.core.CHelper;
import qouteall.imm_ptl.core.IPGlobal;
import qouteall.imm_ptl.core.IPMcHelper;
import qouteall.imm_ptl.core.IPModMainClient;
import qouteall.imm_ptl.core.compat.IPModInfoChecking;
import qouteall.imm_ptl.core.compat.iris_compatibility.IrisSodiumFrapiFallbackRenderer;
import qouteall.imm_ptl.core.portal.BreakableMirror;
import qouteall.imm_ptl.core.portal.EndPortalEntity;
import qouteall.imm_ptl.core.portal.LoadingIndicatorEntity;
import qouteall.imm_ptl.core.portal.Mirror;
import qouteall.imm_ptl.core.portal.Portal;
import qouteall.imm_ptl.core.portal.global_portals.GlobalTrackedPortal;
import qouteall.imm_ptl.core.portal.global_portals.VerticalConnectingPortal;
import qouteall.imm_ptl.core.portal.global_portals.WorldWrappingPortal;
import qouteall.imm_ptl.core.portal.nether_portal.GeneralBreakablePortal;
import qouteall.imm_ptl.core.portal.nether_portal.NetherPortalEntity;
import qouteall.imm_ptl.core.render.LoadingIndicatorRenderer;
import qouteall.imm_ptl.core.render.PortalEntityRenderer;
import qouteall.q_misc_util.Helper;
import qouteall.q_misc_util.my_util.MyTaskList;

public class IPModEntryClient implements ClientModInitializer {
    private static final boolean AUTO_VISIBLE_TEST_PORTAL =
        "true".equalsIgnoreCase(System.getenv("IMM_PTL_AUTO_VISIBLE_TEST_PORTAL"));
    private static final boolean AUTO_MINIMAL_TRAVERSAL_TEST =
        "true".equalsIgnoreCase(System.getenv("IMM_PTL_AUTO_MINIMAL_TRAVERSAL_TEST"));
    private static final String AUTO_DIMENSION_TEST_PORTAL =
        System.getenv("IMM_PTL_AUTO_DIMENSION_TEST_PORTAL");
    private static final String AUTO_DIMENSION_TEST_SOURCE =
        System.getenv("IMM_PTL_AUTO_DIMENSION_TEST_SOURCE");
    private static final int AUTO_MINIMAL_TRAVERSAL_DELAY_TICKS =
        parseDevEnvInt("IMM_PTL_AUTO_MINIMAL_TRAVERSAL_DELAY_TICKS", -1);
    private static boolean autoVisibleTestPortalCommandSent;
    private static int autoVisibleTestPortalTicks;
    private static boolean loggedSodiumPortalPresence;
    private static int sodiumPortalPresenceTicks;
    private static boolean autoMinimalTraversalCommandSent;
    private static int autoMinimalTraversalTicks;
    
    
    
    public static void initPortalRenderers() {
        EntityRendererRegistry.register(Portal.ENTITY_TYPE, PortalEntityRenderer::new);
        EntityRendererRegistry.register(NetherPortalEntity.ENTITY_TYPE, PortalEntityRenderer::new);
        EntityRendererRegistry.register(EndPortalEntity.ENTITY_TYPE, PortalEntityRenderer::new);
        EntityRendererRegistry.register(Mirror.ENTITY_TYPE, PortalEntityRenderer::new);
        EntityRendererRegistry.register(BreakableMirror.ENTITY_TYPE, PortalEntityRenderer::new);
        EntityRendererRegistry.register(GlobalTrackedPortal.ENTITY_TYPE, PortalEntityRenderer::new);
        EntityRendererRegistry.register(WorldWrappingPortal.ENTITY_TYPE, PortalEntityRenderer::new);
        EntityRendererRegistry.register(VerticalConnectingPortal.ENTITY_TYPE, PortalEntityRenderer::new);
        EntityRendererRegistry.register(GeneralBreakablePortal.ENTITY_TYPE, PortalEntityRenderer::new);
        
        EntityRendererRegistry.register(
            LoadingIndicatorEntity.entityType,
            LoadingIndicatorRenderer::new
        );
        
    }
    
    @Override
    public void onInitializeClient() {
        IrisSodiumFrapiFallbackRenderer.registerIfNeeded();

        IPModMainClient.init();
        
        initPortalRenderers();
        
        Helper.log("Vanilla core profile initialized; optional compatibility mixins are resource-profile controlled");
        
        IPModInfoChecking.initClient();

        if (
            FabricLoader.getInstance().isDevelopmentEnvironment() &&
            (AUTO_VISIBLE_TEST_PORTAL || AUTO_DIMENSION_TEST_PORTAL != null || AUTO_MINIMAL_TRAVERSAL_TEST)
        ) {
            ClientTickEvents.END_CLIENT_TICK.register(IPModEntryClient::tickDevPortalTests);
        }
    }

    private static void tickDevPortalTests(Minecraft client) {
        if (client.level == null || client.player == null || client.getConnection() == null) {
            autoVisibleTestPortalTicks = 0;
            autoMinimalTraversalTicks = 0;
            return;
        }

        if ((AUTO_VISIBLE_TEST_PORTAL || AUTO_DIMENSION_TEST_PORTAL != null) && !autoVisibleTestPortalCommandSent) {
            autoVisibleTestPortalTicks++;
            if (autoVisibleTestPortalTicks >= 60) {
                autoVisibleTestPortalCommandSent = true;
                if (AUTO_DIMENSION_TEST_PORTAL != null && !AUTO_DIMENSION_TEST_PORTAL.isBlank()) {
                    if (AUTO_DIMENSION_TEST_SOURCE != null && !AUTO_DIMENSION_TEST_SOURCE.isBlank()) {
                        Helper.log(
                            "Running dev prepared dimension test from " +
                                AUTO_DIMENSION_TEST_SOURCE + " to " + AUTO_DIMENSION_TEST_PORTAL
                        );
                        client.getConnection().sendCommand(
                            "imm_ptl_debug prepare_dimension_test " +
                                AUTO_DIMENSION_TEST_SOURCE + " " + AUTO_DIMENSION_TEST_PORTAL
                        );
                    }
                    else {
                        Helper.log("Running dev auto dimension test portal command to " + AUTO_DIMENSION_TEST_PORTAL);
                        client.getConnection().sendCommand(
                            "imm_ptl_debug create_dimension_test_portal " + AUTO_DIMENSION_TEST_PORTAL
                        );
                    }
                }
                else {
                    Helper.log("Running dev auto visible test portal command");
                    client.getConnection().sendCommand("imm_ptl_debug create_visible_test_portal");
                }
            }
        }

        if (
            (AUTO_VISIBLE_TEST_PORTAL || AUTO_DIMENSION_TEST_PORTAL != null) && autoVisibleTestPortalCommandSent &&
            FabricLoader.getInstance().isModLoaded("sodium") && !loggedSodiumPortalPresence
        ) {
            sodiumPortalPresenceTicks++;
            if (sodiumPortalPresenceTicks >= 20) {
                loggedSodiumPortalPresence = true;
                boolean portalPresent = false;
                for (var entity : client.level.entitiesForRendering()) {
                    if (entity instanceof Portal) {
                        portalPresent = true;
                        break;
                    }
                }
                Helper.log("Portal present client-side under Sodium: " + portalPresent);
            }
        }

        if (AUTO_MINIMAL_TRAVERSAL_TEST && !autoMinimalTraversalCommandSent) {
            autoMinimalTraversalTicks++;
            int delay = AUTO_MINIMAL_TRAVERSAL_DELAY_TICKS > 0
                ? AUTO_MINIMAL_TRAVERSAL_DELAY_TICKS
                : (AUTO_VISIBLE_TEST_PORTAL || AUTO_DIMENSION_TEST_PORTAL != null) ? 140 : 80;
            if (autoMinimalTraversalTicks >= delay) {
                autoMinimalTraversalCommandSent = true;
                Helper.log("Running dev auto minimal portal traversal command");
                client.getConnection().sendCommand(
                    "imm_ptl_client_debug test_minimal_portal_traversal"
                );
            }
        }
    }

    private static int parseDevEnvInt(String name, int defaultValue) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            return defaultValue;
        }

        try {
            return Integer.parseInt(value);
        }
        catch (NumberFormatException e) {
            Helper.err("Invalid " + name + "=" + value + "; using " + defaultValue);
            return defaultValue;
        }
    }
}
