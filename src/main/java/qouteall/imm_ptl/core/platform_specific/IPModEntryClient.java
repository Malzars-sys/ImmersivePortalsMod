package qouteall.imm_ptl.core.platform_specific;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EntityType;
import qouteall.imm_ptl.core.CHelper;
import qouteall.imm_ptl.core.IPGlobal;
import qouteall.imm_ptl.core.IPMcHelper;
import qouteall.imm_ptl.core.IPModMainClient;
import qouteall.imm_ptl.core.compat.IPModInfoChecking;
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
        IPModMainClient.init();
        
        initPortalRenderers();
        
        Helper.log("Vanilla core profile: Sodium and Iris compatibility disabled");
        
        IPModInfoChecking.initClient();
    }
    
}
