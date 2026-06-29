package com.makar.tacticaltablet.game.team;

import com.makar.tacticaltablet.clan.ClanManager;
import com.makar.tacticaltablet.game.MatchMode;
import com.makar.tacticaltablet.game.SpectatorCameraManager;
import com.makar.tacticaltablet.game.lives.LivesManager;
import com.makar.tacticaltablet.voice.VoiceChatTeamManager;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.scores.Team.CollisionRule;
import net.minecraft.world.scores.Team.Visibility;

public final class TeamMatchManager {
   private static final int TEAM_SELECT_SECONDS = 12;
   private static final Random RANDOM = new Random();
   private static final Map<TeamId, LinkedHashSet<UUID>> teams = new EnumMap<>(TeamId.class);
   private static final Map<UUID, TeamId> playerTeams = new HashMap<>();
   private static final Map<UUID, String> playerNames = new HashMap<>();
   private static final Map<String, TeamId> clanTeams = new HashMap<>();
   private static final Map<UUID, String> originalScoreboardTeams = new HashMap<>();
   private static int secondsLeft = 0;
   private static boolean activeSelection = false;

   private TeamMatchManager() {
   }

   public static void startSelection(MatchMode mode) {
      clearAssignments();

      for (TeamId team : TeamId.values()) {
         teams.put(team, new LinkedHashSet<>());
      }

      secondsLeft = 12;
      activeSelection = mode != null && mode.isTeamMode();
   }

   public static void reset(MinecraftServer server) {
      cleanupScoreboardTeams(server);
      clearAssignments();
      secondsLeft = 0;
      activeSelection = false;
   }

   public static boolean isSelectionActive() {
      return activeSelection;
   }

   public static int getSecondsLeft() {
      return Math.max(0, secondsLeft);
   }

   public static void tickSecond() {
      if (activeSelection && secondsLeft > 0) {
         secondsLeft--;
      }
   }

   public static boolean isSelectionComplete() {
      return activeSelection && secondsLeft <= 0;
   }

   public static boolean joinTeam(ServerPlayer player, TeamId team, MatchMode mode) {
      if (player != null && team != null && mode != null && activeSelection && mode.isTeamMode()) {
         LinkedHashSet<UUID> target = teams.computeIfAbsent(team, ignored -> new LinkedHashSet<>());
         UUID uuid = player.m_20148_();
         TeamId currentTeam = playerTeams.get(uuid);
         if (currentTeam == team) {
            rememberName(player);
            return true;
         }

         if (target.size() >= mode.teamSize()) {
            return false;
         }

         removeAssignment(uuid);
         target.add(uuid);
         playerTeams.put(uuid, team);
         rememberName(player);
         VoiceChatTeamManager.onPlayerTeamChanged(player);
         SpectatorCameraManager.onPlayerTeamChanged(player);
         return true;
      } else {
         return false;
      }
   }

   public static void rememberPlayer(ServerPlayer player) {
      if (player != null) {
         rememberName(player);
      }
   }

   public static boolean isInTeamMode(MatchMode mode) {
      return mode != null && mode.isTeamMode() && !playerTeams.isEmpty();
   }

   public static TeamId getTeam(ServerPlayer player) {
      return player == null ? null : playerTeams.get(player.m_20148_());
   }

   public static List<ServerPlayer> getOnlineTeamMembers(MinecraftServer server, TeamId teamId) {
      List<ServerPlayer> result = new ArrayList<>();
      if (server != null && teamId != null) {
         for (UUID uuid : teams.getOrDefault(teamId, new LinkedHashSet<>())) {
            ServerPlayer player = server.m_6846_().m_11259_(uuid);
            if (player != null) {
               result.add(player);
            }
         }

         return result;
      } else {
         return result;
      }
   }

   public static boolean areTeammates(ServerPlayer first, ServerPlayer second) {
      return first != null && second != null ? areTeammates(first.m_20148_(), second.m_20148_()) : false;
   }

   public static boolean areTeammates(UUID first, UUID second) {
      if (first != null && second != null && !first.equals(second)) {
         TeamId firstTeam = playerTeams.get(first);
         return firstTeam != null && firstTeam == playerTeams.get(second);
      } else {
         return false;
      }
   }

   public static List<ServerPlayer> getOnlineTeamMembers(MinecraftServer server, UUID playerUuid) {
      if (server != null && playerUuid != null) {
         TeamId teamId = playerTeams.get(playerUuid);
         return teamId == null ? List.of() : getOnlineTeamMembers(server, teamId);
      } else {
         return List.of();
      }
   }

   public static void autoBalance(MinecraftServer server, MatchMode mode) {
      if (server != null && mode != null && mode.isTeamMode()) {
         for (TeamId team : TeamId.values()) {
            teams.computeIfAbsent(team, ignored -> new LinkedHashSet<>());
         }

         List<ServerPlayer> players = new ArrayList<>(server.m_6846_().m_11314_());
         players.sort((a, b) -> a.m_20149_().compareTo(b.m_20149_()));

         for (ServerPlayer player : players) {
            rememberName(player);
            if (!playerTeams.containsKey(player.m_20148_())) {
               TeamId target = findSmallestAvailableTeam(mode);
               if (target == null) {
                  target = findSmallestTeam();
               }

               if (target != null) {
                  teams.computeIfAbsent(target, ignored -> new LinkedHashSet<>()).add(player.m_20148_());
                  playerTeams.put(player.m_20148_(), target);
               }
            }
         }

         activeSelection = false;
         secondsLeft = 0;
      }
   }

   public static void assignClanWarTeams(MinecraftServer server) {
      clearAssignments();
      if (server != null) {
         for (TeamId team : TeamId.values()) {
            teams.put(team, new LinkedHashSet<>());
         }

         for (ServerPlayer player : server.m_6846_().m_11314_()) {
            assignClanWarPlayer(server, player);
         }

         activeSelection = false;
         secondsLeft = 0;
      }
   }

   public static TeamId assignClanWarPlayer(MinecraftServer server, ServerPlayer player) {
      if (server != null && player != null) {
         rememberName(player);
         String clanId = ClanManager.getClanIdForPlayer(player);
         if (clanId.isBlank()) {
            removeAssignment(player.m_20148_());
            return null;
         }

         TeamId target = clanTeams.get(clanId);
         if (target == null) {
            target = findFreeClanWarTeam();
            if (target == null) {
               player.m_213846_(Component.m_237113_("[WAR] В войне кланов уже заняты все 4 командных слота. Вы играете по соло-правилам."));
               removeAssignment(player.m_20148_());
               return null;
            }

            clanTeams.put(clanId, target);
         }

         removeAssignment(player.m_20148_());
         teams.computeIfAbsent(target, ignored -> new LinkedHashSet<>()).add(player.m_20148_());
         playerTeams.put(player.m_20148_(), target);
         VoiceChatTeamManager.onPlayerTeamChanged(player);
         SpectatorCameraManager.onPlayerTeamChanged(player);
         return target;
      } else {
         return null;
      }
   }

   public static TeamId assignLateJoiner(MinecraftServer server, ServerPlayer player, MatchMode mode) {
      if (server != null && player != null && mode != null && mode.isTeamMode()) {
         rememberName(player);
         TeamId currentTeam = playerTeams.get(player.m_20148_());
         if (currentTeam != null) {
            VoiceChatTeamManager.onPlayerTeamChanged(player);
            SpectatorCameraManager.onPlayerTeamChanged(player);
            return currentTeam;
         }

         int fewestAlive = Integer.MAX_VALUE;
         List<TeamId> participatingTeams = new ArrayList<>();
         List<TeamId> depletedTeams = new ArrayList<>();

         for (TeamId teamId : TeamId.values()) {
            LinkedHashSet<UUID> members = teams.computeIfAbsent(teamId, ignored -> new LinkedHashSet<>());
            if (!members.isEmpty()) {
               participatingTeams.add(teamId);
               int alive = getAliveOnlineMemberCount(server, teamId);
               if (alive < mode.teamSize()) {
                  if (alive < fewestAlive) {
                     fewestAlive = alive;
                     depletedTeams.clear();
                     depletedTeams.add(teamId);
                  } else if (alive == fewestAlive) {
                     depletedTeams.add(teamId);
                  }
               }
            }
         }

         List<TeamId> candidates = depletedTeams.isEmpty() ? participatingTeams : depletedTeams;
         if (candidates.isEmpty()) {
            candidates = List.of(TeamId.values());
         }

         if (candidates.isEmpty()) {
            return null;
         }

         TeamId target = candidates.get(RANDOM.nextInt(candidates.size()));
         teams.get(target).add(player.m_20148_());
         playerTeams.put(player.m_20148_(), target);
         VoiceChatTeamManager.onPlayerTeamChanged(player);
         SpectatorCameraManager.onPlayerTeamChanged(player);
         return target;
      } else {
         return null;
      }
   }

   public static void applyScoreboardTeams(MinecraftServer server) {
      if (server != null) {
         Scoreboard scoreboard = server.m_129896_();

         for (TeamId teamId : TeamId.values()) {
            PlayerTeam team = scoreboard.m_83489_(teamId.scoreboardName());
            if (team == null) {
               team = scoreboard.m_83492_(teamId.scoreboardName());
            }

            configureTeam(teamId, team);
         }

         for (ServerPlayer player : server.m_6846_().m_11314_()) {
            TeamId teamId = playerTeams.get(player.m_20148_());
            if (teamId != null) {
               rememberOriginalTeam(scoreboard, player);
               PlayerTeam team = scoreboard.m_83489_(teamId.scoreboardName());
               if (team != null) {
                  PlayerTeam current = scoreboard.m_83500_(player.m_6302_());
                  if (current != null && current != team) {
                     scoreboard.m_6519_(player.m_6302_(), current);
                  }

                  if (!team.m_6809_().contains(player.m_6302_())) {
                     scoreboard.m_6546_(player.m_6302_(), team);
                  }
               }
            }
         }
      }
   }

   public static void cleanupScoreboardTeams(MinecraftServer server) {
      if (server != null) {
         Scoreboard scoreboard = server.m_129896_();

         for (ServerPlayer player : server.m_6846_().m_11314_()) {
            PlayerTeam current = scoreboard.m_83500_(player.m_6302_());
            if (isManagedTeam(current)) {
               scoreboard.m_6519_(player.m_6302_(), current);
            }

            String originalTeamName = originalScoreboardTeams.remove(player.m_20148_());
            if (originalTeamName != null && !originalTeamName.isBlank()) {
               PlayerTeam original = scoreboard.m_83489_(originalTeamName);
               if (original != null) {
                  scoreboard.m_6546_(player.m_6302_(), original);
               }
            }
         }

         for (TeamId teamId : TeamId.values()) {
            PlayerTeam team = scoreboard.m_83489_(teamId.scoreboardName());
            if (team != null) {
               for (String playerName : new ArrayList(team.m_6809_())) {
                  scoreboard.m_6519_(playerName, team);
               }

               scoreboard.m_83475_(team);
            }
         }

         originalScoreboardTeams.clear();
      }
   }

   public static int getAliveTeamCount(MinecraftServer server) {
      if (server == null) {
         return 0;
      }

      int aliveTeams = 0;

      for (TeamId teamId : TeamId.values()) {
         if (hasAliveOnlineMember(server, teamId)) {
            aliveTeams++;
         }
      }

      return aliveTeams;
   }

   public static TeamId findWinningTeam(MinecraftServer server) {
      if (server == null) {
         return null;
      }

      TeamId winner = null;

      for (TeamId teamId : TeamId.values()) {
         if (hasAliveOnlineMember(server, teamId)) {
            if (winner != null) {
               return null;
            }

            winner = teamId;
         }
      }

      return winner;
   }

   public static ServerPlayer findWinningPlayer(MinecraftServer server) {
      TeamId winner = findWinningTeam(server);
      if (winner == null) {
         return null;
      }

      for (UUID uuid : teams.getOrDefault(winner, new LinkedHashSet<>())) {
         ServerPlayer player = server.m_6846_().m_11259_(uuid);
         if (player != null && LivesManager.isAliveParticipant(player)) {
            return player;
         }
      }

      return null;
   }

   public static TeamMatchManager.Snapshot snapshot(MinecraftServer server, ServerPlayer viewer, MatchMode mode) {
      int selectedTeam = -1;
      if (viewer != null && playerTeams.containsKey(viewer.m_20148_())) {
         selectedTeam = playerTeams.get(viewer.m_20148_()).ordinal();
      }

      Map<String, String> slots = new HashMap<>();
      int maxSlots = mode == null ? 1 : mode.teamSize();

      for (TeamId teamId : TeamId.values()) {
         List<UUID> members = new ArrayList<>(teams.getOrDefault(teamId, new LinkedHashSet<>()));

         for (int slot = 0; slot < Math.max(maxSlots, members.size()); slot++) {
            String key = teamId.ordinal() + ":" + slot;
            String value = "";
            if (slot < members.size()) {
               value = displayName(server, members.get(slot));
            }

            slots.put(key, value);
         }
      }

      return new TeamMatchManager.Snapshot(maxSlots, selectedTeam, slots);
   }

   public static Map<TeamId, List<String>> teamNameSnapshot(MinecraftServer server) {
      Map<TeamId, List<String>> result = new EnumMap<>(TeamId.class);

      for (TeamId teamId : TeamId.values()) {
         List<String> names = new ArrayList<>();

         for (UUID uuid : teams.getOrDefault(teamId, new LinkedHashSet<>())) {
            names.add(displayName(server, uuid));
         }

         result.put(teamId, List.copyOf(names));
      }

      return result;
   }

   private static boolean hasAliveOnlineMember(MinecraftServer server, TeamId teamId) {
      return getAliveOnlineMemberCount(server, teamId) > 0;
   }

   private static int getAliveOnlineMemberCount(MinecraftServer server, TeamId teamId) {
      Set<UUID> members = teams.get(teamId);
      if (members != null && !members.isEmpty()) {
         int alive = 0;

         for (UUID uuid : members) {
            ServerPlayer player = server.m_6846_().m_11259_(uuid);
            if (player != null && LivesManager.isAliveParticipant(player)) {
               alive++;
            }
         }

         return alive;
      } else {
         return 0;
      }
   }

   private static TeamId findSmallestAvailableTeam(MatchMode mode) {
      int bestSize = Integer.MAX_VALUE;
      List<TeamId> candidates = new ArrayList<>();
      List<TeamId> emptyCandidates = new ArrayList<>();

      for (TeamId teamId : TeamId.values()) {
         int size = teams.computeIfAbsent(teamId, ignored -> new LinkedHashSet<>()).size();
         if (size < mode.teamSize()) {
            if (size == 0) {
               emptyCandidates.add(teamId);
            } else if (size < bestSize) {
               bestSize = size;
               candidates.clear();
               candidates.add(teamId);
            } else if (size == bestSize) {
               candidates.add(teamId);
            }
         }
      }

      if (!candidates.isEmpty()) {
         return candidates.get(RANDOM.nextInt(candidates.size()));
      } else {
         return emptyCandidates.isEmpty() ? null : emptyCandidates.get(RANDOM.nextInt(emptyCandidates.size()));
      }
   }

   private static TeamId findSmallestTeam() {
      int bestSize = Integer.MAX_VALUE;
      List<TeamId> candidates = new ArrayList<>();

      for (TeamId teamId : TeamId.values()) {
         int size = teams.computeIfAbsent(teamId, ignored -> new LinkedHashSet<>()).size();
         if (size < bestSize) {
            bestSize = size;
            candidates.clear();
            candidates.add(teamId);
         } else if (size == bestSize) {
            candidates.add(teamId);
         }
      }

      return candidates.isEmpty() ? null : candidates.get(RANDOM.nextInt(candidates.size()));
   }

   private static TeamId findFreeClanWarTeam() {
      for (TeamId teamId : TeamId.values()) {
         if (!clanTeams.containsValue(teamId)) {
            return teamId;
         }
      }

      return null;
   }

   private static void removeAssignment(UUID uuid) {
      TeamId oldTeam = playerTeams.remove(uuid);
      if (oldTeam != null) {
         LinkedHashSet<UUID> members = teams.get(oldTeam);
         if (members != null) {
            members.remove(uuid);
         }
      }
   }

   private static void clearAssignments() {
      teams.clear();
      playerTeams.clear();
      playerNames.clear();
      clanTeams.clear();
   }

   private static void configureTeam(TeamId teamId, PlayerTeam team) {
      team.m_83346_(Visibility.HIDE_FOR_OTHER_TEAMS);
      team.m_83358_(Visibility.ALWAYS);
      team.m_83344_(CollisionRule.ALWAYS);
      team.m_83355_(true);
      team.m_83362_(false);
      team.m_83351_(teamId.chatColor());
   }

   private static boolean isManagedTeam(PlayerTeam team) {
      if (team == null) {
         return false;
      }

      for (TeamId teamId : TeamId.values()) {
         if (teamId.scoreboardName().equals(team.m_5758_())) {
            return true;
         }
      }

      return false;
   }

   private static void rememberOriginalTeam(Scoreboard scoreboard, ServerPlayer player) {
      if (!originalScoreboardTeams.containsKey(player.m_20148_())) {
         PlayerTeam current = scoreboard.m_83500_(player.m_6302_());
         if (current != null && !isManagedTeam(current)) {
            originalScoreboardTeams.put(player.m_20148_(), current.m_5758_());
         } else {
            originalScoreboardTeams.put(player.m_20148_(), "");
         }
      }
   }

   private static void rememberName(ServerPlayer player) {
      playerNames.put(player.m_20148_(), player.m_36316_().getName());
   }

   private static String displayName(MinecraftServer server, UUID uuid) {
      if (server != null) {
         ServerPlayer online = server.m_6846_().m_11259_(uuid);
         if (online != null) {
            rememberName(online);
            return online.m_36316_().getName();
         }
      }

      return playerNames.getOrDefault(uuid, uuid.toString().substring(0, 8));
   }

   public record Snapshot(int maxSlots, int selectedTeam, Map<String, String> slots) {
   }
}
