package qouteall.imm_ptl.api;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import qouteall.imm_ptl.core.McHelper;
import qouteall.imm_ptl.core.portal.Portal;
import qouteall.imm_ptl.core.portal.PortalManipulation;

import java.util.Optional;

public final class PortalBuilder {
    private final MinecraftServer server;
    
    private @Nullable Identifier owner;
    private @Nullable ServerLevel sourceLevel;
    private @Nullable Vec3 sourcePosition;
    private @Nullable ServerLevel targetLevel;
    private @Nullable Vec3 targetPosition;
    private PortalShapeSpec shape = new PortalShapeSpec.Rectangle(2.0, 3.0);
    private PortalVisualOptions visual = PortalVisualOptions.defaults();
    private PortalTeleportOptions teleport = PortalTeleportOptions.defaults();
    private @Nullable String sourceAnchorId;
    private @Nullable String targetAnchorId;
    
    PortalBuilder(MinecraftServer server) {
        this.server = server;
    }
    
    public PortalBuilder owner(Identifier owner) {
        this.owner = owner;
        return this;
    }
    
    public PortalBuilder source(ServerLevel level, Vec3 position) {
        this.sourceLevel = level;
        this.sourcePosition = position;
        return this;
    }
    
    public PortalBuilder target(ServerLevel level, Vec3 position) {
        this.targetLevel = level;
        this.targetPosition = position;
        return this;
    }
    
    public PortalBuilder shape(PortalShapeSpec shape) {
        this.shape = shape;
        return this;
    }
    
    public PortalBuilder visual(PortalVisualOptions visual) {
        this.visual = visual;
        return this;
    }
    
    public PortalBuilder teleport(PortalTeleportOptions teleport) {
        this.teleport = teleport;
        return this;
    }
    
    public PortalBuilder sourceAnchorId(@Nullable String sourceAnchorId) {
        this.sourceAnchorId = sourceAnchorId;
        return this;
    }
    
    public PortalBuilder targetAnchorId(@Nullable String targetAnchorId) {
        this.targetAnchorId = targetAnchorId;
        return this;
    }
    
    public PortalCreationResult create() {
        try {
            validate();
            Portal primary = createPortal(sourceLevel, sourcePosition, targetLevel, targetPosition);
            return new PortalCreationResult.Success(
                PortalPersistentData.createHandle(primary),
                Optional.empty()
            );
        }
        catch (Throwable e) {
            return new PortalCreationResult.Failure(Component.literal(e.getMessage()));
        }
    }
    
    public PortalCreationResult createLinkedPair() {
        try {
            validate();
            Portal primary = createPortal(sourceLevel, sourcePosition, targetLevel, targetPosition);
            Portal reverse = PortalManipulation.createReversePortal(primary, Portal.ENTITY_TYPE);
            applyPersistentData(reverse, targetAnchorId, sourceAnchorId);
            applyVisualOptions(reverse);
            applyTeleportOptions(reverse);
            McHelper.spawnServerEntity(reverse);
            
            return new PortalCreationResult.Success(
                PortalPersistentData.createHandle(primary),
                Optional.of(PortalPersistentData.createHandle(reverse))
            );
        }
        catch (Throwable e) {
            return new PortalCreationResult.Failure(Component.literal(e.getMessage()));
        }
    }
    
    private void validate() {
        if (server == null) {
            throw new IllegalStateException("MinecraftServer must not be null");
        }
        if (owner == null) {
            throw new IllegalStateException("Portal owner must be specified");
        }
        if (sourceLevel == null || targetLevel == null) {
            throw new IllegalStateException("Source and target levels must be specified");
        }
        if (sourcePosition == null || targetPosition == null) {
            throw new IllegalStateException("Source and target positions must be specified");
        }
        if (sourceLevel.isClientSide() || targetLevel.isClientSide()) {
            throw new IllegalStateException("Portal API creation must run on the logical server");
        }
        if (sourceLevel.getServer() != server || targetLevel.getServer() != server) {
            throw new IllegalStateException("Source and target levels must belong to the builder server");
        }
        if (shape == null) {
            throw new IllegalStateException("Portal shape must be specified");
        }
        shape.validate();
        if (visual == null) {
            visual = PortalVisualOptions.defaults();
        }
        if (teleport == null) {
            teleport = PortalTeleportOptions.defaults();
        }
    }
    
    private Portal createPortal(
        ServerLevel fromLevel,
        Vec3 fromPos,
        ServerLevel toLevel,
        Vec3 toPos
    ) {
        Portal portal = Portal.ENTITY_TYPE.create(fromLevel, EntitySpawnReason.COMMAND);
        if (portal == null) {
            throw new IllegalStateException("Failed to create portal entity");
        }
        
        portal.setOriginPos(fromPos);
        portal.setDestinationDimension(toLevel.dimension());
        portal.setDestination(toPos);
        applyShape(portal, fromPos, toPos);
        applyVisualOptions(portal);
        applyTeleportOptions(portal);
        applyPersistentData(portal, sourceAnchorId, targetAnchorId);
        McHelper.spawnServerEntity(portal);
        return portal;
    }
    
    private void applyShape(Portal portal, Vec3 fromPos, Vec3 toPos) {
        Vec3 axisW;
        Vec3 axisH;
        
        if (shape instanceof PortalShapeSpec.HorizontalRectangle horizontal) {
            axisW = new Vec3(1, 0, 0);
            axisH = new Vec3(0, 0, -1);
            portal.setOrientationAndSize(axisW, axisH, horizontal.width(), horizontal.height());
            return;
        }
        
        if (shape instanceof PortalShapeSpec.ExplicitAxes explicitAxes) {
            portal.setOrientationAndSize(
                explicitAxes.axisW(),
                explicitAxes.axisH(),
                explicitAxes.width(),
                explicitAxes.height()
            );
            return;
        }
        
        PortalShapeSpec.Rectangle rectangle = (PortalShapeSpec.Rectangle) shape;
        Vec3 direction = toPos.subtract(fromPos);
        Vec3 horizontalDirection = new Vec3(direction.x, 0, direction.z);
        if (horizontalDirection.lengthSqr() < 1.0e-8) {
            horizontalDirection = new Vec3(0, 0, 1);
        }
        Vec3 normal = horizontalDirection.normalize();
        axisW = new Vec3(normal.z, 0, -normal.x).normalize();
        axisH = new Vec3(0, 1, 0);
        portal.setOrientationAndSize(axisW, axisH, rectangle.width(), rectangle.height());
    }
    
    private void applyVisualOptions(Portal portal) {
        boolean visible = visual.visible() && visual.style() != PortalVisualOptions.Style.INVISIBLE;
        portal.setIsVisible(visible);
    }
    
    private void applyTeleportOptions(Portal portal) {
        portal.setTeleportable(teleport.teleportable());
        portal.setInteractable(teleport.interactable());
        portal.setCrossPortalCollisionEnabled(teleport.crossPortalCollision());
        portal.setTeleportChangesScale(!teleport.preserveEntityScale());
        portal.setTeleportChangesGravity(!teleport.preserveGravity());
    }
    
    private void applyPersistentData(
        Portal portal,
        @Nullable String sourceAnchorId,
        @Nullable String targetAnchorId
    ) {
        PortalPersistentData.set(
            portal,
            new PortalPersistentData(
                PortalPersistentData.CURRENT_API_VERSION,
                owner,
                sourceAnchorId,
                targetAnchorId,
                visual
            )
        );
        portal.portalTag = owner.toString();
    }
}
