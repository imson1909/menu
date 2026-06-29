package com.makar.tacticaltablet.tablet.net;

import com.makar.tacticaltablet.game.GameStateManager;
import com.makar.tacticaltablet.game.MapSetManager;
import com.makar.tacticaltablet.game.MatchPhase;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent.Context;

public final class VoteMapPacket {
   private static final int MAX_MAP_NAME_LENGTH = 64;
   private final String mapName;

   public VoteMapPacket(String mapName) {
      this.mapName = mapName == null ? "" : mapName;
   }

   public VoteMapPacket(FriendlyByteBuf buf) {
      this.mapName = buf.m_130136_(64);
   }

   public void encode(FriendlyByteBuf buf) {
      buf.m_130072_(this.mapName, 64);
   }

   public void handle(Supplier<Context> contextSupplier) {
      Context context = contextSupplier.get();
      context.enqueueWork(() -> {
         ServerPlayer player = context.getSender();
         if (player != null) {
            if (GameStateManager.getMatchPhase() != MatchPhase.MAP_VOTING) {
               MapSetManager.sync(player, false);
            } else {
               MapSetManager.vote(player, this.mapName);
            }
         }
      });
      context.setPacketHandled(true);
   }
}
