package com.makar.tacticaltablet.tablet.net;

import com.makar.tacticaltablet.game.GameStateManager;
import com.makar.tacticaltablet.game.MapSetManager;
import com.makar.tacticaltablet.game.MatchPhase;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent.Context;

public final class SetClanWarPacket {
   private final boolean clanWar;

   public SetClanWarPacket(boolean clanWar) {
      this.clanWar = clanWar;
   }

   public SetClanWarPacket(FriendlyByteBuf buf) {
      this.clanWar = buf.readBoolean();
   }

   public void encode(FriendlyByteBuf buf) {
      buf.writeBoolean(this.clanWar);
   }

   public void handle(Supplier<Context> contextSupplier) {
      Context context = contextSupplier.get();
      context.enqueueWork(() -> {
         ServerPlayer player = context.getSender();
         if (player != null && player.m_20310_(2)) {
            if (GameStateManager.getMatchPhase() != MatchPhase.MAP_VOTING) {
               MapSetManager.sync(player, false);
            } else {
               MapSetManager.setNextSetClanWar(player, this.clanWar);
            }
         }
      });
      context.setPacketHandled(true);
   }
}
