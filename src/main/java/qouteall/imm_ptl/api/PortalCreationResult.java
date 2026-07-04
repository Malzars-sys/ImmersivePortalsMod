package qouteall.imm_ptl.api;

import net.minecraft.network.chat.Component;

import java.util.Optional;

public sealed interface PortalCreationResult permits
    PortalCreationResult.Success,
    PortalCreationResult.Failure {
    
    record Success(
        PortalHandle primary,
        Optional<PortalHandle> reverse
    ) implements PortalCreationResult {}
    
    record Failure(Component reason) implements PortalCreationResult {}
}
