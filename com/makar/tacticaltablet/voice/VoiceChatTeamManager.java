package com.makar.tacticaltablet.voice;

import com.makar.tacticaltablet.core.TacticalTabletMod;
import com.makar.tacticaltablet.game.GameStateManager;
import com.makar.tacticaltablet.game.MatchPhase;
import com.makar.tacticaltablet.game.lives.LivesManager;
import com.makar.tacticaltablet.game.team.TeamId;
import com.makar.tacticaltablet.game.team.TeamMatchManager;
import de.maxhenkel.voicechat.api.Group;
import de.maxhenkel.voicechat.api.VoicechatConnection;
import de.maxhenkel.voicechat.api.VoicechatServerApi;
import de.maxhenkel.voicechat.api.Group.Type;
import de.maxhenkel.voicechat.api.events.PlayerConnectedEvent;
import de.maxhenkel.voicechat.api.events.VoicechatServerStartedEvent;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

public final class VoiceChatTeamManager {
   private static final int MAX_VOICE_TEAMS = 4;
   private static final Map<Integer, Group> teamGroups = new ConcurrentHashMap<>();
   private static volatile VoicechatServerApi api;
   private static volatile MinecraftServer matchServer;

   private VoiceChatTeamManager() {
   }

   public static void onVoicechatServerStarted(VoicechatServerStartedEvent event) {
      api = event.getVoicechat();
      MinecraftServer server = matchServer;
      if (server != null) {
         server.execute(() -> startTeamMatch(server));
      }
   }

   public static void onPlayerConnected(PlayerConnectedEvent event) {
      VoicechatConnection connection = event.getConnection();
      ServerPlayer player = resolvePlayer(connection);
      if (player != null) {
         player.f_8924_.execute(() -> assignPlayerToVoiceGroup(player, connection));
      }
   }

   public static synchronized void startTeamMatch(MinecraftServer server) {
      if (server != null
         && GameStateManager.isRunning(server)
         && GameStateManager.getMatchPhase() == MatchPhase.RUNNING
         && GameStateManager.getCurrentMode().isTeamMode()) {
         endMatch(server);
         matchServer = server;
         ensureVoiceGroups();

         for (ServerPlayer player : server.m_6846_().m_11314_()) {
            assignPlayerToVoiceGroup(player);
         }
      } else {
         endMatch(server);
      }
   }

   public static void assignPlayerToVoiceGroup(ServerPlayer player) {
      VoicechatServerApi currentApi = api;
      if (player != null && currentApi != null) {
         VoicechatConnection connection = currentApi.getConnectionOf(player.m_20148_());
         if (connection != null) {
            assignPlayerToVoiceGroup(player, connection);
         }
      }
   }

   public static void onPlayerTeamChanged(ServerPlayer player) {
      assignPlayerToVoiceGroup(player);
   }

   public static void removePlayerFromVoiceGroup(ServerPlayer player) {
      VoicechatServerApi currentApi = api;
      if (player != null && currentApi != null) {
         VoicechatConnection connection = currentApi.getConnectionOf(player.m_20148_());
         if (connection != null) {
            connection.setGroup(null);
         }
      }
   }

   public static boolean isPlayerVoiceEligible(ServerPlayer player) {
      return player != null
         && GameStateManager.isRunning(player.f_8924_)
         && GameStateManager.getMatchPhase() == MatchPhase.RUNNING
         && GameStateManager.getCurrentMode().isTeamMode()
         && TeamMatchManager.getTeam(player) != null
         && player.m_19880_().contains("war.playing")
         && !player.m_19880_().contains("in_lobby")
         && player.m_6084_()
         && !player.m_21224_()
         && !player.m_5833_()
         && !LivesManager.isEliminated(player)
         && LivesManager.isAliveParticipant(player);
   }

   public static synchronized void endMatch(MinecraftServer server) {
      matchServer = null;
      VoicechatServerApi currentApi = api;
      if (currentApi != null && server != null) {
         for (ServerPlayer player : server.m_6846_().m_11314_()) {
            VoicechatConnection connection = currentApi.getConnectionOf(player.m_20148_());
            if (connection != null) {
               connection.setGroup(null);
            }
         }
      }

      if (currentApi != null) {
         for (Group group : teamGroups.values()) {
            currentApi.removeGroup(group.getId());
         }
      }

      teamGroups.clear();
   }

   public static synchronized void shutdown(MinecraftServer server) {
      endMatch(server);
      api = null;
   }

   private static void assignPlayerToVoiceGroup(ServerPlayer player, VoicechatConnection connection) {
      if (!isPlayerVoiceEligible(player)) {
         connection.setGroup(null);
      } else {
         ensureVoiceGroups();
         TeamId team = TeamMatchManager.getTeam(player);
         Group group = team == null ? null : teamGroups.get(team.ordinal());
         connection.setGroup(group);
      }
   }

   private static synchronized void ensureVoiceGroups() {
      VoicechatServerApi currentApi = api;
      if (currentApi != null) {
         int groupCount = Math.min(4, TeamId.values().length);

         for (int teamIndex = 0; teamIndex < groupCount; teamIndex++) {
            if (!teamGroups.containsKey(teamIndex)) {
               try {
                  Group group = currentApi.groupBuilder()
                     .setName("DW Team " + (teamIndex + 1))
                     .setType(Type.ISOLATED)
                     .setHidden(true)
                     .setPersistent(false)
                     .build();
                  teamGroups.put(teamIndex, group);
               } catch (RuntimeException exception) {
                  TacticalTabletMod.LOGGER.error("Failed to create voice group for team {}", teamIndex + 1, exception);
               }
            }
         }
      }
   }

   private static ServerPlayer resolvePlayer(VoicechatConnection connection) {
      if (connection != null && connection.getPlayer() != null) {
         if (connection.getPlayer().getPlayer() instanceof ServerPlayer player) {
            return player;
         } else {
            MinecraftServer server = matchServer;
            return server == null ? null : server.m_6846_().m_11259_(connection.getPlayer().getUuid());
         }
      } else {
         return null;
      }
   }
}
