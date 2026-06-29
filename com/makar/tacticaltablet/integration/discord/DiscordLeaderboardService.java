package com.makar.tacticaltablet.integration.discord;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.makar.tacticaltablet.clan.ClanManager;
import com.makar.tacticaltablet.core.TacticalTabletMod;
import com.makar.tacticaltablet.game.GameStateManager;
import com.makar.tacticaltablet.game.MapSetManager;
import com.makar.tacticaltablet.game.MatchMode;
import com.makar.tacticaltablet.game.team.TeamId;
import com.makar.tacticaltablet.map.MapRotationManager;
import com.makar.tacticaltablet.stats.PlayerStats;
import com.makar.tacticaltablet.stats.PlayerStatsRepository;
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
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.Map.Entry;
import java.util.concurrent.CompletableFuture;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.LevelResource;

public final class DiscordLeaderboardService {
   private static final int OVERALL_COLOR = 15844367;
   private static final int MATCH_COLOR = 15158332;
   private static final int RANKED_MATCH_COLOR = 10181046;
   private static final int CLAN_WAR_COLOR = 3066993;
   private static final int CLAN_WAR_WIN_CLAN_COINS = 10;
   private static final String SET_STATS_FILE = "map_set_stats.json";
   private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
   private static final Map<String, DiscordLeaderboardService.MatchStats> currentMatchStats = new HashMap<>();
   private static final Map<String, DiscordLeaderboardService.MatchStats> currentSetStats = new HashMap<>();
   private static long currentMatchStartedAtMillis;
   private static int currentMatchAirdrops;
   private static long currentSetStartedAtMillis;
   private static int currentSetAirdrops;
   private static int currentSetMatches;
   private static Path setStatsPath;
   private static String currentSetMap = "";

   private DiscordLeaderboardService() {
   }

   public static void init(MinecraftServer server) {
      DiscordConfig.get(server);
      loadSetState(server);
   }

   public static CompletableFuture<Boolean> sendOverallLeaderboard(MinecraftServer server) {
      DiscordConfig config = DiscordConfig.get(server);
      List<PlayerStats> players = PlayerStatsRepository.loadAll(server);
      return sendLeaderboard(config, "Общий рейтинг - " + config.getServerName(), players, config.getTopLimit(), 15844367);
   }

   public static CompletableFuture<Boolean> sendMatchLeaderboard(List<PlayerStats> players) {
      DiscordConfig config = DiscordConfig.get(null);
      DiscordWebhookClient.DiscordEmbed embed = buildLegacyMatchEmbed(
         "Итоги матча - " + config.getServerName(), MatchMode.SOLO, "", "", players, config.getTopLimit(), 15158332
      );
      return DiscordWebhookClient.sendEmbedAsync(config.getWebhookUrl(), embed);
   }

   public static CompletableFuture<Boolean> sendMatchLeaderboard(MinecraftServer server, List<PlayerStats> players) {
      DiscordConfig config = DiscordConfig.get(server);
      DiscordWebhookClient.DiscordEmbed embed = buildLegacyMatchEmbed(
         "Итоги матча - " + config.getServerName(), GameStateManager.getCurrentMode(), currentMapName(server), "", players, config.getTopLimit(), 15158332
      );
      return DiscordWebhookClient.sendEmbedAsync(config.getWebhookUrl(), embed);
   }

   public static synchronized void startMatch(MinecraftServer server) {
      currentMatchStats.clear();
      currentMatchStartedAtMillis = System.currentTimeMillis();
      currentMatchAirdrops = 0;
      if (currentSetStartedAtMillis <= 0L) {
         currentSetStartedAtMillis = currentMatchStartedAtMillis;
      }

      if (server != null) {
         for (ServerPlayer player : server.m_6846_().m_11314_()) {
            currentMatchStats.put(key(player), DiscordLeaderboardService.MatchStats.fromPlayer(player));
         }
      }
   }

   public static synchronized void resetMatch() {
      currentMatchStats.clear();
      currentMatchStartedAtMillis = 0L;
      currentMatchAirdrops = 0;
   }

   public static synchronized void recordMatchKill(ServerPlayer player) {
      if (player != null) {
         currentMatchStats.computeIfAbsent(key(player), ignored -> DiscordLeaderboardService.MatchStats.fromPlayer(player)).kills++;
      }
   }

   public static synchronized void recordMatchDeath(ServerPlayer player) {
      if (player != null) {
         currentMatchStats.computeIfAbsent(key(player), ignored -> DiscordLeaderboardService.MatchStats.fromPlayer(player)).deaths++;
      }
   }

   public static synchronized void recordMatchDamage(ServerPlayer attacker, double amount) {
      if (attacker != null && !(amount <= 0.0)) {
         currentMatchStats.computeIfAbsent(key(attacker), ignored -> DiscordLeaderboardService.MatchStats.fromPlayer(attacker)).damage += amount;
      }
   }

   public static synchronized int recordTeamKill(MinecraftServer server, ServerPlayer player) {
      if (server != null && player != null) {
         String key = key(player);
         DiscordLeaderboardService.MatchStats setStats = currentSetStats.computeIfAbsent(
            key, ignored -> DiscordLeaderboardService.MatchStats.fromPlayer(player)
         );
         setStats.teamKills++;
         if (currentSetMap == null || currentSetMap.isBlank()) {
            currentSetMap = currentMapName(server);
         }

         saveSetState(server);
         return setStats.teamKills;
      } else {
         return 0;
      }
   }

   public static synchronized void recordMatchAirdropStarted() {
      if (currentMatchStartedAtMillis > 0L) {
         currentMatchAirdrops++;
      }
   }

   public static synchronized DiscordLeaderboardService.SetWinner sendCurrentMatchLeaderboard(
      MinecraftServer server, ServerPlayer winner, boolean setComplete, boolean clanWarSet
   ) {
      if (currentMatchStats.isEmpty()) {
         return null;
      }

      if (winner != null) {
         currentMatchStats.computeIfAbsent(key(winner), ignored -> DiscordLeaderboardService.MatchStats.fromPlayer(winner)).wins = 1;
      }

      for (Entry<String, DiscordLeaderboardService.MatchStats> entry : currentMatchStats.entrySet()) {
         currentSetStats.computeIfAbsent(entry.getKey(), ignored -> new DiscordLeaderboardService.MatchStats(entry.getValue().uuid, entry.getValue().name))
            .merge(entry.getValue());
      }

      currentSetAirdrops = currentSetAirdrops + currentMatchAirdrops;
      currentSetMatches++;
      currentSetMap = currentMapName(server);
      saveSetState(server);
      currentMatchStats.clear();
      currentMatchStartedAtMillis = 0L;
      currentMatchAirdrops = 0;
      if (!setComplete) {
         return null;
      }

      if (clanWarSet) {
         DiscordLeaderboardService.ClanWarStats clanWinner = findClanWarSetWinner(server);
         if (clanWinner != null) {
            ClanManager.addClanCoins(server, clanWinner.clanId, 10);
         }

         sendClanWarSetReport(server, clanWinner);
         currentSetStats.clear();
         currentSetStartedAtMillis = 0L;
         currentSetAirdrops = 0;
         currentSetMatches = 0;
         currentSetMap = "";
         deleteSetState();
         return null;
      } else {
         DiscordLeaderboardService.SetWinner setWinner = findSetWinner(server);
         sendCurrentSetReport(server);
         currentSetStats.clear();
         currentSetStartedAtMillis = 0L;
         currentSetAirdrops = 0;
         currentSetMatches = 0;
         currentSetMap = "";
         deleteSetState();
         return setWinner;
      }
   }

   private static synchronized void loadSetState(MinecraftServer server) {
      if (server != null) {
         Path worldRoot = server.m_129843_(LevelResource.f_78182_).toAbsolutePath().normalize();
         Path serverRoot = worldRoot.getParent() == null ? worldRoot : worldRoot.getParent();
         setStatsPath = serverRoot.resolve("tacticaltablet_data").resolve("map_set_stats.json");
         currentSetStats.clear();
         currentSetStartedAtMillis = 0L;
         currentSetAirdrops = 0;
         currentSetMatches = 0;
         currentSetMap = "";
         if (Files.exists(setStatsPath)) {
            try (Reader reader = Files.newBufferedReader(setStatsPath, StandardCharsets.UTF_8)) {
               DiscordLeaderboardService.SetStatsState loaded = (DiscordLeaderboardService.SetStatsState)GSON.fromJson(
                  reader, DiscordLeaderboardService.SetStatsState.class
               );
               if (loaded != null && normalizeMapName(loaded.mapName).equals(normalizeMapName(currentMapName(server)))) {
                  currentSetMap = loaded.mapName == null ? "" : loaded.mapName;
                  currentSetStartedAtMillis = Math.max(0L, loaded.startedAtMillis);
                  currentSetAirdrops = Math.max(0, loaded.airdrops);
                  currentSetMatches = Math.max(0, loaded.matches);
                  if (loaded.players != null) {
                     currentSetStats.putAll(loaded.players);
                  }
               } else {
                  deleteSetState();
               }
            } catch (IOException | RuntimeException exception) {
               TacticalTabletMod.LOGGER.error("Failed to load map set stats from {}", setStatsPath, exception);
               deleteSetState();
            }
         }
      }
   }

   private static synchronized void saveSetState(MinecraftServer server) {
      if (server != null) {
         if (setStatsPath == null) {
            Path worldRoot = server.m_129843_(LevelResource.f_78182_).toAbsolutePath().normalize();
            Path serverRoot = worldRoot.getParent() == null ? worldRoot : worldRoot.getParent();
            setStatsPath = serverRoot.resolve("tacticaltablet_data").resolve("map_set_stats.json");
         }

         DiscordLeaderboardService.SetStatsState snapshot = new DiscordLeaderboardService.SetStatsState();
         snapshot.mapName = currentSetMap;
         snapshot.startedAtMillis = currentSetStartedAtMillis;
         snapshot.airdrops = currentSetAirdrops;
         snapshot.matches = currentSetMatches;
         snapshot.players = new HashMap<>(currentSetStats);
         Path temp = setStatsPath.resolveSibling(setStatsPath.getFileName() + ".tmp");

         try {
            Files.createDirectories(setStatsPath.getParent());

            try (Writer writer = Files.newBufferedWriter(temp, StandardCharsets.UTF_8)) {
               GSON.toJson(snapshot, writer);
            }

            try {
               Files.move(temp, setStatsPath, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException exception) {
               Files.move(temp, setStatsPath, StandardCopyOption.REPLACE_EXISTING);
            }
         } catch (IOException exception) {
            TacticalTabletMod.LOGGER.error("Failed to save map set stats to {}", setStatsPath, exception);
         }
      }
   }

   private static synchronized void deleteSetState() {
      if (setStatsPath != null) {
         try {
            Files.deleteIfExists(setStatsPath);
         } catch (IOException exception) {
            TacticalTabletMod.LOGGER.warn("Failed to delete completed map set stats at {}", setStatsPath, exception);
         }
      }
   }

   private static String normalizeMapName(String value) {
      return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
   }

   private static CompletableFuture<Boolean> sendCurrentSetReport(MinecraftServer server) {
      DiscordConfig config = DiscordConfig.get(server);
      if (config != null && config.hasWebhook()) {
         List<DiscordLeaderboardService.MatchStats> players = sortedSet(new ArrayList<>(currentSetStats.values()));
         DiscordLeaderboardService.MatchStats winner = players.isEmpty() ? null : players.get(0);
         DiscordLeaderboardService.MatchStats damageLeader = damageLeader(players);
         DiscordWebhookClient.DiscordEmbed embed = buildSetEmbed(
            (MapSetManager.isCompetitiveSet() ? "Итоги рейтингового матча" : "Итоги сета") + " из " + currentSetMatches + " игр - " + config.getServerName(),
            currentMapName(server),
            players,
            config.getTopLimit(),
            currentSetStartedAtMillis,
            currentSetAirdrops,
            winner,
            damageLeader
         );
         return DiscordWebhookClient.sendEmbedAsync(config.getWebhookUrl(), embed);
      } else {
         TacticalTabletMod.LOGGER.warn("Discord webhook is not configured. Set webhookUrl in config/tacticaltablet_discord.json");
         return CompletableFuture.completedFuture(false);
      }
   }

   private static DiscordLeaderboardService.SetWinner findSetWinner(MinecraftServer server) {
      if (currentSetStats.isEmpty()) {
         return null;
      }

      List<DiscordLeaderboardService.MatchStats> sorted = sortedSet(new ArrayList<>(currentSetStats.values()));
      if (sorted.isEmpty()) {
         return null;
      }

      DiscordLeaderboardService.MatchStats winner = sorted.get(0);

      try {
         return new DiscordLeaderboardService.SetWinner(UUID.fromString(winner.uuid), winner.name);
      } catch (RuntimeException exception) {
         if (server != null) {
            for (ServerPlayer player : server.m_6846_().m_11314_()) {
               if (player.m_36316_().getName().equalsIgnoreCase(winner.name)) {
                  return new DiscordLeaderboardService.SetWinner(player.m_20148_(), winner.name);
               }
            }
         }

         TacticalTabletMod.LOGGER.warn("Cannot award set winner {}: invalid UUID {}", winner.name, winner.uuid);
         return null;
      }
   }

   private static CompletableFuture<Boolean> sendClanWarSetReport(MinecraftServer server, DiscordLeaderboardService.ClanWarStats winner) {
      DiscordConfig config = DiscordConfig.get(server);
      if (config != null && config.hasClanWarWebhook()) {
         List<DiscordLeaderboardService.ClanWarStats> clans = sortedClanWarSet(buildClanWarStats(server));
         DiscordLeaderboardService.ClanWarStats damageLeader = clanWarDamageLeader(clans);
         DiscordWebhookClient.DiscordEmbed embed = buildClanWarSetEmbed(
            "Итоги войны кланов из " + currentSetMatches + " игр - " + config.getServerName(),
            currentMapName(server),
            clans,
            config.getTopLimit(),
            currentSetStartedAtMillis,
            currentSetAirdrops,
            winner,
            damageLeader
         );
         return DiscordWebhookClient.sendEmbedAsync(config.getClanWarWebhookUrl(), embed);
      } else {
         TacticalTabletMod.LOGGER.warn("Discord clan-war webhook is not configured. Set clanWarWebhookUrl in config/tacticaltablet_discord.json");
         return CompletableFuture.completedFuture(false);
      }
   }

   private static DiscordLeaderboardService.ClanWarStats findClanWarSetWinner(MinecraftServer server) {
      List<DiscordLeaderboardService.ClanWarStats> clans = sortedClanWarSet(buildClanWarStats(server));
      return clans.isEmpty() ? null : clans.get(0);
   }

   private static List<DiscordLeaderboardService.ClanWarStats> buildClanWarStats(MinecraftServer server) {
      Map<String, DiscordLeaderboardService.ClanWarStats> clans = new HashMap<>();
      if (server == null) {
         return List.of();
      }

      for (DiscordLeaderboardService.MatchStats stats : currentSetStats.values()) {
         String clanId = ClanManager.getClanIdForPlayerUuid(server, stats.uuid);
         if (!clanId.isBlank()) {
            DiscordLeaderboardService.ClanWarStats clan = clans.computeIfAbsent(
               clanId,
               ignored -> new DiscordLeaderboardService.ClanWarStats(
                  clanId, ClanManager.getClanNameById(server, clanId), ClanManager.getClanCoinsById(server, clanId)
               )
            );
            clan.merge(stats);
         }
      }

      return new ArrayList<>(clans.values());
   }

   public static List<PlayerStats> sorted(List<PlayerStats> players) {
      List<PlayerStats> result = new ArrayList<>(players == null ? List.of() : players);
      result.sort(
         Comparator.comparingInt(PlayerStats::getKills)
            .reversed()
            .thenComparing(Comparator.comparingDouble(PlayerStats::getKd).reversed())
            .thenComparing(Comparator.comparingInt(PlayerStats::getWins).reversed())
            .thenComparing(PlayerStats::getName, String.CASE_INSENSITIVE_ORDER)
      );
      return result;
   }

   private static CompletableFuture<Boolean> sendLeaderboard(DiscordConfig config, String title, List<PlayerStats> players, int topLimit, int color) {
      if (config != null && config.hasWebhook()) {
         DiscordWebhookClient.DiscordEmbed message = buildOverallEmbed(title, players, topLimit, color);
         return DiscordWebhookClient.sendEmbedAsync(config.getWebhookUrl(), message);
      } else {
         TacticalTabletMod.LOGGER.warn("Discord webhook is not configured. Set webhookUrl in config/tacticaltablet_discord.json");
         return CompletableFuture.completedFuture(false);
      }
   }

   private static CompletableFuture<Boolean> sendMatchReport(
      MinecraftServer server,
      List<DiscordLeaderboardService.MatchStats> players,
      String winnerName,
      long startedAtMillis,
      int airdrops,
      MatchMode mode,
      Map<TeamId, List<String>> teams
   ) {
      DiscordConfig config = DiscordConfig.get(server);
      if (config != null && config.hasWebhook()) {
         MatchMode safeMode = mode == null ? MatchMode.SOLO : mode;
         List<DiscordLeaderboardService.MatchStats> sorted = sortedMatch(players);
         DiscordLeaderboardService.MatchStats damageLeader = damageLeader(sorted);
         DiscordWebhookClient.DiscordEmbed embed = buildMatchEmbed(
            "Итоги матча - " + config.getServerName(),
            currentMapName(server),
            winnerName,
            sorted,
            config.getTopLimit(),
            startedAtMillis,
            airdrops,
            damageLeader,
            safeMode,
            teams
         );
         return DiscordWebhookClient.sendEmbedAsync(config.getWebhookUrl(), embed);
      } else {
         TacticalTabletMod.LOGGER.warn("Discord webhook is not configured. Set webhookUrl in config/tacticaltablet_discord.json");
         return CompletableFuture.completedFuture(false);
      }
   }

   private static Map<String, DiscordLeaderboardService.MatchStats> matchStatsByName(List<DiscordLeaderboardService.MatchStats> players) {
      Map<String, DiscordLeaderboardService.MatchStats> result = new HashMap<>();
      if (players == null) {
         return result;
      }

      for (DiscordLeaderboardService.MatchStats stats : players) {
         if (stats != null) {
            result.put(normalizeStatsName(stats.name), stats);
         }
      }

      return result;
   }

   private static DiscordWebhookClient.DiscordEmbed buildOverallEmbed(String title, List<PlayerStats> players, int topLimit, int color) {
      List<PlayerStats> sorted = sorted(players);
      int count = Math.min(Math.max(1, topLimit), sorted.size());
      StringBuilder description = new StringBuilder();
      if (sorted.isEmpty()) {
         description.append("Нет данных.");
      } else {
         description.append("**Рейтинг игроков**\n");

         for (int i = 0; i < count; i++) {
            PlayerStats stats = sorted.get(i);
            description.append('\n')
               .append("**#")
               .append(i + 1)
               .append(" ")
               .append(stats.getName())
               .append("**\n")
               .append("Убийства: **`")
               .append(stats.getKills())
               .append("`**  ")
               .append("Смерти: `")
               .append(stats.getDeaths())
               .append("`  ")
               .append("У/С: `")
               .append(stats.getFormattedKd())
               .append("`\n")
               .append("Победы: `")
               .append(stats.getWins())
               .append("`  ")
               .append("Матчи: `")
               .append(stats.getMatchesPlayed())
               .append("`  ")
               .append("Монеты: `")
               .append(stats.getCoins())
               .append("`\n");
         }
      }

      return new DiscordWebhookClient.DiscordEmbed(title, description.toString(), color, "Тактический планшет");
   }

   private static DiscordWebhookClient.DiscordEmbed buildLegacyMatchEmbed(
      String title, MatchMode mode, String mapName, String winnerName, List<PlayerStats> players, int topLimit, int color
   ) {
      List<PlayerStats> sorted = sorted(players);
      int count = Math.min(Math.max(1, topLimit), sorted.size());
      StringBuilder description = new StringBuilder();
      appendHeaderLine(description, "Режим", (mode == null ? MatchMode.SOLO : mode).displayName());
      appendHeaderLine(description, "Карта", mapName);
      appendHeaderLine(description, "Победитель", winnerName);
      if (sorted.isEmpty()) {
         description.append("\nНет данных по матчу.");
      } else {
         description.append("\n**Результаты игроков**\n");

         for (int i = 0; i < count; i++) {
            PlayerStats stats = sorted.get(i);
            description.append('\n')
               .append("**#")
               .append(i + 1)
               .append(" ")
               .append(stats.getName())
               .append("**\n")
               .append("Убийства: `")
               .append(stats.getKills())
               .append("`  ")
               .append("Смерти: `")
               .append(stats.getDeaths())
               .append("`  ")
               .append("У/С: `")
               .append(stats.getFormattedKd())
               .append("`  ")
               .append("Монеты: `")
               .append(stats.getCoins())
               .append("`\n");
         }
      }

      return new DiscordWebhookClient.DiscordEmbed(title, description.toString(), color, "Тактический планшет");
   }

   private static DiscordWebhookClient.DiscordEmbed buildMatchEmbed(
      String title,
      String mapName,
      String winnerName,
      List<DiscordLeaderboardService.MatchStats> players,
      int topLimit,
      long startedAtMillis,
      int airdrops,
      DiscordLeaderboardService.MatchStats damageLeader,
      MatchMode mode,
      Map<TeamId, List<String>> teams
   ) {
      MatchMode safeMode = mode == null ? MatchMode.SOLO : mode;
      int count = Math.min(Math.max(1, topLimit), players.size());
      StringBuilder description = new StringBuilder();
      appendHeaderLine(description, "Режим", safeMode.displayName());
      appendHeaderLine(description, "Карта", mapName);
      appendHeaderLine(description, "Длительность", formatDuration(startedAtMillis));
      description.append("**Сбросов за матч:** `").append(Math.max(0, airdrops)).append("`\n");
      if (winnerName != null && !winnerName.isBlank()) {
         DiscordLeaderboardService.MatchStats winner = findMatchPlayer(players, winnerName);
         description.append("**Победитель:** **").append(winnerName).append("**");
         if (winner != null) {
            description.append("  Убийства `").append(winner.kills).append("`").append("  Урон `").append(formatDamage(winner.damage)).append("`");
         }

         description.append('\n');
      }

      if (damageLeader != null) {
         description.append("**Топ урон:** `").append(damageLeader.name).append("`  `").append(formatDamage(damageLeader.damage)).append("`\n");
      }

      if (players.isEmpty()) {
         description.append("\nНет данных по матчу.");
      } else if (safeMode.isTeamMode()) {
         appendTeamMatchResults(description, teams, players);
      } else {
         description.append("\n**Результаты игроков**\n");

         for (int i = 0; i < count; i++) {
            appendMatchPlayerRow(description, i + 1, players.get(i));
         }
      }

      return new DiscordWebhookClient.DiscordEmbed(title, description.toString(), 15158332, "Тактический планшет");
   }

   private static DiscordWebhookClient.DiscordEmbed buildSetEmbed(
      String title,
      String mapName,
      List<DiscordLeaderboardService.MatchStats> players,
      int topLimit,
      long startedAtMillis,
      int airdrops,
      DiscordLeaderboardService.MatchStats winner,
      DiscordLeaderboardService.MatchStats damageLeader
   ) {
      int count = Math.min(Math.max(1, topLimit), players.size());
      StringBuilder description = new StringBuilder();
      appendHeaderLine(description, "Карта", mapName);
      appendHeaderLine(description, "Длительность сета", formatDuration(startedAtMillis));
      description.append("**Игры:** `").append(currentSetMatches).append("`\n");
      description.append("**Аирдропы за сет:** `").append(Math.max(0, airdrops)).append("`\n");
      if (winner != null) {
         description.append("**Победитель сета:** **")
            .append(winner.name)
            .append("**  ")
            .append("Победы `")
            .append(winner.wins)
            .append("`  ")
            .append("Убийства `")
            .append(winner.kills)
            .append("`  ")
            .append("Урон `")
            .append(formatDamage(winner.damage))
            .append("`\n");
      }

      if (damageLeader != null) {
         description.append("**Топ урон:** `").append(damageLeader.name).append("`  `").append(formatDamage(damageLeader.damage)).append("`\n");
      }

      if (players.isEmpty()) {
         description.append("\nНет данных по сету.");
      } else {
         description.append("\n**Суммарные результаты игроков**\n");

         for (int i = 0; i < count; i++) {
            appendSetPlayerRow(description, i + 1, players.get(i));
         }
      }

      int color = MapSetManager.isCompetitiveSet() ? 10181046 : 15158332;
      return new DiscordWebhookClient.DiscordEmbed(title, description.toString(), color, "Тактический планшет");
   }

   private static DiscordWebhookClient.DiscordEmbed buildClanWarSetEmbed(
      String title,
      String mapName,
      List<DiscordLeaderboardService.ClanWarStats> clans,
      int topLimit,
      long startedAtMillis,
      int airdrops,
      DiscordLeaderboardService.ClanWarStats winner,
      DiscordLeaderboardService.ClanWarStats damageLeader
   ) {
      int count = Math.min(Math.max(1, topLimit), clans.size());
      StringBuilder description = new StringBuilder();
      appendHeaderLine(description, "Карта", mapName);
      appendHeaderLine(description, "Длительность сета", formatDuration(startedAtMillis));
      description.append("**Игры:** `").append(currentSetMatches).append("`\n");
      description.append("**Аирдропы за сет:** `").append(Math.max(0, airdrops)).append("`\n");
      if (winner != null) {
         description.append("**Победитель:** **")
            .append(winner.name)
            .append("**  ")
            .append("+`")
            .append(10)
            .append("` КК  ")
            .append("Победы `")
            .append(winner.wins)
            .append("`  ")
            .append("Убийства `")
            .append(winner.kills)
            .append("`  ")
            .append("Урон `")
            .append(formatDamage(winner.damage))
            .append("`\n");
      }

      if (damageLeader != null) {
         description.append("**Топ урон:** `").append(damageLeader.name).append("`  `").append(formatDamage(damageLeader.damage)).append("`\n");
      }

      if (clans.isEmpty()) {
         description.append("\nНет данных по кланам.");
      } else {
         description.append("\n**Места кланов**\n");

         for (int i = 0; i < count; i++) {
            appendClanWarRow(description, i + 1, clans.get(i));
         }
      }

      return new DiscordWebhookClient.DiscordEmbed(title, description.toString(), 3066993, "Тактический планшет");
   }

   private static void appendClanWarRow(StringBuilder description, int place, DiscordLeaderboardService.ClanWarStats stats) {
      description.append('\n')
         .append("**#")
         .append(place)
         .append(' ')
         .append(stats.name)
         .append("**\n")
         .append("Победы: **`")
         .append(stats.wins)
         .append("`**  ")
         .append("Убийства: **`")
         .append(stats.kills)
         .append("`**  ")
         .append("Урон: **`")
         .append(formatDamage(stats.damage))
         .append("`**  ")
         .append("Смерти: `")
         .append(stats.deaths)
         .append("`  ")
         .append("У/С: `")
         .append(stats.formattedKd())
         .append("`  ")
         .append("КК: `")
         .append(stats.clanCoins)
         .append("`\n");
   }

   private static void appendSetPlayerRow(StringBuilder description, int place, DiscordLeaderboardService.MatchStats stats) {
      description.append('\n')
         .append("**#")
         .append(place)
         .append(' ')
         .append(stats.name)
         .append("**\n")
         .append("Победы: **`")
         .append(stats.wins)
         .append("`**  ")
         .append("Убийства: **`")
         .append(stats.kills)
         .append("`**  ")
         .append("Урон: **`")
         .append(formatDamage(stats.damage))
         .append("`**  ")
         .append("Смерти: `")
         .append(stats.deaths)
         .append("`  ")
         .append("У/С: `")
         .append(stats.formattedKd())
         .append("`  ")
         .append("Монеты: `")
         .append(stats.coinsEarned())
         .append("`\n");
   }

   private static void appendMatchPlayerRow(StringBuilder description, int place, DiscordLeaderboardService.MatchStats stats) {
      description.append('\n').append("**#").append(place).append(" ").append(stats.name).append("**");
      if (stats.wins > 0) {
         description.append("  `ПОБЕДА`");
      }

      description.append('\n')
         .append("Убийства: **`")
         .append(stats.kills)
         .append("`**  ")
         .append("Урон: **`")
         .append(formatDamage(stats.damage))
         .append("`**  ")
         .append("Смерти: `")
         .append(stats.deaths)
         .append("`  ")
         .append("У/С: `")
         .append(stats.formattedKd())
         .append("`  ")
         .append("Монеты: `")
         .append(stats.coinsEarned())
         .append("`\n");
   }

   private static void appendTeamMatchResults(StringBuilder description, Map<TeamId, List<String>> teams, List<DiscordLeaderboardService.MatchStats> players) {
      Map<String, DiscordLeaderboardService.MatchStats> statsByName = matchStatsByName(players);
      boolean anyTeam = false;
      description.append("\n**Команды**\n");

      for (TeamId teamId : TeamId.values()) {
         List<String> members = teams == null ? List.of() : teams.getOrDefault(teamId, List.of());
         if (!members.isEmpty()) {
            anyTeam = true;
            description.append('\n').append("**").append(teamId.displayName()).append("**\n");

            for (String name : members) {
               DiscordLeaderboardService.MatchStats stats = statsByName.get(normalizeStatsName(name));
               if (stats == null) {
                  description.append("**")
                     .append(safeDisplayName(name))
                     .append("**\n")
                     .append("Убийства: **`0`**  ")
                     .append("Урон: **`0.0`**  ")
                     .append("Смерти: `0`  ")
                     .append("У/С: `0.00`  ")
                     .append("Монеты: `0`\n");
               } else {
                  appendTeamPlayerRow(description, stats);
               }
            }
         }
      }

      if (!anyTeam) {
         description.append("Нет данных по командам.");
      }
   }

   private static void appendTeamPlayerRow(StringBuilder description, DiscordLeaderboardService.MatchStats stats) {
      description.append("**").append(stats.name).append("**");
      if (stats.wins > 0) {
         description.append("  `ПОБЕДА`");
      }

      description.append('\n')
         .append("Убийства: **`")
         .append(stats.kills)
         .append("`**  ")
         .append("Урон: **`")
         .append(formatDamage(stats.damage))
         .append("`**  ")
         .append("Смерти: `")
         .append(stats.deaths)
         .append("`  ")
         .append("У/С: `")
         .append(stats.formattedKd())
         .append("`  ")
         .append("Монеты: `")
         .append(stats.coinsEarned())
         .append("`\n");
   }

   private static void appendHeaderLine(StringBuilder description, String label, String value) {
      if (value != null && !value.isBlank()) {
         description.append("**").append(label).append(":** `").append(value).append("`\n");
      }
   }

   private static List<DiscordLeaderboardService.MatchStats> sortedMatch(List<DiscordLeaderboardService.MatchStats> players) {
      List<DiscordLeaderboardService.MatchStats> result = new ArrayList<>(players == null ? List.of() : players);
      result.sort(
         Comparator.<DiscordLeaderboardService.MatchStats>comparingInt(stats -> stats.kills)
            .reversed()
            .thenComparing(Comparator.<DiscordLeaderboardService.MatchStats>comparingDouble(stats -> stats.damage).reversed())
            .thenComparingInt(stats -> stats.deaths)
            .thenComparing(stats -> stats.name, String.CASE_INSENSITIVE_ORDER)
      );
      return result;
   }

   private static List<DiscordLeaderboardService.MatchStats> sortedSet(List<DiscordLeaderboardService.MatchStats> players) {
      List<DiscordLeaderboardService.MatchStats> result = new ArrayList<>(players == null ? List.of() : players);
      result.sort(
         Comparator.<DiscordLeaderboardService.MatchStats>comparingInt(stats -> stats.wins)
            .reversed()
            .thenComparing(Comparator.<DiscordLeaderboardService.MatchStats>comparingInt(stats -> stats.kills).reversed())
            .thenComparing(Comparator.<DiscordLeaderboardService.MatchStats>comparingDouble(stats -> stats.damage).reversed())
            .thenComparingInt(stats -> stats.deaths)
            .thenComparing(stats -> stats.name, String.CASE_INSENSITIVE_ORDER)
      );
      return result;
   }

   private static List<DiscordLeaderboardService.ClanWarStats> sortedClanWarSet(List<DiscordLeaderboardService.ClanWarStats> clans) {
      List<DiscordLeaderboardService.ClanWarStats> result = new ArrayList<>(clans == null ? List.of() : clans);
      result.sort(
         Comparator.<DiscordLeaderboardService.ClanWarStats>comparingInt(stats -> stats.wins)
            .reversed()
            .thenComparing(Comparator.<DiscordLeaderboardService.ClanWarStats>comparingInt(stats -> stats.kills).reversed())
            .thenComparing(Comparator.<DiscordLeaderboardService.ClanWarStats>comparingDouble(stats -> stats.damage).reversed())
            .thenComparingInt(stats -> stats.deaths)
            .thenComparing(stats -> stats.name, String.CASE_INSENSITIVE_ORDER)
      );
      return result;
   }

   private static DiscordLeaderboardService.MatchStats damageLeader(List<DiscordLeaderboardService.MatchStats> players) {
      DiscordLeaderboardService.MatchStats best = null;

      for (DiscordLeaderboardService.MatchStats stats : players) {
         if (best == null || stats.damage > best.damage) {
            best = stats;
         }
      }

      return best != null && best.damage > 0.0 ? best : null;
   }

   private static DiscordLeaderboardService.ClanWarStats clanWarDamageLeader(List<DiscordLeaderboardService.ClanWarStats> clans) {
      DiscordLeaderboardService.ClanWarStats best = null;

      for (DiscordLeaderboardService.ClanWarStats stats : clans) {
         if (best == null || stats.damage > best.damage) {
            best = stats;
         }
      }

      return best != null && best.damage > 0.0 ? best : null;
   }

   private static DiscordLeaderboardService.MatchStats findMatchPlayer(List<DiscordLeaderboardService.MatchStats> players, String name) {
      if (name != null && !name.isBlank()) {
         for (DiscordLeaderboardService.MatchStats stats : players) {
            if (stats.name.equalsIgnoreCase(name)) {
               return stats;
            }
         }

         return null;
      } else {
         return null;
      }
   }

   private static String normalizeStatsName(String name) {
      return safeTableName(name).toLowerCase(Locale.ROOT);
   }

   private static String safeDisplayName(String value) {
      return safeTableName(value);
   }

   private static String safeTableName(String value) {
      return value != null && !value.isBlank() ? value.replace('\n', '_').replace('\r', '_').replace('`', '\'').trim() : "unknown";
   }

   private static String formatDuration(long startedAtMillis) {
      if (startedAtMillis <= 0L) {
         return "неизвестно";
      }

      long totalSeconds = Math.max(0L, (System.currentTimeMillis() - startedAtMillis) / 1000L);
      long minutes = totalSeconds / 60L;
      long seconds = totalSeconds % 60L;
      return String.format(Locale.ROOT, "%d мин %02d сек", minutes, seconds);
   }

   private static String formatDamage(double damage) {
      return String.format(Locale.US, "%.1f", Math.max(0.0, damage));
   }

   private static String key(ServerPlayer player) {
      return player.m_20149_();
   }

   private static String currentMapName(MinecraftServer server) {
      if (server == null) {
         return "";
      }

      try {
         MapRotationManager.RotationStatus status = MapRotationManager.getStatus(server);
         if (status.currentMap() != null && !status.currentMap().isBlank()) {
            return status.currentMap();
         }
      } catch (RuntimeException exception) {
         TacticalTabletMod.LOGGER.warn("Failed to read map name for Discord match leaderboard", exception);
      }

      try {
         return server.m_129843_(LevelResource.f_78182_).getFileName().toString();
      } catch (RuntimeException exception) {
         return "";
      }
   }

   private static final class ClanWarStats {
      private final String clanId;
      private final String name;
      private final int clanCoins;
      private int kills;
      private int deaths;
      private int wins;
      private double damage;

      private ClanWarStats(String clanId, String name, int clanCoins) {
         this.clanId = clanId == null ? "" : clanId;
         this.name = DiscordLeaderboardService.MatchStats.sanitizeName(name != null && !name.isBlank() ? name : "unknown");
         this.clanCoins = Math.max(0, clanCoins);
      }

      private void merge(DiscordLeaderboardService.MatchStats other) {
         if (other != null) {
            this.kills = this.kills + other.kills;
            this.deaths = this.deaths + other.deaths;
            this.wins = this.wins + other.wins;
            this.damage = this.damage + other.damage;
         }
      }

      private double kd() {
         return this.deaths <= 0 ? this.kills : (double)this.kills / this.deaths;
      }

      private String formattedKd() {
         return String.format(Locale.US, "%.2f", this.kd());
      }
   }

   private static final class MatchStats {
      private final String uuid;
      private final String name;
      private int kills;
      private int deaths;
      private int wins;
      private double damage;
      private int teamKills;

      private MatchStats(String name) {
         this("", name);
      }

      private MatchStats(String uuid, String name) {
         this.uuid = uuid == null ? "" : uuid;
         this.name = sanitizeName(name);
      }

      private static DiscordLeaderboardService.MatchStats fromPlayer(ServerPlayer player) {
         return new DiscordLeaderboardService.MatchStats(player.m_20149_(), player.m_36316_().getName());
      }

      private double kd() {
         return this.deaths <= 0 ? this.kills : (double)this.kills / this.deaths;
      }

      private String formattedKd() {
         return String.format(Locale.US, "%.2f", this.kd());
      }

      private int coinsEarned() {
         return this.kills * 2 + this.wins * 5;
      }

      private PlayerStats toPlayerStats() {
         return new PlayerStats(this.name, this.kills, this.deaths, this.wins, 1, this.coinsEarned());
      }

      private void merge(DiscordLeaderboardService.MatchStats other) {
         if (other != null) {
            this.kills = this.kills + other.kills;
            this.deaths = this.deaths + other.deaths;
            this.wins = this.wins + other.wins;
            this.damage = this.damage + other.damage;
            this.teamKills = this.teamKills + other.teamKills;
         }
      }

      private static String sanitizeName(String value) {
         return value != null && !value.isBlank() ? value.replace('\n', '_').replace('\r', '_').replace('`', '\'').trim() : "unknown";
      }
   }

   private static final class SetStatsState {
      String mapName = "";
      long startedAtMillis;
      int airdrops;
      int matches;
      Map<String, DiscordLeaderboardService.MatchStats> players = new HashMap<>();
   }

   public record SetWinner(UUID uuid, String name) {
   }
}
