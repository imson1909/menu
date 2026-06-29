package com.makar.tacticaltablet.clan;

import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent.Context;

public class ClanKickMemberPacket {
   private final String clanId;
   private final String memberUuid;

   public ClanKickMemberPacket(String clanId, String memberUuid) {
      this.clanId = clanId == null ? "" : clanId;
      this.memberUuid = memberUuid == null ? "" : memberUuid;
   }

   public ClanKickMemberPacket(FriendlyByteBuf buf) {
      this.clanId = buf.m_130136_(64);
      this.memberUuid = buf.m_130136_(64);
   }

   public void encode(FriendlyByteBuf buf) {
      buf.m_130072_(limit(this.clanId), 64);
      buf.m_130072_(limit(this.memberUuid), 64);
   }

   public void handle(Supplier<Context> ctx) {
      ctx.get().enqueueWork(() -> {
         ServerPlayer player = ctx.get().getSender();
         if (player != null) {
            ClanManager.Result result = ClanManager.kickMember(player, this.clanId, this.memberUuid);
            player.m_213846_(Component.m_237113_(message(result)));
         }
      });
      ctx.get().setPacketHandled(true);
   }

   private static String message(ClanManager.Result result) {
      return switch (result) {
         case CLAN_WAR_LOCKED -> "[WAR] Во время войны кланов нельзя исключать игроков из клана.";
         case SUCCESS -> "[WAR] Игрок исключен из клана.";
         case NOT_OWNER -> "[WAR] Только глава клана может выполнить это действие.";
         case CANNOT_KICK_OWNER -> "[WAR] Нельзя исключить главу клана.";
         case NOT_FOUND -> "[WAR] Игрок не найден.";
         default -> "[WAR] Игрок не исключен.";
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
