package com.makar.tacticaltablet.game.lives;

import com.makar.tacticaltablet.clan.ClanManager;
import com.makar.tacticaltablet.game.GameStateManager;
import com.makar.tacticaltablet.game.MapSetManager;
import com.makar.tacticaltablet.game.SpectatorCameraManager;
import com.makar.tacticaltablet.game.clanwar.ClanWarManager;
import com.makar.tacticaltablet.game.lobby.LobbyManager;
import com.makar.tacticaltablet.game.respawn.RespawnControlManager;
import com.makar.tacticaltablet.game.respawn.RtpTimerManager;
import com.makar.tacticaltablet.inventory.InventoryManager;
import com.makar.tacticaltablet.progression.ClassCooldownManager;
import com.makar.tacticaltablet.progression.ClassXPManager;
import com.makar.tacticaltablet.progression.PassiveClassXPManager;
import com.makar.tacticaltablet.tablet.PlayerTabletState;
import com.makar.tacticaltablet.voice.VoiceChatTeamManager;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.Scoreboard;

public class LivesManager {
   public static final String OBJECTIVE = "lives";
   public static final int MAX_LIVES = 3;
   private static final String TAG_LIVES_INIT = "war.lives_init";
   private static final String TAG_ELIMINATED = "war.eliminated";

   public static void ensureStarted(ServerPlayer player) {
      if (player != null) {
         if (!player.m_19880_().contains("war.lives_init")) {
            if (!player.m_19880_().contains("war.eliminated")) {
               setLives(player, GameStateManager.getLivesPerPlayer());
               player.m_20049_("war.lives_init");
            }
         }
      }
   }

   public static int getLives(ServerPlayer player) {
      if (player == null) {
         return 0;
      }

      Scoreboard scoreboard = player.m_36329_();
      Objective objective = getOrCreateLivesObjective(player);
      return objective == null ? GameStateManager.getLivesPerPlayer() : scoreboard.m_83471_(player.m_6302_(), objective).m_83400_();
   }

   public static void setLives(ServerPlayer player, int lives) {
      if (player != null) {
         Scoreboard scoreboard = player.m_36329_();
         Objective objective = getOrCreateLivesObjective(player);
         if (objective != null) {
            scoreboard.m_83471_(player.m_6302_(), objective).m_83402_(lives);
         }
      }
   }

   public static boolean isEliminated(ServerPlayer player) {
      return player != null && player.m_19880_().contains("war.eliminated");
   }

   public static boolean hasStarted(ServerPlayer player) {
      return player != null && player.m_19880_().contains("war.lives_init");
   }

   public static boolean canContinueMatch(ServerPlayer player) {
      if (player == null) {
         return false;
      } else if (isEliminated(player)) {
         return false;
      } else {
         return !player.m_19880_().contains("war.lives_init") ? true : getLives(player) > 0;
      }
   }

   public static boolean ensureEliminatedIfOutOfLives(ServerPlayer player) {
      if (player == null) {
         return false;
      } else if (isEliminated(player)) {
         moveEliminatedToSpectator(player);
         return true;
      } else if (hasStarted(player) && getLives(player) <= 0) {
         eliminate(player, 0);
         return true;
      } else {
         return false;
      }
   }

   public static boolean isAliveParticipant(ServerPlayer player) {
      if (player == null) {
         return false;
      } else if (!player.m_19880_().contains("war.lives_init")) {
         return false;
      } else {
         return isEliminated(player) ? false : getLives(player) > 0;
      }
   }

   public static int getAlivePlayerCount(MinecraftServer server) {
      if (server == null) {
         return 0;
      }

      int count = 0;

      for (ServerPlayer player : server.m_6846_().m_11314_()) {
         if (isAliveParticipant(player)) {
            count++;
         }
      }

      return count;
   }

   public static int getRemainingLivesTotal(MinecraftServer server) {
      if (server == null) {
         return 0;
      }

      int total = 0;

      for (ServerPlayer player : server.m_6846_().m_11314_()) {
         if (isAliveParticipant(player)) {
            total += Math.max(0, getLives(player));
         }
      }

      return total;
   }

   public static int handleDeath(ServerPlayer victim) {
      if (victim == null) {
         return 0;
      }

      if (MapSetManager.isClanWarSet()) {
         return handleClanWarDeath(victim);
      }

      if (!victim.m_19880_().contains("war.lives_init")) {
         ensureStarted(victim);
      }

      int lives = Math.max(0, getLives(victim) - 1);
      setLives(victim, lives);
      ClassCooldownManager.setCooldownForSelectedClass(victim);
      victim.m_20137_("war.playing");
      RtpTimerManager.cancel(victim);
      PassiveClassXPManager.clear(victim);
      if (lives > 0 && !RespawnControlManager.areRespawnsDisabled()) {
         victim.m_20049_("in_lobby");
         victim.m_213846_(Component.m_237113_("[WAR] Осталось жизней: " + lives));
      } else {
         eliminate(victim, lives);
      }

      return lives;
   }

   private static int handleClanWarDeath(ServerPlayer victim) {
      if (victim == null) {
         return 0;
      }

      if (!victim.m_19880_().contains("war.lives_init")) {
         ensureStarted(victim);
      }

      String clanId = ClanManager.getClanIdForPlayer(victim);
      ClassCooldownManager.setCooldownForSelectedClass(victim);
      victim.m_20137_("war.playing");
      victim.m_20137_("in_lobby");
      victim.m_20049_("war.clan_spectating");
      RtpTimerManager.cancel(victim);
      PassiveClassXPManager.clear(victim);
      VoiceChatTeamManager.removePlayerFromVoiceGroup(victim);
      if (clanId.isBlank()) {
         eliminate(victim, 0);
         return 0;
      } else if (!ClanWarManager.isClanWiped(victim.f_8924_, clanId)) {
         victim.m_213846_(Component.m_237113_("[WAR] Ты погиб. Ожидай исхода боя клана."));
         return ClanWarManager.getClanLives(clanId);
      } else {
         int remaining = ClanWarManager.consumeClanLife(victim.f_8924_, clanId);
         if (remaining > 0 && !RespawnControlManager.areRespawnsDisabled()) {
            regroupClan(victim.f_8924_, clanId);
            return remaining;
         } else {
            eliminateClan(victim.f_8924_, clanId);
            return 0;
         }
      }
   }

   private static void regroupClan(MinecraftServer server, String clanId) {
      ClanWarManager.markClanForRegroup(server, clanId);

      for (ServerPlayer player : server.m_6846_().m_11314_()) {
         if (clanId.equals(ClanManager.getClanIdForPlayer(player)) && !player.m_21224_()) {
            ClanWarManager.preparePlayerForRegroup(player);
            LobbyManager.moveToLobby(player);
            ClassXPManager.sync(player);
         }
      }
   }

   private static void eliminateClan(MinecraftServer server, String clanId) {
      for (ServerPlayer player : server.m_6846_().m_11314_()) {
         if (clanId.equals(ClanManager.getClanIdForPlayer(player))) {
            player.m_20137_("war.clan_spectating");
            player.m_20137_("war.clan_regroup_pending");
            eliminate(player, 0);
         }
      }
   }

   public static void eliminateForRespawnDisabled(ServerPlayer player) {
      if (player != null) {
         int unusedLives = Math.max(0, getLives(player));
         ClassCooldownManager.setCooldownForSelectedClass(player);
         player.m_20137_("war.playing");
         player.m_20137_("in_lobby");
         RtpTimerManager.cancel(player);
         PassiveClassXPManager.clear(player);
         eliminate(player, unusedLives);
      }
   }

   private static void eliminate(ServerPlayer player, int unusedLives) {
      player.m_20137_("in_lobby");
      player.m_20049_("war.eliminated");
      if (RespawnControlManager.areRespawnsDisabled() && unusedLives > 0) {
         RespawnControlManager.compensateUnusedLives(player, unusedLives);
      }

      setLives(player, 0);
      player.m_213846_(Component.m_237113_("[WAR] Ты выбыл из матча. Включён режим наблюдателя."));
      if (!player.m_21224_()) {
         moveEliminatedToSpectator(player);
      }
   }

   public static void moveEliminatedToSpectator(ServerPlayer player) {
      if (player != null) {
         VoiceChatTeamManager.removePlayerFromVoiceGroup(player);
         MinecraftServer server = player.f_8924_;
         ServerLevel overworld = GameStateManager.getOverworld(server);
         if (overworld != null) {
            RtpTimerManager.cancel(player);
            PassiveClassXPManager.clear(player);
            PlayerTabletState.reset(player);
            InventoryManager.clearInventory(player);
            player.m_20137_("war.playing");
            player.m_20137_("in_lobby");
            BlockPos spawn = overworld.m_220360_();
            double x = spawn.m_123341_() + 0.5;
            double y = spawn.m_123342_() + 2.0;
            double z = spawn.m_123343_() + 0.5;
            if (player.m_9236_().m_46472_().equals(overworld.m_46472_())) {
               x = player.m_20185_();
               y = player.m_20186_();
               z = player.m_20189_();
            }

            player.m_8999_(overworld, x, y, z, player.m_146908_(), player.m_146909_());
            player.m_143403_(GameType.SPECTATOR);
            SpectatorCameraManager.onPlayerEliminated(player);
            ClassXPManager.sync(player);
         }
      }
   }

   public static void resetPlayer(ServerPlayer player) {
      if (player != null) {
         player.m_20137_("war.lives_init");
         player.m_20137_("war.eliminated");
         setLives(player, GameStateManager.getLivesPerPlayer());
      }
   }

   public static void resetAll(MinecraftServer server) {
      if (server != null) {
         for (ServerPlayer player : server.m_6846_().m_11314_()) {
            resetPlayer(player);
         }
      }
   }

   private static Objective getOrCreateLivesObjective(ServerPlayer player) {
      Scoreboard scoreboard = player.m_36329_();
      Objective objective = scoreboard.m_83477_("lives");
      if (objective != null) {
         return objective;
      }

      CommandSourceStack source = player.f_8924_.m_129893_().m_81324_().m_81325_(4);
      player.f_8924_.m_129892_().m_230957_(source, "scoreboard objectives add lives dummy");
      return scoreboard.m_83477_("lives");
   }
}
