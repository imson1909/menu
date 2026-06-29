package com.makar.tacticaltablet.game.respawn;

import com.makar.tacticaltablet.game.MapSetManager;
import com.makar.tacticaltablet.game.lives.LivesManager;
import com.makar.tacticaltablet.progression.ClassXPManager;
import com.makar.tacticaltablet.progression.PlayerProgressManager;
import com.makar.tacticaltablet.tablet.PlayerTabletState;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

public class RespawnControlManager {
   private static final int XP_PER_UNUSED_LIFE = 10;
   private static final int COINS_PER_UNUSED_LIFE = 2;
   private static boolean respawnsDisabled;

   public static boolean areRespawnsDisabled() {
      return respawnsDisabled;
   }

   public static void reset(MinecraftServer server) {
      respawnsDisabled = false;
      syncAll(server);
   }

   public static void disableRespawns(MinecraftServer server) {
      if (server != null) {
         if (!respawnsDisabled) {
            respawnsDisabled = true;
            broadcast(server, "[WAR] Возрождения отключены. Началась финальная фаза.");
         }

         eliminateWaitingPlayers(server);
         syncAll(server);
      }
   }

   public static void enableRespawns(MinecraftServer server) {
      respawnsDisabled = false;
      syncAll(server);
   }

   public static void compensateUnusedLives(ServerPlayer player, int unusedLives) {
      if (player != null && unusedLives > 0) {
         int cappedLives = Math.min(unusedLives, 2);
         if (cappedLives > 0) {
            int xpReward = cappedLives * 10;
            int coinReward = cappedLives * 2;
            PlayerProgressManager.addCoins(player, coinReward);
            String clazz = PlayerTabletState.getSelectedClass(player);
            boolean xpEnabled = !MapSetManager.isCompetitiveSet() && ClassXPManager.isStandardClass(clazz);
            int awardedXp = 0;
            if (xpEnabled) {
               awardedXp = ClassXPManager.addXP(player, clazz, xpReward);
            }

            player.m_213846_(
               Component.m_237113_(
                  "[WAR] Компенсация за неиспользованные жизни: +" + coinReward + " монет" + (xpEnabled ? ", +" + awardedXp + " опыта" : "") + "."
               )
            );
            PlayerProgressManager.savePlayer(player);
            ClassXPManager.sync(player);
         }
      }
   }

   private static void eliminateWaitingPlayers(MinecraftServer server) {
      for (ServerPlayer player : server.m_6846_().m_11314_()) {
         if (LivesManager.hasStarted(player)
            && !LivesManager.isEliminated(player)
            && !player.m_19880_().contains("war.playing")
            && LivesManager.getLives(player) > 0) {
            LivesManager.eliminateForRespawnDisabled(player);
         }
      }
   }

   private static void syncAll(MinecraftServer server) {
      if (server != null) {
         for (ServerPlayer player : server.m_6846_().m_11314_()) {
            ClassXPManager.sync(player);
         }
      }
   }

   private static void broadcast(MinecraftServer server, String message) {
      Component component = Component.m_237113_(message);

      for (ServerPlayer player : server.m_6846_().m_11314_()) {
         player.m_213846_(component);
      }
   }
}
