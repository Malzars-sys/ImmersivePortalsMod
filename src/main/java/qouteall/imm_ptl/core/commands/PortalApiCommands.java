package qouteall.imm_ptl.core.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.logging.LogUtils;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import qouteall.imm_ptl.api.PortalApi;
import qouteall.imm_ptl.api.PortalCreationResult;
import qouteall.imm_ptl.api.PortalHandle;
import qouteall.imm_ptl.api.PortalPersistentData;
import qouteall.imm_ptl.api.PortalRemovalReason;
import qouteall.imm_ptl.api.PortalShapeSpec;
import qouteall.imm_ptl.api.PortalTeleportOptions;
import qouteall.imm_ptl.api.PortalVisualOptions;
import qouteall.imm_ptl.core.McHelper;
import qouteall.imm_ptl.core.portal.Portal;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public final class PortalApiCommands {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Identifier COMMAND_OWNER = McHelper.newIdentifier("imm_ptl", "command_api");
    private static final Identifier FLOOR_COMMAND_OWNER = McHelper.newIdentifier("imm_ptl", "command_api_floor");
    private static final double DEFAULT_DISTANCE = 10.0;
    private static final double DEFAULT_WIDTH = 2.0;
    private static final double DEFAULT_HEIGHT = 3.0;
    private static final double DEFAULT_FLOOR_SIZE = 3.0;
    private static final double DEFAULT_INSPECT_RADIUS = 16.0;
    private static final double DEFAULT_LIST_RADIUS = 32.0;

    private PortalApiCommands() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        LOGGER.info("Registering PortalApiCommands");
        dispatcher.register(Commands.literal("imm_ptl_api")
            .requires(source -> PortalCommand.hasPermissionLevel(source, 2))
            .then(withPortalSizeArgs(
                Commands.literal("create_forward"),
                PortalApiCommands::createForward,
                DEFAULT_DISTANCE,
                DEFAULT_WIDTH,
                DEFAULT_HEIGHT
            ))
            .then(withPortalSizeArgs(
                Commands.literal("create_linked_forward"),
                PortalApiCommands::createLinkedForward,
                DEFAULT_DISTANCE,
                DEFAULT_WIDTH,
                DEFAULT_HEIGHT
            ))
            .then(withFloorArgs(Commands.literal("create_floor_linked")))
            .then(Commands.literal("inspect_nearest")
                .executes(context -> inspectNearest(
                    context.getSource().getPlayerOrException(),
                    DEFAULT_INSPECT_RADIUS
                ))
                .then(Commands.argument("radius", DoubleArgumentType.doubleArg(1.0))
                    .executes(context -> inspectNearest(
                        context.getSource().getPlayerOrException(),
                        DoubleArgumentType.getDouble(context, "radius")
                    ))
                )
            )
            .then(Commands.literal("list_nearby")
                .executes(context -> listNearby(
                    context.getSource().getPlayerOrException(),
                    DEFAULT_LIST_RADIUS
                ))
                .then(Commands.argument("radius", DoubleArgumentType.doubleArg(1.0))
                    .executes(context -> listNearby(
                        context.getSource().getPlayerOrException(),
                        DoubleArgumentType.getDouble(context, "radius")
                    ))
                )
            )
            .then(Commands.literal("remove_nearest")
                .executes(context -> removeNearest(
                    context.getSource().getPlayerOrException(),
                    DEFAULT_INSPECT_RADIUS
                ))
                .then(Commands.argument("radius", DoubleArgumentType.doubleArg(1.0))
                    .executes(context -> removeNearest(
                        context.getSource().getPlayerOrException(),
                        DoubleArgumentType.getDouble(context, "radius")
                    ))
                )
            )
        );
    }

    private static LiteralArgumentBuilder<CommandSourceStack> withPortalSizeArgs(
        LiteralArgumentBuilder<CommandSourceStack> builder,
        PortalSizeCommand command,
        double defaultDistance,
        double defaultWidth,
        double defaultHeight
    ) {
        return builder
            .executes(context -> command.run(
                context.getSource().getPlayerOrException(),
                defaultDistance,
                defaultWidth,
                defaultHeight
            ))
            .then(Commands.argument("distance", DoubleArgumentType.doubleArg(1.0))
                .executes(context -> command.run(
                    context.getSource().getPlayerOrException(),
                    DoubleArgumentType.getDouble(context, "distance"),
                    defaultWidth,
                    defaultHeight
                ))
                .then(Commands.argument("width", DoubleArgumentType.doubleArg(0.1))
                    .executes(context -> command.run(
                        context.getSource().getPlayerOrException(),
                        DoubleArgumentType.getDouble(context, "distance"),
                        DoubleArgumentType.getDouble(context, "width"),
                        defaultHeight
                    ))
                    .then(Commands.argument("height", DoubleArgumentType.doubleArg(0.1))
                        .executes(context -> command.run(
                            context.getSource().getPlayerOrException(),
                            DoubleArgumentType.getDouble(context, "distance"),
                            DoubleArgumentType.getDouble(context, "width"),
                            DoubleArgumentType.getDouble(context, "height")
                        ))
                    )
                )
            );
    }

    private static LiteralArgumentBuilder<CommandSourceStack> withFloorArgs(
        LiteralArgumentBuilder<CommandSourceStack> builder
    ) {
        return builder
            .executes(context -> createFloorLinked(
                context.getSource().getPlayerOrException(),
                DEFAULT_DISTANCE,
                DEFAULT_FLOOR_SIZE
            ))
            .then(Commands.argument("distance", DoubleArgumentType.doubleArg(1.0))
                .executes(context -> createFloorLinked(
                    context.getSource().getPlayerOrException(),
                    DoubleArgumentType.getDouble(context, "distance"),
                    DEFAULT_FLOOR_SIZE
                ))
                .then(Commands.argument("size", DoubleArgumentType.doubleArg(0.1))
                    .executes(context -> createFloorLinked(
                        context.getSource().getPlayerOrException(),
                        DoubleArgumentType.getDouble(context, "distance"),
                        DoubleArgumentType.getDouble(context, "size")
                    ))
                )
            );
    }

    private static int createForward(
        ServerPlayer player,
        double distance,
        double width,
        double height
    ) {
        ServerLevel level = player.level();
        Vec3 source = getForwardPortalOrigin(player);
        Vec3 target = source.add(getHorizontalFacing(player).scale(distance));

        PortalCreationResult result = PortalApi.builder(level.getServer())
            .owner(COMMAND_OWNER)
            .source(level, source)
            .target(level, target)
            .shape(new PortalShapeSpec.Rectangle(width, height))
            .visual(PortalVisualOptions.defaults())
            .teleport(PortalTeleportOptions.defaults())
            .sourceAnchorId("command_forward_source")
            .targetAnchorId("command_forward_target")
            .create();

        return reportCreation(player, result, "create_forward", source, target);
    }

    private static int createLinkedForward(
        ServerPlayer player,
        double distance,
        double width,
        double height
    ) {
        ServerLevel level = player.level();
        Vec3 source = getForwardPortalOrigin(player);
        Vec3 target = source.add(getHorizontalFacing(player).scale(distance));

        PortalCreationResult result = PortalApi.builder(level.getServer())
            .owner(COMMAND_OWNER)
            .source(level, source)
            .target(level, target)
            .shape(new PortalShapeSpec.Rectangle(width, height))
            .visual(PortalVisualOptions.defaults())
            .teleport(PortalTeleportOptions.defaults())
            .sourceAnchorId("command_linked_source")
            .targetAnchorId("command_linked_target")
            .createLinkedPair();

        return reportCreation(player, result, "create_linked_forward", source, target);
    }

    private static int createFloorLinked(
        ServerPlayer player,
        double distance,
        double size
    ) {
        ServerLevel level = player.level();
        Vec3 source = player.position().add(0, 0.05, 0);
        Vec3 target = source.add(getHorizontalFacing(player).scale(distance));

        PortalCreationResult result = PortalApi.builder(level.getServer())
            .owner(FLOOR_COMMAND_OWNER)
            .source(level, source)
            .target(level, target)
            .shape(new PortalShapeSpec.HorizontalRectangle(size, size))
            .visual(new PortalVisualOptions(
                true,
                0xAA55FFFF,
                PortalVisualOptions.Style.SIMPLE_MAGIC
            ))
            .teleport(PortalTeleportOptions.defaults())
            .sourceAnchorId("command_floor_source")
            .targetAnchorId("command_floor_target")
            .createLinkedPair();

        return reportCreation(player, result, "create_floor_linked", source, target);
    }

    private static int inspectNearest(ServerPlayer player, double radius) {
        Portal portal = findNearestPortal(player, radius);
        if (portal == null) {
            player.sendSystemMessage(Component.literal("No portal found within %.1f blocks.".formatted(radius)));
            return 0;
        }

        player.sendSystemMessage(Component.literal("Nearest portal:"));
        sendPortalDetails(player, portal, true);
        return 1;
    }

    private static int listNearby(ServerPlayer player, double radius) {
        List<Portal> portals = getNearbyPortals(player, radius).stream()
            .sorted(Comparator.comparingDouble(portal -> portal.distanceToSqr(player)))
            .toList();

        player.sendSystemMessage(Component.literal(
            "Found %d portal(s) within %.1f blocks.".formatted(portals.size(), radius)
        ));
        for (Portal portal : portals.stream().limit(12).toList()) {
            PortalPersistentData data = PortalPersistentData.get(portal);
            player.sendSystemMessage(Component.literal(
                "- %s owner=%s distance=%.2f dest=%s".formatted(
                    shortId(portal),
                    data != null ? data.owner() : "<no imm_ptl_public_api>",
                    Math.sqrt(portal.distanceToSqr(player)),
                    portal.getDestDim().identifier()
                )
            ));
        }
        if (portals.size() > 12) {
            player.sendSystemMessage(Component.literal("... truncated to 12 entries."));
        }
        return portals.size();
    }

    private static int removeNearest(ServerPlayer player, double radius) {
        Portal portal = findNearestPortal(player, radius);
        if (portal == null) {
            player.sendSystemMessage(Component.literal("No portal found within %.1f blocks.".formatted(radius)));
            return 0;
        }

        PortalHandle handle = createHandle(portal);
        boolean removed = PortalApi.removePortal(
            player.level().getServer(),
            handle,
            PortalRemovalReason.COMMAND
        );

        player.sendSystemMessage(Component.literal(
            "PortalApi.removePortal %s for %s owner=%s".formatted(
                removed ? "succeeded" : "failed",
                handle.entityId(),
                handle.owner()
            )
        ));
        return removed ? 1 : 0;
    }

    private static int reportCreation(
        ServerPlayer player,
        PortalCreationResult result,
        String commandName,
        Vec3 source,
        Vec3 target
    ) {
        if (result instanceof PortalCreationResult.Failure failure) {
            player.sendSystemMessage(Component.literal(
                "PortalApi %s failed: %s".formatted(commandName, failure.reason().getString())
            ));
            return 0;
        }

        PortalCreationResult.Success success = (PortalCreationResult.Success) result;
        player.sendSystemMessage(Component.literal(
            "PortalApi %s success primary=%s reverse=%s source=%s target=%s".formatted(
                commandName,
                success.primary().entityId(),
                success.reverse().map(PortalHandle::entityId).map(Object::toString).orElse("<none>"),
                source,
                target
            )
        ));
        LOGGER.info(
            "PortalApi command {} success primary={} reverse={} source={} target={}",
            commandName, success.primary(), success.reverse().orElse(null), source, target
        );
        return 1;
    }

    private static void sendPortalDetails(ServerPlayer player, Portal portal, boolean verbose) {
        PortalPersistentData data = PortalPersistentData.get(portal);
        player.sendSystemMessage(Component.literal("uuid=" + portal.getUUID()));
        player.sendSystemMessage(Component.literal("dimension=" + portal.level().dimension().identifier()));
        player.sendSystemMessage(Component.literal("destinationDimension=" + portal.getDestDim().identifier()));
        player.sendSystemMessage(Component.literal("destinationPosition=" + portal.getDestPos()));
        player.sendSystemMessage(Component.literal("has imm_ptl_public_api=" + (data != null)));
        if (data != null) {
            player.sendSystemMessage(Component.literal("owner=" + data.owner()));
            player.sendSystemMessage(Component.literal("sourceAnchorId=" + nullableText(data.sourceAnchorId())));
            player.sendSystemMessage(Component.literal("targetAnchorId=" + nullableText(data.targetAnchorId())));
            player.sendSystemMessage(Component.literal("visual=" + data.visualOptions()));
        }
        if (verbose) {
            player.sendSystemMessage(Component.literal("teleportable=" + portal.isTeleportable()));
            player.sendSystemMessage(Component.literal("interactable=" + portal.isInteractable()));
            player.sendSystemMessage(Component.literal("crossPortalCollision=" + portal.getHasCrossPortalCollision()));
        }
    }

    private static PortalHandle createHandle(Portal portal) {
        PortalPersistentData data = PortalPersistentData.get(portal);
        Identifier owner = data != null
            ? data.owner()
            : McHelper.newIdentifier("immersive_portals", "unknown_owner");
        return new PortalHandle(
            portal.getUUID(),
            portal.level().dimension(),
            owner,
            data != null ? data.sourceAnchorId() : null,
            data != null ? data.targetAnchorId() : null
        );
    }

    private static @Nullable Portal findNearestPortal(ServerPlayer player, double radius) {
        return getNearbyPortals(player, radius).stream()
            .min(Comparator.comparingDouble(portal -> portal.distanceToSqr(player)))
            .orElse(null);
    }

    private static List<Portal> getNearbyPortals(ServerPlayer player, double radius) {
        return McHelper.getEntitiesNearby(player.level(), player.position(), Portal.class, radius).stream()
            .filter(portal -> portal.distanceToSqr(player) <= radius * radius)
            .toList();
    }

    private static Vec3 getForwardPortalOrigin(ServerPlayer player) {
        return player.position().add(getHorizontalFacing(player).scale(4.0)).add(0, 1.5, 0);
    }

    private static Vec3 getHorizontalFacing(ServerPlayer player) {
        Vec3 view = player.getLookAngle();
        Vec3 horizontal = new Vec3(view.x, 0, view.z);
        if (horizontal.lengthSqr() < 1.0e-8) {
            return Vec3.atLowerCornerOf(player.getDirection().getUnitVec3i());
        }
        return horizontal.normalize();
    }

    private static String shortId(Portal portal) {
        return portal.getUUID().toString().substring(0, 8);
    }

    private static String nullableText(@Nullable String value) {
        return value != null ? value : "<none>";
    }

    @FunctionalInterface
    private interface PortalSizeCommand {
        int run(ServerPlayer player, double distance, double width, double height);
    }
}
