package com.makar.tacticaltablet.clan;

import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent.Context;

public class ClanCreatePacket {
   private final String name;
   private final int color;
   private final String tag;

   public ClanCreatePacket(String name, int color, String tag) {
      this.name = name == null ? "" : name;
      this.color = color;
      this.tag = tag == null ? "" : tag;
   }

   public ClanCreatePacket(FriendlyByteBuf buf) {
      this.name = buf.m_130136_(24);
      this.color = buf.readInt();
      this.tag = buf.m_130136_(4);
   }

   public void encode(FriendlyByteBuf buf) {
      buf.m_130072_(limit(this.name, 24), 24);
      buf.writeInt(this.color);
      buf.m_130072_(limit(this.tag, 4), 4);
   }

   public void handle(Supplier<Context> ctx) {
      ctx.get().enqueueWork(() -> {
         ServerPlayer player = ctx.get().getSender();
         if (player != null) {
            ClanManager.Result result = ClanManager.createClan(player, this.name, this.color, this.tag);
            player.m_213846_(Component.m_237113_(message(result)));
         }
      });
      ctx.get().setPacketHandled(true);
   }

   private static String message(ClanManager.Result result) {
      return switch (result) {
         case SUCCESS -> "[WAR] Клан создан.";
         case NOT_ENOUGH_COINS -> "[WAR] Недостаточно монет для создания клана.";
         case ALREADY_IN_CLAN -> "[WAR] Вы уже состоите в клане.";
         case NAME_TAKEN -> "[WAR] Название или тег уже заняты.";
         default -> "[WAR] Клан не создан.";
      };
   }

   private static String limit(String value, int max) {
      return value.length() <= max ? value : value.substring(0, max);
   }
}
