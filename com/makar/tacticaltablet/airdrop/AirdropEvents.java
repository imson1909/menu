package com.makar.tacticaltablet.airdrop;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.TickEvent.Phase;
import net.minecraftforge.event.TickEvent.ServerTickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public final class AirdropEvents {
   private AirdropEvents() {
   }

   @SubscribeEvent
   public static void onServerTick(ServerTickEvent event) {
      if (event.phase == Phase.END) {
         MinecraftServer server = event.getServer();
         ServerLevel overworld = server.m_129880_(Level.f_46428_);
         if (overworld != null) {
            AirdropManager.serverTick(overworld);
         }
      }
   }

   @SubscribeEvent
   public static void onPlayerLoggedIn(PlayerLoggedInEvent event) {
      if (event.getEntity() instanceof ServerPlayer player) {
         AirdropManager.giveCompassToJoiningPlayer(player);
      }
   }

   @SubscribeEvent
   public static void onEntityJoinLevel(EntityJoinLevelEvent event) {
      if (!event.getLevel().m_5776_()) {
         if (AirdropManager.isOrphanedVisualEntity(event.getEntity())) {
            event.getEntity().m_146870_();
            event.setCanceled(true);
         }
      }
   }
}
