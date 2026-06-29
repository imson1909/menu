package com.makar.tacticaltablet.integration.discord;

import java.time.LocalDate;
import java.time.LocalDateTime;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.event.TickEvent.Phase;
import net.minecraftforge.event.TickEvent.ServerTickEvent;

public final class LeaderboardScheduler {
   private static int tickCounter;
   private static LocalDate lastSentDate;

   private LeaderboardScheduler() {
   }

   public static void onServerStarted(MinecraftServer server) {
      DiscordConfig.reload(server);
      tickCounter = 0;
      lastSentDate = null;
   }

   public static void reset() {
      tickCounter = 0;
      lastSentDate = null;
   }

   public static void tick(ServerTickEvent event) {
      if (event != null && event.phase == Phase.END) {
         MinecraftServer server = event.getServer();
         if (server != null && ++tickCounter >= 20) {
            tickCounter = 0;
            DiscordConfig config = DiscordConfig.get(server);
            if (config.hasWebhook()) {
               LocalDateTime now = LocalDateTime.now();
               LocalDate today = now.toLocalDate();
               if (lastSentDate == null || !lastSentDate.equals(today)) {
                  if (now.getHour() == config.getDailyHour() && now.getMinute() == config.getDailyMinute()) {
                     lastSentDate = today;
                     DiscordLeaderboardService.sendOverallLeaderboard(server);
                  }
               }
            }
         }
      }
   }
}
