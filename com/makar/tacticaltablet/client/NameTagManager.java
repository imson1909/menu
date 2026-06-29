package com.makar.tacticaltablet.client;

import com.makar.tacticaltablet.game.GameStateManager;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.scores.Team.CollisionRule;
import net.minecraft.world.scores.Team.Visibility;

public final class NameTagManager {
   private static final String LEGACY_SHARED_TEAM_NAME = "war_hidden_names";
   private static final String PRIVATE_TEAM_PREFIX = "ttn_";
   private static final int PRIVATE_TEAM_HASH_LENGTH = 12;

   private NameTagManager() {
   }

   public static void applyToAll(MinecraftServer server) {
      if (server != null) {
         Scoreboard scoreboard = server.m_129896_();
         Set<String> activeTeamNames = new HashSet<>();

         for (ServerPlayer player : server.m_6846_().m_11314_()) {
            if (!shouldHideName(player)) {
               removeFromManagedTeam(scoreboard, player);
            } else {
               String teamName = privateTeamName(player);
               activeTeamNames.add(teamName);
               applyToPlayer(scoreboard, player, teamName);
            }
         }

         cleanupLegacySharedTeam(scoreboard);
         cleanupInactivePrivateTeams(scoreboard, activeTeamNames);
      }
   }

   public static void apply(ServerPlayer player) {
      if (player != null && player.f_8924_ != null) {
         Scoreboard scoreboard = player.f_8924_.m_129896_();
         if (!shouldHideName(player)) {
            removeFromManagedTeam(scoreboard, player);
            cleanupLegacySharedTeam(scoreboard);
         } else {
            applyToPlayer(scoreboard, player, privateTeamName(player));
            cleanupLegacySharedTeam(scoreboard);
         }
      }
   }

   public static void remove(ServerPlayer player) {
      if (player != null && player.f_8924_ != null) {
         Scoreboard scoreboard = player.f_8924_.m_129896_();
         removeFromManagedTeam(scoreboard, player);
         PlayerTeam privateTeam = scoreboard.m_83489_(privateTeamName(player));
         if (privateTeam != null && privateTeam.m_6809_().isEmpty()) {
            scoreboard.m_83475_(privateTeam);
         }

         cleanupLegacySharedTeam(scoreboard);
      }
   }

   private static void applyToPlayer(Scoreboard scoreboard, ServerPlayer player, String teamName) {
      String playerName = player.m_6302_();
      PlayerTeam privateTeam = scoreboard.m_83489_(teamName);
      if (privateTeam == null) {
         privateTeam = scoreboard.m_83492_(teamName);
      }

      configurePrivateTeam(privateTeam);
      PlayerTeam currentTeam = scoreboard.m_83500_(playerName);
      if (currentTeam != null && currentTeam != privateTeam) {
         scoreboard.m_6519_(playerName, currentTeam);
      }

      if (!privateTeam.m_6809_().contains(playerName)) {
         scoreboard.m_6546_(playerName, privateTeam);
      }
   }

   private static boolean shouldHideName(ServerPlayer player) {
      if (player == null || player.f_8924_ == null) {
         return false;
      } else if (!GameStateManager.isRunning(player.f_8924_)) {
         return false;
      } else {
         return GameStateManager.getCurrentMode().isTeamMode()
            ? false
            : player.m_19880_().contains("war.playing") || player.m_19880_().contains("in_lobby") || GameStateManager.isInLobby(player);
      }
   }

   private static void removeFromManagedTeam(Scoreboard scoreboard, ServerPlayer player) {
      String playerName = player.m_6302_();
      PlayerTeam currentTeam = scoreboard.m_83500_(playerName);
      if (isManagedTeam(currentTeam)) {
         scoreboard.m_6519_(playerName, currentTeam);
      }
   }

   private static void configurePrivateTeam(PlayerTeam team) {
      team.m_83346_(Visibility.NEVER);
      team.m_83358_(Visibility.ALWAYS);
      team.m_83344_(CollisionRule.ALWAYS);
      team.m_83355_(true);
      team.m_83362_(false);
   }

   private static void cleanupLegacySharedTeam(Scoreboard scoreboard) {
      PlayerTeam legacyTeam = scoreboard.m_83489_("war_hidden_names");
      if (legacyTeam != null) {
         for (String playerName : new ArrayList(legacyTeam.m_6809_())) {
            scoreboard.m_6519_(playerName, legacyTeam);
         }

         if (legacyTeam.m_6809_().isEmpty()) {
            scoreboard.m_83475_(legacyTeam);
         }
      }
   }

   private static void cleanupInactivePrivateTeams(Scoreboard scoreboard, Set<String> activeTeamNames) {
      for (PlayerTeam team : new ArrayList(scoreboard.m_83491_())) {
         String teamName = team.m_5758_();
         if (teamName.startsWith("ttn_") && !activeTeamNames.contains(teamName) && team.m_6809_().isEmpty()) {
            scoreboard.m_83475_(team);
         }
      }
   }

   private static boolean isManagedTeam(PlayerTeam team) {
      if (team == null) {
         return false;
      }

      String teamName = team.m_5758_();
      return "war_hidden_names".equals(teamName) || teamName.startsWith("ttn_");
   }

   private static String privateTeamName(ServerPlayer player) {
      String normalizedName = player.m_6302_().toLowerCase();
      UUID id = UUID.nameUUIDFromBytes(("tacticaltablet:hidden-team:" + normalizedName).getBytes(StandardCharsets.UTF_8));
      String hash = id.toString().replace("-", "").substring(0, 12);
      return "ttn_" + hash;
   }
}
