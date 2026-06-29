package com.makar.tacticaltablet.progression;

import com.makar.tacticaltablet.game.MapSetManager;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public class XpNotifier {
   public static void send(ServerPlayer player, int xp, String reason) {
      if (player != null && xp > 0 && reason != null && !MapSetManager.isCompetitiveSet()) {
         ChatFormatting reasonColor = ChatFormatting.YELLOW;
         if (reason.contains("mortar")) {
            reasonColor = ChatFormatting.RED;
         } else if (reason.contains("grenade") || reason.contains("mine") || reason.contains("explosion")) {
            reasonColor = ChatFormatting.GOLD;
         } else if (reason.contains("sniper")) {
            reasonColor = ChatFormatting.LIGHT_PURPLE;
         } else if (reason.contains("firearm")) {
            reasonColor = ChatFormatting.AQUA;
         } else if (reason.contains("melee")) {
            reasonColor = ChatFormatting.GREEN;
         }

         Component message = Component.m_237119_()
            .m_7220_(Component.m_237113_("+").m_130940_(ChatFormatting.DARK_GRAY))
            .m_7220_(Component.m_237113_(String.valueOf(xp)).m_130944_(new ChatFormatting[]{ChatFormatting.GREEN, ChatFormatting.BOLD}))
            .m_7220_(Component.m_237113_(" опыта").m_130940_(ChatFormatting.GREEN))
            .m_7220_(Component.m_237113_("  |  ").m_130940_(ChatFormatting.DARK_GRAY))
            .m_7220_(Component.m_237113_(reason).m_130940_(reasonColor));
         player.m_213846_(message);
      }
   }
}
