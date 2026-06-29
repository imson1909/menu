package com.makar.tacticaltablet.tablet.net;

import com.makar.tacticaltablet.game.contract.ContractManager;
import java.util.UUID;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent.Context;

public class ContractSelectTargetPacket {
   private final UUID targetUuid;

   public ContractSelectTargetPacket(UUID targetUuid) {
      this.targetUuid = targetUuid;
   }

   public ContractSelectTargetPacket(FriendlyByteBuf buf) {
      this.targetUuid = buf.m_130259_();
   }

   public void encode(FriendlyByteBuf buf) {
      buf.m_130077_(this.targetUuid);
   }

   public void handle(Supplier<Context> ctx) {
      ctx.get().enqueueWork(() -> {
         ServerPlayer player = ctx.get().getSender();
         if (player != null) {
            ContractManager.selectTarget(player, this.targetUuid);
         }
      });
      ctx.get().setPacketHandled(true);
   }
}
