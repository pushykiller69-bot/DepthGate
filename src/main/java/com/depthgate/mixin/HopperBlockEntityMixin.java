package com.depthgate.mixin;
import com.depthgate.DepthGateConfig;
import net.minecraft.block.entity.HopperBlockEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.util.collection.DefaultedList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(HopperBlockEntity.class)
public abstract class HopperBlockEntityMixin {
    @Shadow private DefaultedList<ItemStack> inventory;
    @Unique private int skipCounter = 0;
    @Inject(method = "insertAndExtract", at = @At("HEAD"), cancellable = true)
    private static void depthgate_skipEmptyHoppers(
            net.minecraft.world.World world,
            net.minecraft.util.math.BlockPos pos,
            net.minecraft.block.BlockState state,
            HopperBlockEntity hopper,
            CallbackInfo ci) {
        DepthGateConfig config = DepthGateConfig.get();
        if (!config.hopperOptEnabled) return;
        Inventory inv = hopper;
        boolean isEmpty = true;
        for (int i = 0; i < inv.size(); i++) {
            if (!inv.getStack(i).isEmpty()) {
                isEmpty = false;
                break;
            }
        }
        if (isEmpty) {
            long worldTime = world.getTime();
            if (worldTime % config.hopperSkipTicks != 0) {
                ci.cancel();
            }
        }
    }
}

