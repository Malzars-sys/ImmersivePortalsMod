package qouteall.imm_ptl.api;

/**
 * Stable teleport-related switches backed by existing Portal fields.
 */
public record PortalTeleportOptions(
    boolean teleportable,
    boolean interactable,
    boolean crossPortalCollision,
    boolean preserveEntityScale,
    boolean preserveGravity
) {
    public static PortalTeleportOptions defaults() {
        return new PortalTeleportOptions(true, true, true, true, true);
    }
}
