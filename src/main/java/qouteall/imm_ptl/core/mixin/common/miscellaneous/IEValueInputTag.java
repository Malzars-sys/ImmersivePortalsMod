package qouteall.imm_ptl.core.mixin.common.miscellaneous;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.storage.TagValueInput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(TagValueInput.class)
public interface IEValueInputTag {
    @Accessor("input")
    CompoundTag ip_getInputTag();
}
