package qouteall.imm_ptl.api;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.Nullable;
import qouteall.imm_ptl.core.McHelper;
import qouteall.imm_ptl.core.portal.Portal;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * Minimal public API metadata persisted on portal NBT.
 * This stores only stable owner/link identifiers, not renderer state.
 */
public record PortalPersistentData(
    int apiVersion,
    Identifier owner,
    @Nullable String sourceAnchorId,
    @Nullable String targetAnchorId,
    PortalVisualOptions visualOptions
) {
    public static final int CURRENT_API_VERSION = 1;
    private static final String TAG_KEY = "imm_ptl_public_api";
    private static final Map<Portal, PortalPersistentData> DATA = new WeakHashMap<>();
    private static boolean initialized;
    
    public static synchronized void init() {
        if (initialized) {
            return;
        }
        initialized = true;
        
        Portal.READ_PORTAL_DATA_SIGNAL.register((portal, tag) -> {
            PortalPersistentData data = read(tag);
            if (data != null) {
                DATA.put(portal, data);
            }
        });
        
        Portal.WRITE_PORTAL_DATA_SIGNAL.register((portal, tag) -> {
            PortalPersistentData data = DATA.get(portal);
            if (data != null) {
                tag.put(TAG_KEY, data.toTag());
            }
        });
    }
    
    public static void set(Portal portal, PortalPersistentData data) {
        DATA.put(portal, data);
    }
    
    public static @Nullable PortalPersistentData get(Portal portal) {
        return DATA.get(portal);
    }
    
    static PortalHandle createHandle(Portal portal) {
        PortalPersistentData data = DATA.get(portal);
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
    
    private CompoundTag toTag() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("apiVersion", apiVersion);
        tag.putString("owner", owner.toString());
        if (sourceAnchorId != null) {
            tag.putString("sourceAnchorId", sourceAnchorId);
        }
        if (targetAnchorId != null) {
            tag.putString("targetAnchorId", targetAnchorId);
        }
        tag.putBoolean("visible", visualOptions.visible());
        tag.putInt("frameColorArgb", visualOptions.frameColorArgb());
        tag.putString("style", visualOptions.style().name());
        return tag;
    }
    
    private static @Nullable PortalPersistentData read(CompoundTag portalTag) {
        if (!portalTag.contains(TAG_KEY)) {
            return null;
        }
        
        CompoundTag tag = portalTag.getCompoundOrEmpty(TAG_KEY);
        Identifier owner = Identifier.parse(tag.getStringOr("owner", "immersive_portals:unknown_owner"));
        PortalVisualOptions.Style style = PortalVisualOptions.Style.DEFAULT;
        try {
            style = PortalVisualOptions.Style.valueOf(tag.getStringOr("style", "DEFAULT"));
        }
        catch (IllegalArgumentException ignored) {
            // Keep old saves loadable even if an experimental style name changes.
        }
        
        return new PortalPersistentData(
            tag.getIntOr("apiVersion", CURRENT_API_VERSION),
            owner,
            tag.contains("sourceAnchorId") ? tag.getStringOr("sourceAnchorId", "") : null,
            tag.contains("targetAnchorId") ? tag.getStringOr("targetAnchorId", "") : null,
            new PortalVisualOptions(
                tag.getBooleanOr("visible", true),
                tag.getIntOr("frameColorArgb", 0xFF55FFFF),
                style
            )
        );
    }
}
