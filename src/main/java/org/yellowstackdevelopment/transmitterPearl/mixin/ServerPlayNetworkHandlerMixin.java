package org.yellowstackdevelopment.transmitterPearl.mixin;

import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.c2s.play.CreativeInventoryActionC2SPacket;
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Hand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.yellowstackdevelopment.transmitterPearl.TransmitterPearl;
import org.yellowstackdevelopment.transmitterPearl.item.TransmitterPearlItem;

@Mixin(ServerPlayNetworkHandler.class)
public class ServerPlayNetworkHandlerMixin {
    @Shadow
    public ServerPlayerEntity player;

    @Inject(method = "onCreativeInventoryAction", at = @At("HEAD"))
    private void transmitter_pearl$creativeBypass(CreativeInventoryActionC2SPacket packet, CallbackInfo ci) {
        if (player.getAbilities().creativeMode) {
            int slotId = packet.slot();
            if (slotId >= 0 && slotId < player.currentScreenHandler.slots.size()) {
                ItemStack clientStack = packet.stack();
                if (clientStack.isOf(TransmitterPearl.TRANSMITTER_PEARL)) {
                    ItemStack serverStack = player.currentScreenHandler.getSlot(slotId).getStack();
                    if (serverStack.isOf(TransmitterPearl.TRANSMITTER_PEARL)) {
                        if (serverStack.contains(TransmitterPearl.TRANSMITTER_LINK)) {
                            clientStack.set(TransmitterPearl.TRANSMITTER_LINK, serverStack.get(TransmitterPearl.TRANSMITTER_LINK));
                        }
                    }
                }
            }
        }
    }

    @Inject(method = "onHandSwing", at = @At("HEAD"))
    private void transmitter_pearl$onHandSwing(HandSwingC2SPacket packet, CallbackInfo ci) {
        if (packet.getHand() == Hand.MAIN_HAND) {
            ItemStack stack = player.getMainHandStack();
            if (stack.isOf(TransmitterPearl.TRANSMITTER_PEARL)) {
                if (player.getItemCooldownManager().isCoolingDown(TransmitterPearl.TRANSMITTER_PEARL)) {
                    return;
                }

                TransmitterPearlItem item = (TransmitterPearlItem) stack.getItem();
                if (player.isSneaking()) {
                    item.clearLinks(player, stack);
                } else {
                    item.processTransmission(player, stack);
                }
            }
        }
    }
}
