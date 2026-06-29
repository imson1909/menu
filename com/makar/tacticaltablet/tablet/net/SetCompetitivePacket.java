package com.makar.tacticaltablet.tablet.net;

import com.makar.tacticaltablet.game.GameStateManager;
import com.makar.tacticaltablet.game.MapSetManager;
import com.makar.tacticaltablet.game.MatchPhase;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent.Context;

public final class SetCompetitivePacket {
   private final boolean competitive;

   public SetCompetitivePacket(boolean competitive) {
      this.competitive = competitive;
   }

   public SetCompetitivePacket(FriendlyByteBuf buf) {
      this.competitive = buf.readBoolean();
   }

   public void encode(FriendlyByteBuf buf) {
      buf.writeBoolean(this.competitive);
   }

   public void handle(Supplier<Context> contextSupplier) {
      Context context = contextSupplier.get();
      context.enqueueWork(() -> {
         ServerPlayer player = context.getSender();
         if (player != null && player.m_20310_(2)) {
            if (GameStateManager.getMatchPhase() != MatchPhase.MAP_VOTING) {
               MapSetManager.sync(player, false);
            } else {
               MapSetManager.setNextSetCompetitive(player, this.competitive);
            }
         }
      });
      context.setPacketHandled(true);
   }
}
