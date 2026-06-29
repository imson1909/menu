package com.makar.tacticaltablet.clan;

import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent.Context;

public class ClanAcceptJoinPacket {
   private final String clanId;
   private final String applicantUuid;

   public ClanAcceptJoinPacket(String clanId, String applicantUuid) {
      this.clanId = clanId == null ? "" : clanId;
      this.applicantUuid = applicantUuid == null ? "" : applicantUuid;
   }

   public ClanAcceptJoinPacket(FriendlyByteBuf buf) {
      this.clanId = buf.m_130136_(64);
      this.applicantUuid = buf.m_130136_(64);
   }

   public void encode(FriendlyByteBuf buf) {
      buf.m_130072_(limit(this.clanId), 64);
      buf.m_130072_(limit(this.applicantUuid), 64);
   }

   public void handle(Supplier<Context> ctx) {
      ctx.get().enqueueWork(() -> {
         ServerPlayer player = ctx.get().getSender();
         if (player != null) {
            ClanManager.Result result = ClanManager.acceptJoin(player, this.clanId, this.applicantUuid);
            player.m_213846_(Component.m_237113_(message(result)));
         }
      });
      ctx.get().setPacketHandled(true);
   }

   private static String message(ClanManager.Result result) {
      return switch (result) {
         case SUCCESS -> "[WAR] Игрок принят в клан.";
         case NOT_OWNER -> "[WAR] Только глава клана может выполнить это действие.";
         case ALREADY_IN_CLAN -> "[WAR] Игрок уже состоит в другом клане.";
         case CLAN_FULL -> "[WAR] В клане уже максимум 5 участников.";
         case NOT_FOUND -> "[WAR] Заявка не найдена.";
         default -> "[WAR] Заявка не принята.";
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
