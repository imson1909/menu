package com.makar.tacticaltablet.clan;

import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent.Context;

public class ClanDisbandPacket {
   private final String clanId;

   public ClanDisbandPacket(String clanId) {
      this.clanId = clanId == null ? "" : clanId;
   }

   public ClanDisbandPacket(FriendlyByteBuf buf) {
      this.clanId = buf.m_130136_(64);
   }

   public void encode(FriendlyByteBuf buf) {
      buf.m_130072_(limit(this.clanId), 64);
   }

   public void handle(Supplier<Context> ctx) {
      ctx.get().enqueueWork(() -> {
         ServerPlayer player = ctx.get().getSender();
         if (player != null) {
            ClanManager.Result result = ClanManager.disbandClan(player, this.clanId);
            player.m_213846_(Component.m_237113_(message(result)));
         }
      });
      ctx.get().setPacketHandled(true);
   }

   private static String message(ClanManager.Result result) {
      return switch (result) {
         case CLAN_WAR_LOCKED -> "[WAR] Во время войны кланов нельзя распускать клан.";
         case SUCCESS -> "[WAR] Клан распущен.";
         case NOT_OWNER -> "[WAR] Только глава клана может выполнить это действие.";
         case NOT_FOUND -> "[WAR] Клан не найден.";
         default -> "[WAR] Клан не распущен.";
      };
   }

   private static String limit(String value) {
      if (value == null) {
         return "";
      } else {
         return value.length() <= 64 ? value : value.substring(0, 64);
      }
   }
}
