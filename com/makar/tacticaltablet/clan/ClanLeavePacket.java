package com.makar.tacticaltablet.clan;

import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent.Context;

public class ClanLeavePacket {
   public ClanLeavePacket() {
   }

   public ClanLeavePacket(FriendlyByteBuf buf) {
   }

   public void encode(FriendlyByteBuf buf) {
   }

   public void handle(Supplier<Context> ctx) {
      ctx.get().enqueueWork(() -> {
         ServerPlayer player = ctx.get().getSender();
         if (player != null) {
            ClanManager.Result result = ClanManager.leaveCurrentClan(player);
            player.m_213846_(Component.m_237113_(message(result)));
         }
      });
      ctx.get().setPacketHandled(true);
   }

   private static String message(ClanManager.Result result) {
      return switch (result) {
         case CLAN_WAR_LOCKED -> "[WAR] Во время войны кланов нельзя выходить из клана.";
         case SUCCESS -> "[WAR] Вы вышли из клана.";
         case OWNER_CANNOT_LEAVE -> "[WAR] Владелец не может выйти из клана. Распустите клан.";
         case NOT_FOUND -> "[WAR] Клан не найден.";
         default -> "[WAR] Выход из клана не выполнен.";
      };
   }
}
