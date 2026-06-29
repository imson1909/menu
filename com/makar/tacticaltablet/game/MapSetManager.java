package com.makar.tacticaltablet.game;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.makar.tacticaltablet.core.TacticalTabletMod;
import com.makar.tacticaltablet.map.MapRotationManager;
import com.makar.tacticaltablet.progression.ClassXPManager;
import com.makar.tacticaltablet.progression.PlayerProgressManager;
import com.makar.tacticaltablet.tablet.net.MapVoteStatePacket;
import com.makar.tacticaltablet.tablet.net.PacketHandler;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.LevelResource;

public final class MapSetManager {
   public static final int GAMES_PER_MAP = 4;
   public static final int MAP_VOTE_SECONDS = 30;
   public static final int RESTART_COUNTDOWN_SECONDS = 10;
   private static final int DATA_VERSION = 2;
   private static final String STATE_FILE = "map_set_state.json";
   private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
   private static final Random RANDOM = new Random();
   private static final List<String> TEST_MAPS = List.of(
      "Лесецк", "Дикий Запад", "Глубокая пещера", "Раскольск", "Манийск", "Завод", "Долина", "Аэродром", "Советский город"
   );
   private static final Map<UUID, String> votes = new HashMap<>();
   private static MapSetManager.SetState state = new MapSetManager.SetState();
   private static Path statePath;
   private static boolean voting;
   private static int voteSecondsLeft;
   private static int restartSecondsLeft = -1;
   private static String selectedMap = "";
   private static boolean stopIssued;

   private MapSetManager() {
   }

   public static synchronized void onServerStarted(MinecraftServer server) {
      resetRuntimeState();
      initStorage(server);
      MapRotationManager.RotationStatus rotationStatus = MapRotationManager.getStatus(server);
      String currentMap = currentMapName(server);
      String lastRotation = rotationStatus.lastRotation() == null ? "" : rotationStatus.lastRotation();
      if (state.mapName != null && !state.mapName.isBlank()) {
         if (normalize(state.mapName).equals(normalize(currentMap))
            && (state.lastRotation.isBlank() || lastRotation.isBlank() || lastRotation.equals(state.lastRotation))) {
            if (state.lastRotation.isBlank() && !lastRotation.isBlank()) {
               state.lastRotation = lastRotation;
               saveState();
            }
         } else {
            TacticalTabletMod.LOGGER
               .info(
                  "Tactical Tablet detected a completed map rotation. Resetting map set: {} -> {}, competitive={}, clanWar={}",
                  new Object[]{state.mapName, currentMap, state.nextSetCompetitive, state.nextSetClanWar}
               );
            state.mapName = currentMap;
            state.lastRotation = lastRotation;
            state.completedGames = 0;
            state.competitiveSet = state.nextSetCompetitive;
            state.clanWarSet = state.nextSetClanWar;
            state.nextSetCompetitive = false;
            state.nextSetClanWar = false;
            saveState();
         }
      } else {
         state.mapName = currentMap;
         state.lastRotation = lastRotation;
         state.completedGames = 0;
         saveState();
      }
   }

   public static synchronized void onServerStopped() {
      saveState();
      resetRuntimeState();
      statePath = null;
      state = new MapSetManager.SetState();
   }

   public static synchronized int getCurrentGameNumber() {
      return Math.min(4, Math.max(1, state.completedGames + 1));
   }

   public static synchronized int getCompletedGames() {
      return Math.max(0, Math.min(4, state.completedGames));
   }

   public static synchronized boolean onGameCompleted(MinecraftServer server) {
      initStorage(server);
      state.completedGames = Math.min(4, state.completedGames + 1);
      saveState();
      return state.completedGames >= 4;
   }

   public static synchronized boolean isSetComplete() {
      return state.completedGames >= 4;
   }

   public static synchronized boolean isCompetitiveSet() {
      return state.competitiveSet;
   }

   public static synchronized boolean isClanWarSet() {
      return state.clanWarSet;
   }

   public static synchronized boolean isNextSetCompetitive() {
      return state.nextSetCompetitive;
   }

   public static synchronized boolean isNextSetClanWar() {
      return state.nextSetClanWar;
   }

   public static synchronized void announceGameStart(MinecraftServer server) {
      if (server != null) {
         int game = getCurrentGameNumber();
         Component title = Component.m_237113_(game + "-я игра началась");
         Component subtitle = Component.m_237113_(state.competitiveSet ? "Соревновательный сет • игра " + game + " из 4" : "Игра " + game + " из 4");

         for (ServerPlayer player : server.m_6846_().m_11314_()) {
            player.f_8906_.m_9829_(new ClientboundSetTitlesAnimationPacket(10, 60, 20));
            player.f_8906_.m_9829_(new ClientboundSetTitleTextPacket(title));
            player.f_8906_.m_9829_(new ClientboundSetSubtitleTextPacket(subtitle));
         }
      }
   }

   public static synchronized void startVoting(MinecraftServer server, boolean debug) {
      if (server != null && !voting && restartSecondsLeft < 0) {
         initStorage(server);
         if (debug) {
            state.completedGames = 4;
            saveState();
         }

         votes.clear();
         selectedMap = "";
         voteSecondsLeft = 30;
         voting = true;
         stopIssued = false;
         state.nextSetCompetitive = false;
         state.nextSetClanWar = false;
         saveState();
         broadcast(
            server,
            debug
               ? "[WAR] Отладочное голосование за следующую карту началось. После выбора сервер будет перезапущен."
               : "[WAR] Сет из 4 игр завершён. Выберите следующую карту — осталось 30 секунд."
         );
         syncAll(server, true);
      }
   }

   public static synchronized void vote(ServerPlayer player, String mapName) {
      if (player != null && voting && GameStateManager.isInLobby(player)) {
         String canonical = canonicalMapName(mapName);
         if (canonical != null) {
            votes.put(player.m_20148_(), canonical);
            syncAll(player.f_8924_, false);
         }
      }
   }

   public static synchronized void setNextSetCompetitive(ServerPlayer player, boolean competitive) {
      if (player != null && player.m_20310_(2) && voting) {
         state.nextSetCompetitive = competitive;
         if (competitive) {
            state.nextSetClanWar = false;
         }

         saveState();
         broadcast(player.f_8924_, "[WAR] Следующий сет: " + (competitive ? "соревновательный" : "обычный casual") + ".");
         syncAll(player.f_8924_, false);
      }
   }

   public static synchronized void setNextSetClanWar(ServerPlayer player, boolean clanWar) {
      if (player != null && player.m_20310_(2) && voting) {
         state.nextSetClanWar = clanWar;
         if (clanWar) {
            state.nextSetCompetitive = false;
         }

         saveState();
         broadcast(player.f_8924_, "[WAR] Следующий сет: " + (clanWar ? "война кланов" : "обычный casual") + ".");
         syncAll(player.f_8924_, false);
      }
   }

   public static synchronized void setDebugClanWarSet(MinecraftServer server, boolean clanWar) {
      initStorage(server);
      state.clanWarSet = clanWar;
      if (clanWar) {
         state.competitiveSet = false;
         state.nextSetCompetitive = false;
      }

      saveState();
   }

   public static synchronized MapSetManager.VoteTickResult tickVoting(MinecraftServer server) {
      if (voting && server != null) {
         if (voteSecondsLeft > 0) {
            voteSecondsLeft--;
            ClassXPManager.syncAll(server);
            syncAll(server, false);
            if (voteSecondsLeft > 0) {
               return MapSetManager.VoteTickResult.ACTIVE;
            }
         }

         String winner = resolveWinner();

         try {
            MapRotationManager.setNextMap(server, winner);
            MapRotationManager.arm(server);
         } catch (IOException | RuntimeException exception) {
            TacticalTabletMod.LOGGER.error("Failed to prepare voted map {}", winner, exception);
            voteSecondsLeft = 10;
            broadcast(server, "[WAR] Не удалось подготовить карту «" + winner + "»: " + exception.getMessage() + ". Повторная попытка через 10 секунд.");
            syncAll(server, false);
            return MapSetManager.VoteTickResult.FAILED;
         }

         selectedMap = winner;
         voting = false;
         restartSecondsLeft = 10;
         broadcast(server, "[WAR] Выбрана карта «" + winner + "». Перезапуск сервера через 10 секунд.");
         syncAll(server, false);
         return MapSetManager.VoteTickResult.PREPARED;
      } else {
         return MapSetManager.VoteTickResult.FAILED;
      }
   }

   public static synchronized void tickRestart(MinecraftServer server) {
      if (server != null && restartSecondsLeft >= 0 && !stopIssued) {
         if (restartSecondsLeft == 0) {
            stopIssued = true;
            PlayerProgressManager.saveAll();
            broadcast(server, "[WAR] Сервер перезапускается для смены карты на «" + selectedMap + "».");
            server.m_7570_(false);
         } else {
            if (restartSecondsLeft <= 5 || restartSecondsLeft == 10) {
               broadcast(server, "[WAR] Перезапуск через " + restartSecondsLeft + " сек.");
            }

            restartSecondsLeft--;
         }
      }
   }

   public static synchronized void sync(ServerPlayer player, boolean openScreen) {
      if (player != null) {
         PacketHandler.sendToPlayer(player, createStatePacket(player, openScreen));
      }
   }

   public static synchronized boolean isVoting() {
      return voting;
   }

   public static synchronized int getVoteSecondsLeft() {
      return Math.max(0, voteSecondsLeft);
   }

   public static synchronized List<String> mapPool() {
      return TEST_MAPS;
   }

   private static MapVoteStatePacket createStatePacket(ServerPlayer player, boolean openScreen) {
      Map<String, Integer> counts = voteCounts();
      return new MapVoteStatePacket(
         voting,
         openScreen,
         player.m_20310_(2),
         state.nextSetCompetitive,
         state.nextSetClanWar,
         voteSecondsLeft,
         votes.getOrDefault(player.m_20148_(), ""),
         TEST_MAPS,
         counts
      );
   }

   private static void syncAll(MinecraftServer server, boolean openScreen) {
      if (server != null) {
         for (ServerPlayer player : server.m_6846_().m_11314_()) {
            sync(player, openScreen);
         }
      }
   }

   private static Map<String, Integer> voteCounts() {
      Map<String, Integer> counts = new LinkedHashMap<>();

      for (String map : TEST_MAPS) {
         counts.put(map, 0);
      }

      for (String vote : votes.values()) {
         counts.computeIfPresent(vote, (ignored, value) -> value + 1);
      }

      return counts;
   }

   private static String resolveWinner() {
      Map<String, Integer> counts = voteCounts();
      int best = counts.values().stream().max(Comparator.naturalOrder()).orElse(0);
      List<String> leaders = new ArrayList<>();

      for (String map : TEST_MAPS) {
         if (counts.getOrDefault(map, 0) == best) {
            leaders.add(map);
         }
      }

      return leaders.isEmpty() ? TEST_MAPS.get(0) : leaders.get(RANDOM.nextInt(leaders.size()));
   }

   private static String canonicalMapName(String value) {
      String normalized = normalize(value);

      for (String map : TEST_MAPS) {
         if (normalize(map).equals(normalized)) {
            return map;
         }
      }

      return null;
   }

   private static void initStorage(MinecraftServer server) {
      if (statePath == null && server != null) {
         Path worldRoot = server.m_129843_(LevelResource.f_78182_).toAbsolutePath().normalize();
         Path serverRoot = worldRoot.getParent() == null ? worldRoot : worldRoot.getParent();
         Path dataRoot = serverRoot.resolve("tacticaltablet_data");
         statePath = dataRoot.resolve("map_set_state.json");

         try {
            Files.createDirectories(dataRoot);
            if (Files.exists(statePath)) {
               try (Reader reader = Files.newBufferedReader(statePath, StandardCharsets.UTF_8)) {
                  MapSetManager.SetState loaded = (MapSetManager.SetState)GSON.fromJson(reader, MapSetManager.SetState.class);
                  if (loaded != null) {
                     state = loaded;
                  }
               }
            }

            normalizeState();
            saveState();
         } catch (IOException | RuntimeException exception) {
            TacticalTabletMod.LOGGER.error("Failed to initialize map set state at {}", statePath, exception);
            state = new MapSetManager.SetState();
         }
      }
   }

   private static void saveState() {
      if (statePath != null) {
         normalizeState();
         Path temp = statePath.resolveSibling(statePath.getFileName() + ".tmp");

         try {
            Files.createDirectories(statePath.getParent());

            try (Writer writer = Files.newBufferedWriter(temp, StandardCharsets.UTF_8)) {
               GSON.toJson(state, writer);
            }

            try {
               Files.move(temp, statePath, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException exception) {
               Files.move(temp, statePath, StandardCopyOption.REPLACE_EXISTING);
            }
         } catch (IOException exception) {
            TacticalTabletMod.LOGGER.error("Failed to save map set state at {}", statePath, exception);
         }
      }
   }

   private static void normalizeState() {
      state.dataVersion = 2;
      state.mapName = state.mapName == null ? "" : state.mapName.trim();
      state.lastRotation = state.lastRotation == null ? "" : state.lastRotation.trim();
      state.completedGames = Math.max(0, Math.min(4, state.completedGames));
      if (state.competitiveSet && state.clanWarSet) {
         state.clanWarSet = false;
      }

      if (state.nextSetCompetitive && state.nextSetClanWar) {
         state.nextSetClanWar = false;
      }
   }

   private static String currentMapName(MinecraftServer server) {
      try {
         MapRotationManager.RotationStatus status = MapRotationManager.getStatus(server);
         if (status.currentMap() != null && !status.currentMap().isBlank()) {
            return status.currentMap();
         }
      } catch (RuntimeException exception) {
         TacticalTabletMod.LOGGER.warn("Could not read current map from rotation state", exception);
      }

      return server.m_129843_(LevelResource.f_78182_).getFileName().toString();
   }

   private static void resetRuntimeState() {
      votes.clear();
      voting = false;
      voteSecondsLeft = 0;
      restartSecondsLeft = -1;
      selectedMap = "";
      stopIssued = false;
   }

   private static void broadcast(MinecraftServer server, String message) {
      if (server != null) {
         Component component = Component.m_237113_(message);

         for (ServerPlayer player : server.m_6846_().m_11314_()) {
            player.m_213846_(component);
         }
      }
   }

   private static String normalize(String value) {
      return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
   }

   private static final class SetState {
      int dataVersion = 2;
      String mapName = "";
      String lastRotation = "";
      int completedGames;
      boolean competitiveSet;
      boolean nextSetCompetitive;
      boolean clanWarSet;
      boolean nextSetClanWar;
   }

   public enum VoteTickResult {
      ACTIVE,
      PREPARED,
      FAILED;
   }
}
