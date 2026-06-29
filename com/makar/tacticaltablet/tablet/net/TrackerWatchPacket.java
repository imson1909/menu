package com.makar.tacticaltablet.tablet.net;

import com.makar.tacticaltablet.game.contract.ContractManager;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent.Context;

public class TrackerWatchPacket {
   private final boolean watching;

   public TrackerWatchPacket(boolean watching) {
      this.watching = watching;
   }

   public TrackerWatchPacket(FriendlyByteBuf buf) {
      this.watching = buf.readBoolean();
   }

   public void encode(FriendlyByteBuf buf) {
      buf.writeBoolean(this.watching);
   }

   public void handle(Supplier<Context> ctx) {
      ctx.get().enqueueWork(() -> {
         ServerPlayer player = ctx.get().getSender();
         if (player != null) {
            if (this.watching) {
               ContractManager.addTrackerViewer(player);
            } else {
               ContractManager.removeTrackerViewer(player);
            }
         }
      });
      ctx.get().setPacketHandled(true);
   }
}
