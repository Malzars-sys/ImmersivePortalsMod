package qouteall.imm_ptl.api;

/**
 * High-level visual options only. This intentionally does not expose renderer,
 * framebuffer, shaderpack, Sodium, or Iris internals.
 */
public record PortalVisualOptions(
    boolean visible,
    int frameColorArgb,
    Style style
) {
    public enum Style {
        DEFAULT,
        DEBUG_CYAN,
        INVISIBLE,
        SIMPLE_MAGIC
    }
    
    public static PortalVisualOptions defaults() {
        return new PortalVisualOptions(true, 0xFF55FFFF, Style.DEFAULT);
    }
    
    public static PortalVisualOptions invisible() {
        return new PortalVisualOptions(false, 0x00000000, Style.INVISIBLE);
    }
    
    public PortalVisualOptions {
        if (style == null) {
            style = Style.DEFAULT;
        }
    }
}
