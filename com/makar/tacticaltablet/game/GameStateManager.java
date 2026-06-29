package com.makar.tacticaltablet.game;

import com.makar.tacticaltablet.admin.TestModeManager;
import com.makar.tacticaltablet.airdrop.AirdropManager;
import com.makar.tacticaltablet.clan.ClanManager;
import com.makar.tacticaltablet.core.TacticalTabletMod;
import com.makar.tacticaltablet.game.clanwar.ClanWarManager;
import com.makar.tacticaltablet.game.contract.ContractManager;
import com.makar.tacticaltablet.game.extraction.ExtractionPointManager;
import com.makar.tacticaltablet.game.lives.LivesManager;
import com.makar.tacticaltablet.game.lobby.LobbyManager;
import com.makar.tacticaltablet.game.respawn.RespawnControlManager;
import com.makar.tacticaltablet.game.respawn.RtpTimerManager;
import com.makar.tacticaltablet.game.team.TeamId;
import com.makar.tacticaltablet.game.team.TeamMatchManager;
import com.makar.tacticaltablet.game.team.VoteManager;
import com.makar.tacticaltablet.game.teleport.SafeTeleport;
import com.makar.tacticaltablet.game.zone.ZoneManager;
import com.makar.tacticaltablet.integration.discord.DiscordLeaderboardService;
import com.makar.tacticaltablet.inventory.InventoryManager;
import com.makar.tacticaltablet.map.WorldCleanupManager;
import com.makar.tacticaltablet.progression.ClassCooldownManager;
import com.makar.tacticaltablet.progression.ClassXPManager;
import com.makar.tacticaltablet.progression.PassiveClassXPManager;
import com.makar.tacticaltablet.progression.PlayerProgressManager;
import com.makar.tacticaltablet.voice.VoiceChatTeamManager;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.Scoreboard;

public class GameStateManager {
   public static final ResourceKey<Level> LOBBY_DIMENSION = ResourceKey.m_135785_(Registries.f_256858_, new ResourceLocation("lobby", "lobby"));
   public static final int WAITING = 0;
   public static final int RUNNING = 1;
   private static final int MIN_PLAYERS = 2;
   private static final int START_DELAY_SECONDS = 10;
   private static final int POST_GAME_DELAY_SECONDS = 3;
   private static final int WIN_XP_ALL_CLASSES = 10;
   private static final String GAME_STATE_OBJECTIVE = "gameState";
   private static final ResourceLocation START_GAME_FUNCTION = new ResourceLocation("war", "start_game");
   private static final ResourceLocation RESET_GAME_FUNCTION = new ResourceLocation("war", "reset");
   private static boolean matchHadEnoughPlayers = false;
   private static int matchStartingParticipants = 0;
   private static int tickCounter = 0;
   private static int startCountdown = -1;
   private static int postGameDelay = 0;
   private static MatchPhase matchPhase = MatchPhase.WAITING;
   private static MatchMode currentMode = MatchMode.SOLO;

   public static int getGameState(MinecraftServer server) {
      if (server == null) {
         return 0;
      }

      Objective objective = getOrCreateGameStateObjective(server);
      return objective == null ? 0 : server.m_129896_().m_83471_("#state", objective).m_83400_();
   }

   public static void setGameState(MinecraftServer server, int state) {
      if (server != null) {
         Objective objective = getOrCreateGameStateObjective(server);
         if (objective != null) {
            Scoreboard scoreboard = server.m_129896_();
            int current = scoreboard.m_83471_("#state", objective).m_83400_();
            if (current != state) {
               scoreboard.m_83471_("#state", objective).m_83402_(state);
            }
         }
      }
   }

   private static Objective getOrCreateGameStateObjective(MinecraftServer server) {
      Scoreboard scoreboard = server.m_129896_();
      Objective objective = scoreboard.m_83477_("gameState");
      if (objective != null) {
         return objective;
      }

      CommandSourceStack source = server.m_129893_().m_81324_().m_81325_(4);
      server.m_129892_().m_230957_(source, "scoreboard objectives add gameState dummy");
      return scoreboard.m_83477_("gameState");
   }

   public static boolean isRunning(MinecraftServer server) {
      return getGameState(server) == 1;
   }

   public static MatchPhase getMatchPhase() {
      return matchPhase;
   }

   public static MatchMode getCurrentMode() {
      return currentMode;
   }

   public static int getLivesPerPlayer() {
      return MapSetManager.isClanWarSet() ? 1 : currentMode.livesPerPlayer();
   }

   public static boolean isTabletAvailableInLobby(MinecraftServer server) {
      return server == null ? false : isRunning(server) || matchPhase == MatchPhase.VOTING || matchPhase == MatchPhase.TEAM_SELECT;
   }

   public static boolean isInLobby(ServerPlayer player) {
      return player != null && player.m_9236_().m_46472_().equals(LOBBY_DIMENSION);
   }

   public static ServerLevel getLobbyLevel(MinecraftServer server) {
      return server == null ? null : server.m_129880_(LOBBY_DIMENSION);
   }

   public static ServerLevel getOverworld(MinecraftServer server) {
      return server == null ? null : server.m_129880_(Level.f_46428_);
   }

   public static int onlinePlayers(MinecraftServer server) {
      return server == null ? 0 : server.m_6846_().m_11309_();
   }

   public static int playingPlayers(MinecraftServer server) {
      return LivesManager.getAlivePlayerCount(server);
   }

   private static ServerPlayer findWinner(MinecraftServer server) {
      if (server == null) {
         return null;
      }

      for (ServerPlayer player : server.m_6846_().m_11314_()) {
         if (LivesManager.isAliveParticipant(player)) {
            return player;
         }
      }

      return null;
   }

   public static void onServerTick(MinecraftServer server) {
      if (server != null) {
         if (++tickCounter >= 20) {
            tickCounter = 0;
            if (postGameDelay > 0) {
               postGameDelay--;
               if (postGameDelay <= 0) {
                  cleanupMatchRuntime(server);
                  if (MapSetManager.isSetComplete()) {
                     beginMapVoting(server, false);
                  } else {
                     matchPhase = MatchPhase.WAITING;
                  }

                  ClassXPManager.syncAll(server);
               }
            } else if (!isRunning(server)) {
               handleWaitingTick(server);
            } else {
               startCountdown = -1;
               matchPhase = MatchPhase.RUNNING;
               ZoneManager.tick(server);
               checkForMatchEnd(server);
            }
         }
      }
   }

   public static void checkForMatchEnd(MinecraftServer server) {
      if (server != null && isRunning(server) && postGameDelay <= 0) {
         if (MapSetManager.isClanWarSet()) {
            int aliveClans = ClanWarManager.getAliveClanCount(server);
            if (aliveClans <= 1 && (!ClanWarManager.isSoloDebugEnabled() || aliveClans <= 0)) {
               ServerPlayer winner = ClanWarManager.findWinningClanRepresentative(server);
               TeamId winnerTeam = TeamMatchManager.getTeam(winner);
               endGame(server, winner, clanWarWinnerLabel(winner), winnerTeam);
            }
         } else if (currentMode.isTeamMode()) {
            int aliveTeams = TeamMatchManager.getAliveTeamCount(server);
            if (aliveTeams <= 1) {
               TeamId winningTeam = TeamMatchManager.findWinningTeam(server);
               ServerPlayer winner = TeamMatchManager.findWinningPlayer(server);
               String winnerLabel = winningTeam == null ? "Нет победителя" : winningTeam.displayName();
               endGame(server, winner, winnerLabel, winningTeam);
            }
         } else {
            int alive = playingPlayers(server);
            if (matchStartingParticipants > 0 && alive <= 0) {
               endGame(server, null, "Нет победителя");
            } else {
               int requiredPlayers = TestModeManager.getRequiredPlayers(2);
               if (requiredPlayers > 1) {
                  if (alive >= requiredPlayers) {
                     matchHadEnoughPlayers = true;
                  } else {
                     if (matchHadEnoughPlayers && alive <= 1) {
                        endGame(server, findWinner(server));
                     }
                  }
               }
            }
         }
      }
   }

   public static void startGame(MinecraftServer server) {
      if (server != null) {
         if (!validateRuntimeRequirements(server)) {
            setGameState(server, 0);
            broadcast(server, "[WAR] Старт матча отменён: сервер настроен не полностью. Проверь лог.");
         } else {
            RtpTimerManager.clearAll();
            PassiveClassXPManager.clearAll();
            RespawnControlManager.reset(server);
            LivesManager.resetAll(server);
            setGameState(server, 1);
            matchPhase = MatchPhase.RUNNING;
            if (MapSetManager.isClanWarSet()) {
               ClanWarManager.startMatch(server);
               currentMode = MatchMode.SQUADS;
               TeamMatchManager.assignClanWarTeams(server);
               TeamMatchManager.applyScoreboardTeams(server);
            } else if (currentMode.isTeamMode()) {
               TeamMatchManager.autoBalance(server, currentMode);
               TeamMatchManager.applyScoreboardTeams(server);
            } else {
               TeamMatchManager.reset(server);
            }

            AirdropManager.resetAutoScheduler();
            DiscordLeaderboardService.startMatch(server);
            ContractManager.onMatchStart(server);
            matchHadEnoughPlayers = false;
            matchStartingParticipants = 0;
            CommandSourceStack source = server.m_129893_().m_81324_();
            server.m_129892_().m_230957_(source, "function " + START_GAME_FUNCTION);
            MapSetManager.announceGameStart(server);
            DropControlManager.enforceGameRules(server);
            ZoneManager.start(server);
            SafeTeleport.preparePool(server);

            for (ServerPlayer player : server.m_6846_().m_11314_()) {
               if (MapSetManager.isClanWarSet() && !ClanWarManager.hasClan(player)) {
                  player.m_20137_("war.eliminated");
                  player.m_20137_("war.playing");
                  player.m_20137_("in_lobby");
                  InventoryManager.clearInventory(player);
                  player.m_143403_(GameType.SPECTATOR);
                  ClanWarManager.showNeedClan(player);
                  ClassXPManager.sync(player);
               } else {
                  LivesManager.ensureStarted(player);
                  PlayerProgressManager.addMatchPlayed(player);
                  player.m_20137_("war.eliminated");
                  player.m_20137_("war.playing");
                  player.m_20049_("in_lobby");
                  LobbyManager.moveToLobby(player);
                  ContractManager.giveSelectionTrackerIfAvailable(player);
                  ClassXPManager.sync(player);
               }
            }

            VoiceChatTeamManager.startTeamMatch(server);
            matchStartingParticipants = playingPlayers(server);
            matchHadEnoughPlayers = matchStartingParticipants >= TestModeManager.getRequiredPlayers(2);
            ExtractionPointManager.onMatchStart(server);
            ClassXPManager.syncAll(server);
            broadcast(server, "[WAR] Матч начался. Выбери класс и используй RTP.");
         }
      }
   }

   private static String clanWarWinnerLabel(ServerPlayer winner) {
      if (winner == null) {
         return "РќРµС‚ РїРѕР±РµРґРёС‚РµР»СЏ";
      }

      String clanName = ClanManager.getClanNameForPlayer(winner);
      return clanName.isBlank() ? winner.m_7755_().getString() : clanName;
   }

   public static void endGame(MinecraftServer server) {
      endGame(server, findWinner(server));
   }

   public static void endGame(MinecraftServer server, ServerPlayer winner) {
      endGame(server, winner, winner != null ? winner.m_7755_().getString() : "Нет победителя", null);
   }

   private static void endGame(MinecraftServer server, ServerPlayer winner, String winnerName) {
      endGame(server, winner, winnerName, null);
   }

   private static void endGame(MinecraftServer server, ServerPlayer winner, String winnerName, TeamId winnerTeam) {
      if (server != null) {
         matchHadEnoughPlayers = false;
         matchStartingParticipants = 0;
         startCountdown = -1;
         postGameDelay = 3;
         matchPhase = MatchPhase.POST_GAME;
         setGameState(server, 0);
         SpectatorCameraManager.onMatchEnd(server);
         VoiceChatTeamManager.endMatch(server);
         applySelectedClassCooldowns(server);
         ContractManager.finishMatch(server);
         ExtractionPointManager.reset(server);
         boolean clanWarSet = MapSetManager.isClanWarSet();
         boolean setComplete = MapSetManager.onGameCompleted(server);
         DiscordLeaderboardService.SetWinner setWinner = DiscordLeaderboardService.sendCurrentMatchLeaderboard(server, winner, setComplete, clanWarSet);
         if (setComplete && MapSetManager.isCompetitiveSet() && setWinner != null) {
            if (PlayerProgressManager.addCoins(server, setWinner.uuid(), 100)) {
               broadcast(server, "[WAR] Победитель соревновательного сета " + setWinner.name() + " получает 100 монет для casual-режима.");
            } else {
               TacticalTabletMod.LOGGER.error("Failed to award 100 competitive-set coins to {} ({})", setWinner.name(), setWinner.uuid());
            }
         }

         if (winner != null) {
            PlayerProgressManager.addWin(winner);
            PlayerProgressManager.addCoins(winner, 5);
            ClassXPManager.addXPToAllClasses(winner, 10);
            PlayerProgressManager.savePlayer(winner);
         }

         showWinnerTitle(server, winnerName, winnerTeam);
      }
   }

   private static void applySelectedClassCooldowns(MinecraftServer server) {
      if (server != null) {
         for (ServerPlayer player : server.m_6846_().m_11314_()) {
            ClassCooldownManager.setCooldownForSelectedClass(player);
         }

         ClassXPManager.syncAll(server);
      }
   }

   public static void resetRuntime(MinecraftServer server) {
      matchHadEnoughPlayers = false;
      matchStartingParticipants = 0;
      tickCounter = 0;
      startCountdown = -1;
      postGameDelay = 0;
      matchPhase = MatchPhase.WAITING;
      currentMode = MatchMode.SOLO;
      ClanWarManager.resetRuntime();
      VoteManager.reset();
      SpectatorCameraManager.onMatchEnd(server);
      VoiceChatTeamManager.endMatch(server);
      TeamMatchManager.reset(server);
      ExtractionPointManager.reset(server);
      if (server != null) {
         setGameState(server, 0);
      }
   }

   public static boolean forceStopMatch(MinecraftServer server) {
      if (server == null) {
         return false;
      }

      boolean hadActiveState = isRunning(server) || matchPhase != MatchPhase.WAITING || postGameDelay > 0 || startCountdown >= 0;
      matchHadEnoughPlayers = false;
      matchStartingParticipants = 0;
      tickCounter = 0;
      startCountdown = -1;
      postGameDelay = 0;
      matchPhase = MatchPhase.WAITING;
      cleanupMatchRuntime(server);
      broadcast(server, hadActiveState ? "[WAR] Матч принудительно остановлен." : "[WAR] Состояние матча сброшено.");
      ClassXPManager.syncAll(server);
      return hadActiveState;
   }

   private static void cleanupMatchRuntime(MinecraftServer server) {
      if (server != null) {
         setGameState(server, 0);
         SpectatorCameraManager.onMatchEnd(server);
         VoiceChatTeamManager.endMatch(server);
         TeamMatchManager.cleanupScoreboardTeams(server);
         AirdropManager.resetAutoScheduler();
         ContractManager.reset(server);
         ExtractionPointManager.reset(server);
         ServerLevel activeAirdropLevel = getOverworld(server);
         if (activeAirdropLevel != null) {
            AirdropManager.cancel(activeAirdropLevel);
         }

         ZoneManager.reset(server);
         RespawnControlManager.reset(server);
         PassiveClassXPManager.clearAll();
         RtpTimerManager.clearAll();
         SafeTeleport.clearPool();
         ClanWarManager.resetRuntime();
         WorldCleanupManager.clearDroppedItems(server);
         CommandSourceStack source = server.m_129893_().m_81324_();
         server.m_129892_().m_230957_(source, "function " + RESET_GAME_FUNCTION);
         DropControlManager.enforceGameRules(server);
         LivesManager.resetAll(server);

         for (ServerPlayer player : server.m_6846_().m_11314_()) {
            player.m_20137_("war.playing");
            player.m_20137_("in_lobby");
            LobbyManager.moveToLobby(player);
            ClassXPManager.sync(player);
         }

         currentMode = MatchMode.SOLO;
         VoteManager.reset();
         TeamMatchManager.reset(server);
      }
   }

   public static boolean validateRuntimeRequirements(MinecraftServer server) {
      if (server == null) {
         return false;
      }

      boolean valid = true;
      if (getOverworld(server) == null) {
         TacticalTabletMod.LOGGER.error("Tactical Tablet setup error: overworld dimension is unavailable.");
         valid = false;
      }

      if (getLobbyLevel(server) == null) {
         TacticalTabletMod.LOGGER.error("Tactical Tablet setup error: lobby:lobby dimension is unavailable.");
         valid = false;
      }

      if (!hasFunction(server, START_GAME_FUNCTION)) {
         TacticalTabletMod.LOGGER.error("Tactical Tablet setup error: datapack function {} is missing.", START_GAME_FUNCTION);
         valid = false;
      }

      if (!hasFunction(server, RESET_GAME_FUNCTION)) {
         TacticalTabletMod.LOGGER.error("Tactical Tablet setup error: datapack function {} is missing.", RESET_GAME_FUNCTION);
         valid = false;
      }

      getOrCreateGameStateObjective(server);
      return valid;
   }

   private static boolean hasFunction(MinecraftServer server, ResourceLocation id) {
      return server.m_129890_().m_136118_(id).isPresent();
   }

   public static boolean forceStartVoting(MinecraftServer server) {
      if (server != null && !isRunning(server)) {
         postGameDelay = 0;
         startCountdown = -1;
         currentMode = MatchMode.SOLO;
         TeamMatchManager.reset(server);
         VoteManager.start();
         matchPhase = MatchPhase.VOTING;
         setGameState(server, 0);
         broadcast(server, "[WAR] Отладочное голосование началось: " + describeVoteModes(server) + ".");
         giveLobbyTabletsAndSync(server);
         return true;
      } else {
         return false;
      }
   }

   public static boolean forceStartMapVoting(MinecraftServer server) {
      if (server != null && !isRunning(server)) {
         postGameDelay = 0;
         startCountdown = -1;
         cleanupMatchRuntime(server);
         beginMapVoting(server, true);
         return true;
      } else {
         return false;
      }
   }

   public static boolean forceStartTeamSelect(MinecraftServer server, MatchMode mode) {
      if (server != null && !isRunning(server) && mode != null && mode.isTeamMode()) {
         postGameDelay = 0;
         startCountdown = -1;
         VoteManager.reset();
         currentMode = mode;
         TeamMatchManager.startSelection(mode);
         matchPhase = MatchPhase.TEAM_SELECT;
         setGameState(server, 0);
         broadcast(server, "[WAR] Отладочный выбор команды начался: " + mode.displayName() + ".");
         giveLobbyTabletsAndSync(server);
         return true;
      } else {
         return false;
      }
   }

   public static boolean forceStartClanWar(MinecraftServer server, boolean skipPreStartWait) {
      if (server != null && !isRunning(server)) {
         postGameDelay = 0;
         startCountdown = -1;
         cleanupMatchRuntime(server);
         currentMode = MatchMode.SQUADS;
         matchPhase = MatchPhase.WAITING;
         if (skipPreStartWait) {
            ClanWarManager.skipPreStartWait();
            startGame(server);
         } else {
            setGameState(server, 0);
            giveLobbyTabletsAndSync(server);
         }

         return true;
      } else {
         return false;
      }
   }

   private static void handleWaitingTick(MinecraftServer server) {
      matchHadEnoughPlayers = false;
      if (matchPhase == MatchPhase.MAP_VOTING) {
         MapSetManager.VoteTickResult result = MapSetManager.tickVoting(server);
         if (result == MapSetManager.VoteTickResult.PREPARED) {
            matchPhase = MatchPhase.RESTARTING;
            ClassXPManager.syncAll(server);
         }
      } else if (matchPhase == MatchPhase.RESTARTING) {
         MapSetManager.tickRestart(server);
      } else if (matchPhase == MatchPhase.WAITING && MapSetManager.isSetComplete()) {
         beginMapVoting(server, false);
      } else if (matchPhase == MatchPhase.VOTING) {
         handleVotingTick(server);
      } else if (matchPhase == MatchPhase.TEAM_SELECT) {
         handleTeamSelectTick(server);
      } else if (matchPhase == MatchPhase.STARTING) {
         handleStartingTick(server);
      } else {
         int requiredPlayers = TestModeManager.getRequiredPlayers(2);
         if (onlinePlayers(server) < requiredPlayers) {
            startCountdown = -1;
         } else if (MapSetManager.isClanWarSet()) {
            if (!ClanWarManager.tickPreStartWait(server)) {
               currentMode = MatchMode.SQUADS;
               startGame(server);
            }
         } else if (canStartVoting(server) && !TestModeManager.isSoloStartEnabled()) {
            matchPhase = MatchPhase.VOTING;
            VoteManager.start();
            startCountdown = -1;
            broadcast(server, "[WAR] Голосование за режим матча: " + describeVoteModes(server) + ".");
            giveLobbyTabletsAndSync(server);
         } else if (startCountdown < 0) {
            matchPhase = MatchPhase.STARTING;
            startCountdown = 10;
            String suffix = TestModeManager.isSoloStartEnabled() ? " (соло-тест)" : "";
            broadcast(server, "[WAR] Матч начнётся через " + startCountdown + " сек." + suffix);
         }
      }
   }

   private static void handleStartingTick(MinecraftServer server) {
      int requiredPlayers = TestModeManager.getRequiredPlayers(2);
      if (onlinePlayers(server) < requiredPlayers) {
         matchPhase = MatchPhase.WAITING;
         startCountdown = -1;
      } else if (startCountdown == 0) {
         currentMode = MapSetManager.isClanWarSet() ? MatchMode.SQUADS : MatchMode.SOLO;
         startGame(server);
         startCountdown = -1;
      } else {
         if (startCountdown <= 5 || startCountdown == 10) {
            broadcast(server, "[WAR] Матч начнётся через " + startCountdown + "...");
         }

         startCountdown--;
      }
   }

   private static void handleVotingTick(MinecraftServer server) {
      if (!canStartVoting(server)) {
         VoteManager.reset();
         matchPhase = MatchPhase.WAITING;
         currentMode = MatchMode.SOLO;
         ClassXPManager.syncAll(server);
      } else {
         VoteManager.tickSecond();
         ClassXPManager.syncAll(server);
         if (VoteManager.isComplete()) {
            currentMode = VoteManager.resolve(server);
            broadcast(server, "[WAR] Результат голосования: " + currentMode.displayName() + ".");
            if (currentMode.isTeamMode()) {
               TeamMatchManager.startSelection(currentMode);
               matchPhase = MatchPhase.TEAM_SELECT;
               broadcast(server, "[WAR] Выбери команду в открытом окне.");
               giveLobbyTabletsAndSync(server);
            } else {
               startGame(server);
            }
         }
      }
   }

   private static void handleTeamSelectTick(MinecraftServer server) {
      if (currentMode.isTeamMode() && hasEnoughPlayersForMode(server, currentMode)) {
         TeamMatchManager.tickSecond();
         ClassXPManager.syncAll(server);
         if (TeamMatchManager.isSelectionComplete()) {
            TeamMatchManager.autoBalance(server, currentMode);
            startGame(server);
         }
      } else {
         TeamMatchManager.reset(server);
         currentMode = MatchMode.SOLO;
         matchPhase = MatchPhase.WAITING;
         ClassXPManager.syncAll(server);
      }
   }

   private static boolean canStartVoting(MinecraftServer server) {
      return onlinePlayers(server) >= MatchMode.DUO.minPlayers() || TestModeManager.canBypassTeamModeMinimums();
   }

   private static boolean hasEnoughPlayersForMode(MinecraftServer server, MatchMode mode) {
      return onlinePlayers(server) >= mode.minPlayers() || TestModeManager.canBypassTeamModeMinimums();
   }

   private static void showWinnerTitle(MinecraftServer server, String winnerName, TeamId winnerTeam) {
      boolean noWinner = winnerName == null
         || winnerName.isBlank()
         || "No winner".equalsIgnoreCase(winnerName)
         || "Нет победителя".equalsIgnoreCase(winnerName);
      MutableComponent title = Component.m_237113_(noWinner ? "НЕТ ПОБЕДИТЕЛЯ" : winnerName + " ПОБЕЖДАЕТ!");
      Component subtitle = Component.m_237113_(noWinner ? "Матч завершён." : "+10 опыта стандартным классам, +5 монет");
      MutableComponent chat = Component.m_237113_(noWinner ? "[WAR] Матч завершён без победителя." : "[WAR] ");
      if (!noWinner && winnerTeam != null) {
         title.m_130940_(winnerTeam.chatColor());
         chat.m_7220_(Component.m_237113_(winnerName).m_130940_(winnerTeam.chatColor())).m_7220_(Component.m_237113_(" побеждает!"));
      } else if (!noWinner) {
         chat.m_7220_(Component.m_237113_(winnerName + " побеждает!"));
      }

      for (ServerPlayer player : server.m_6846_().m_11314_()) {
         player.f_8906_.m_9829_(new ClientboundSetTitlesAnimationPacket(10, 100, 20));
         player.f_8906_.m_9829_(new ClientboundSetTitleTextPacket(title));
         player.f_8906_.m_9829_(new ClientboundSetSubtitleTextPacket(subtitle));
         player.m_213846_(chat);
      }
   }

   private static void broadcast(MinecraftServer server, String message) {
      Component component = Component.m_237113_(message);

      for (ServerPlayer player : server.m_6846_().m_11314_()) {
         player.m_213846_(component);
      }
   }

   private static void giveLobbyTabletsAndSync(MinecraftServer server) {
      for (ServerPlayer player : server.m_6846_().m_11314_()) {
         LobbyManager.giveTabletIfMissing(player);
         ClassXPManager.sync(player);
      }
   }

   private static void beginMapVoting(MinecraftServer server, boolean debug) {
      matchPhase = MatchPhase.MAP_VOTING;
      currentMode = MatchMode.SOLO;
      VoteManager.reset();
      TeamMatchManager.reset(server);
      giveLobbyTabletsAndSync(server);
      MapSetManager.startVoting(server, debug);
   }

   private static String describeVoteModes(MinecraftServer server) {
      int online = onlinePlayers(server);
      return MatchMode.selectableModes(online, TestModeManager.canBypassTeamModeMinimums())
         .stream()
         .map(MatchMode::displayName)
         .reduce((left, right) -> left + " / " + right)
         .orElse(MatchMode.SOLO.displayName());
   }
}
