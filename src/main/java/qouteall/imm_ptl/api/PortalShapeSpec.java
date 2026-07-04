package qouteall.imm_ptl.api;

import net.minecraft.world.phys.Vec3;

/**
 * Stable high-level shape descriptions for public API consumers.
 * These are translated to internal portal geometry by PortalBuilder.
 */
public sealed interface PortalShapeSpec permits
    PortalShapeSpec.Rectangle,
    PortalShapeSpec.HorizontalRectangle,
    PortalShapeSpec.ExplicitAxes {
    
    double width();
    
    double height();
    
    default void validate() {
        if (!(width() > 0) || !(height() > 0)) {
            throw new IllegalArgumentException("Portal shape width and height must be positive");
        }
    }
    
    record Rectangle(double width, double height) implements PortalShapeSpec {}
    
    record HorizontalRectangle(double width, double height) implements PortalShapeSpec {}
    
    record ExplicitAxes(
        Vec3 axisW,
        Vec3 axisH,
        double width,
        double height
    ) implements PortalShapeSpec {
        @Override
        public void validate() {
            PortalShapeSpec.super.validate();
            if (axisW == null || axisH == null) {
                throw new IllegalArgumentException("Explicit portal axes must not be null");
            }
            if (axisW.lengthSqr() < 1.0e-8 || axisH.lengthSqr() < 1.0e-8) {
                throw new IllegalArgumentException("Explicit portal axes must not be zero");
            }
            if (axisW.normalize().cross(axisH.normalize()).lengthSqr() < 1.0e-8) {
                throw new IllegalArgumentException("Explicit portal axes must not be parallel");
            }
        }
    }
}
