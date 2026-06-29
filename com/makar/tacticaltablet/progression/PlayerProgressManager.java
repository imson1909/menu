package com.makar.tacticaltablet.progression;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import com.makar.tacticaltablet.core.TacticalTabletMod;
import com.makar.tacticaltablet.game.MapSetManager;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.Map.Entry;
import java.util.stream.Stream;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.LevelResource;

public class PlayerProgressManager {
   private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
   private static final String DATA_DIRECTORY = "tacticaltablet_data";
   private static final String PLAYERS_DIRECTORY = "players";
   private static final String BACKUPS_DIRECTORY = "backups";
   private static final String SEASON_FILE = "season.json";
   private static final int DATA_VERSION = 8;
   public static final int STANDARD_TIER = 0;
   public static final int EPIC_TIER = 1;
   public static final int LEGEND_TIER = 2;
   public static final int EPIC_XP = 300;
   public static final int LEGEND_XP = 800;
   public static final int BASE_UNLOCK_COST = 25;
   public static final int EPIC_UPGRADE_COST = 20;
   public static final int LEGEND_UPGRADE_COST = 50;
   public static final int KILL_COIN_REWARD = 2;
   public static final int WIN_COIN_REWARD = 5;
   private static final String[] INITIAL_BASE_CLASSES = new String[]{"stormtrooper", "sniper", "scout"};
   private static final String[] BASE_CLASSES = new String[]{"stormtrooper", "sniper", "scout", "droneoperator", "machinegunner", "mortarman", "rpgtrooper"};
   private static final String[] SHOP_CLASSES = new String[]{"boomguy", "dream", "tagilla", "blackops", "cowboy", "solider", "rebel", "saboteur"};
   private static final String[] EXCLUSIVE_CLASSES = new String[]{"killer", "miniboss", "shahed", "krot"};
   private static final String[] ALL_CLASSES = new String[]{
      "stormtrooper",
      "sniper",
      "scout",
      "droneoperator",
      "machinegunner",
      "mortarman",
      "rpgtrooper",
      "boomguy",
      "dream",
      "tagilla",
      "blackops",
      "cowboy",
      "solider",
      "rebel",
      "saboteur"
   };
   private static final Map<String, Integer> SHOP_CLASS_PRICES = Map.of(
      "boomguy", 500, "dream", 500, "tagilla", 750, "blackops", 1000, "cowboy", 100, "solider", 50, "rebel", 1000, "saboteur", 1000
   );
   private static final Map<String, Integer> SHOP_CLASS_LEVELS = Map.of(
      "boomguy", 2, "dream", 2, "tagilla", 2, "blackops", 2, "cowboy", 1, "solider", 0, "rebel", 2, "saboteur", 2
   );
   private static final int AUTOSAVE_INTERVAL_TICKS = 1200;
   private static final int BACKUP_INTERVAL_TICKS = 36000;
   private static final int MAX_BACKUP_FOLDERS = 48;
   private static final DateTimeFormatter BACKUP_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss").withZone(ZoneId.systemDefault());
   private static final Map<String, PlayerProgressManager.PlayerProgress> cache = new HashMap<>();
   private static final Map<String, Boolean> dirty = new HashMap<>();
   private static Path dataRoot;
   private static Path playersRoot;
   private static Path backupsRoot;
   private static int autosaveTicks;
   private static int backupTicks;

   public static String[] getStandardClasses() {
      return (String[])BASE_CLASSES.clone();
   }

   public static String[] getInitialBaseClasses() {
      return (String[])INITIAL_BASE_CLASSES.clone();
   }

   public static String[] getUnlockableBaseClasses() {
      return Arrays.stream(BASE_CLASSES).filter(PlayerProgressManager::isUnlockableBaseClass).toArray(String[]::new);
   }

   public static String[] getShopClasses() {
      return (String[])SHOP_CLASSES.clone();
   }

   public static String[] getAllClasses() {
      return (String[])ALL_CLASSES.clone();
   }

   public static String[] getExclusiveClasses() {
      return (String[])EXCLUSIVE_CLASSES.clone();
   }

   public static int getShopPrice(String clazz) {
      return SHOP_CLASS_PRICES.getOrDefault(normalizeClass(clazz), 0);
   }

   public static int getShopFixedLevel(String clazz) {
      return SHOP_CLASS_LEVELS.getOrDefault(normalizeClass(clazz), 0);
   }

   public static boolean isShopClass(String clazz) {
      return SHOP_CLASS_PRICES.containsKey(normalizeClass(clazz));
   }

   public static boolean isExclusiveClass(String clazz) {
      return containsClass(EXCLUSIVE_CLASSES, clazz);
   }

   public static boolean isInitialBaseClass(String clazz) {
      return containsClass(INITIAL_BASE_CLASSES, clazz);
   }

   public static boolean isUnlockableBaseClass(String clazz) {
      String normalizedClass = normalizeClass(clazz);
      return isBaseProgressionClass(normalizedClass) && !isInitialBaseClass(normalizedClass);
   }

   public static boolean isBaseProgressionClass(String clazz) {
      return containsClass(BASE_CLASSES, clazz);
   }

   public static int getBaseUnlockCost(String clazz) {
      return isUnlockableBaseClass(clazz) ? 25 : 0;
   }

   public static int getUpgradeCost(int targetTier) {
      if (targetTier == 1) {
         return 20;
      } else {
         return targetTier == 2 ? 50 : 0;
      }
   }

   public static int getXpCapForTier(int tier) {
      if (tier >= 2) {
         return 800;
      } else {
         return tier == 1 ? 800 : 300;
      }
   }

   public static synchronized void loadPlayer(ServerPlayer player) {
      if (player != null) {
         init(player.f_8924_);
         String key = getPlayerKey(player);
         PlayerProgressManager.PlayerProgress progress = cache.get(key);
         boolean fileExists = Files.exists(getPlayerFile(key));
         if (progress == null) {
            progress = readOrCreateProgress(player, key);
            cache.put(key, progress);
         }

         int oldVersion = progress.dataVersion;
         boolean changed = updateIdentity(progress, player);
         normalize(progress);
         if (oldVersion < 8) {
            changed = true;
         }

         if (changed || !fileExists) {
            markDirty(key);
            savePlayer(player);
         }
      }
   }

   public static synchronized void savePlayer(ServerPlayer player) {
      if (player != null) {
         init(player.f_8924_);
         String key = getPlayerKey(player);
         PlayerProgressManager.PlayerProgress progress = getOrLoad(player, key);
         updateIdentity(progress, player);
         progress.lastSeen = Instant.now().toEpochMilli();
         normalize(progress);

         try {
            writeJsonAtomically(getPlayerFile(key), progress);
            dirty.remove(key);
         } catch (IOException exception) {
            TacticalTabletMod.LOGGER.error("Failed to save Tactical Tablet progress for {}", key, exception);
         }
      }
   }

   public static synchronized void saveAll() {
      if (playersRoot != null) {
         for (Entry<String, PlayerProgressManager.PlayerProgress> entry : cache.entrySet()) {
            String key = entry.getKey();
            PlayerProgressManager.PlayerProgress progress = entry.getValue();
            progress.lastSeen = Instant.now().toEpochMilli();
            normalize(progress);

            try {
               writeJsonAtomically(getPlayerFile(key), progress);
               dirty.remove(key);
            } catch (IOException exception) {
               TacticalTabletMod.LOGGER.error("Failed to save Tactical Tablet progress for {}", key, exception);
            }
         }
      }
   }

   public static synchronized void resetStorage() {
      saveAll();
      cache.clear();
      dirty.clear();
      dataRoot = null;
      playersRoot = null;
      backupsRoot = null;
      autosaveTicks = 0;
      backupTicks = 0;
   }

   public static synchronized int getXP(ServerPlayer player, String clazz) {
      if (player != null && clazz != null) {
         PlayerProgressManager.PlayerProgress progress = getOrLoad(player, getPlayerKey(player));
         return progress.classes.getOrDefault(normalizeClass(clazz), 0);
      } else {
         return 0;
      }
   }

   public static synchronized int addXP(ServerPlayer player, String clazz, int amount) {
      if (player != null && clazz != null && !clazz.isBlank() && amount > 0) {
         String normalizedClass = normalizeClass(clazz);
         if (!canGainXp(player, normalizedClass)) {
            return 0;
         }

         String key = getPlayerKey(player);
         PlayerProgressManager.PlayerProgress progress = getOrLoad(player, key);
         int current = progress.classes.getOrDefault(normalizedClass, 0);
         int tier = getStoredTier(progress, normalizedClass);
         int capped = Math.min(getXpCapForTier(tier), safeAdd(current, amount));
         progress.classes.put(normalizedClass, capped);
         markDirty(key);
         return Math.max(0, capped - current);
      } else {
         return 0;
      }
   }

   public static synchronized boolean isXpBoostEnabled(ServerPlayer player) {
      if (player == null) {
         return false;
      }

      PlayerProgressManager.PlayerProgress progress = getOrLoad(player, getPlayerKey(player));
      return progress.xpBoost;
   }

   public static synchronized void setXpBoostEnabled(ServerPlayer player, boolean enabled) {
      if (player != null) {
         String key = getPlayerKey(player);
         PlayerProgressManager.PlayerProgress progress = getOrLoad(player, key);
         if (progress.xpBoost != enabled) {
            progress.xpBoost = enabled;
            markDirty(key);
         }
      }
   }

   public static synchronized boolean isSadTromboneKillsEnabled(ServerPlayer player) {
      if (player == null) {
         return false;
      }

      PlayerProgressManager.PlayerProgress progress = getOrLoad(player, getPlayerKey(player));
      return progress.sadTromboneKills;
   }

   public static synchronized void setSadTromboneKillsEnabled(ServerPlayer player, boolean enabled) {
      if (player != null) {
         String key = getPlayerKey(player);
         PlayerProgressManager.PlayerProgress progress = getOrLoad(player, key);
         if (progress.sadTromboneKills != enabled) {
            progress.sadTromboneKills = enabled;
            markDirty(key);
         }
      }
   }

   public static synchronized void setXP(ServerPlayer player, String clazz, int amount) {
      if (player != null && clazz != null && !clazz.isBlank()) {
         String normalizedClass = normalizeClass(clazz);
         String key = getPlayerKey(player);
         PlayerProgressManager.PlayerProgress progress = getOrLoad(player, key);
         int value = isShopClass(normalizedClass) ? 0 : Math.max(0, amount);
         if (isBaseProgressionClass(normalizedClass)) {
            value = Math.min(value, 800);
         }

         progress.classes.put(normalizedClass, value);
         markDirty(key);
      }
   }

   public static synchronized int getLevel(ServerPlayer player, String clazz) {
      String normalizedClass = normalizeClass(clazz);
      if (MapSetManager.isCompetitiveSet()) {
         return isBaseProgressionClass(normalizedClass) ? 1 : 0;
      }

      if (isShopClass(normalizedClass)) {
         return isClassPurchased(player, normalizedClass) ? getShopFixedLevel(normalizedClass) : 0;
      }

      if (!isBaseClassUnlocked(player, normalizedClass)) {
         return 0;
      }

      PlayerProgressManager.PlayerProgress progress = getOrLoad(player, getPlayerKey(player));
      return getStoredTier(progress, normalizedClass);
   }

   public static int getLevelForXP(int xp) {
      if (xp >= 800) {
         return 2;
      } else {
         return xp >= 300 ? 1 : 0;
      }
   }

   public static synchronized boolean isBaseClassUnlocked(ServerPlayer player, String clazz) {
      if (player != null && clazz != null) {
         String normalizedClass = normalizeClass(clazz);
         if (!isBaseProgressionClass(normalizedClass)) {
            return false;
         }

         if (MapSetManager.isCompetitiveSet()) {
            return true;
         }

         PlayerProgressManager.PlayerProgress progress = getOrLoad(player, getPlayerKey(player));
         return isBaseClassUnlocked(progress, normalizedClass);
      } else {
         return false;
      }
   }

   public static synchronized boolean canGainXp(ServerPlayer player, String clazz) {
      if (player == null || clazz == null) {
         return false;
      }

      if (MapSetManager.isCompetitiveSet()) {
         return false;
      }

      String normalizedClass = normalizeClass(clazz);
      return isBaseProgressionClass(normalizedClass) && isBaseClassUnlocked(player, normalizedClass);
   }

   public static synchronized PlayerProgressManager.ProgressionResult unlockBaseClass(ServerPlayer player, String clazz) {
      if (player == null || clazz == null) {
         return PlayerProgressManager.ProgressionResult.INVALID_CLASS;
      }

      if (MapSetManager.isCompetitiveSet()) {
         return PlayerProgressManager.ProgressionResult.ALREADY_UNLOCKED;
      }

      String normalizedClass = normalizeClass(clazz);
      if (!isUnlockableBaseClass(normalizedClass)) {
         return PlayerProgressManager.ProgressionResult.INVALID_CLASS;
      }

      String key = getPlayerKey(player);
      PlayerProgressManager.PlayerProgress progress = getOrLoad(player, key);
      if (isBaseClassUnlocked(progress, normalizedClass)) {
         return PlayerProgressManager.ProgressionResult.ALREADY_UNLOCKED;
      }

      if (progress.coins < 25) {
         return PlayerProgressManager.ProgressionResult.NOT_ENOUGH_COINS;
      }

      progress.coins -= 25;
      progress.unlockedBaseClasses.put(normalizedClass, 1);
      progress.classes.putIfAbsent(normalizedClass, 0);
      progress.classTiers.putIfAbsent(normalizedClass, 0);
      markDirty(key);
      return PlayerProgressManager.ProgressionResult.SUCCESS;
   }

   public static synchronized PlayerProgressManager.ProgressionResult upgradeClassTier(ServerPlayer player, String clazz, int targetTier) {
      if (player == null || clazz == null) {
         return PlayerProgressManager.ProgressionResult.INVALID_CLASS;
      }

      if (MapSetManager.isCompetitiveSet()) {
         return PlayerProgressManager.ProgressionResult.WRONG_TIER;
      }

      String normalizedClass = normalizeClass(clazz);
      if (!isBaseProgressionClass(normalizedClass)) {
         return PlayerProgressManager.ProgressionResult.INVALID_CLASS;
      }

      if (targetTier != 1 && targetTier != 2) {
         return PlayerProgressManager.ProgressionResult.INVALID_CLASS;
      }

      String key = getPlayerKey(player);
      PlayerProgressManager.PlayerProgress progress = getOrLoad(player, key);
      if (!isBaseClassUnlocked(progress, normalizedClass)) {
         return PlayerProgressManager.ProgressionResult.LOCKED;
      }

      int currentTier = getStoredTier(progress, normalizedClass);
      if (currentTier >= 2) {
         return PlayerProgressManager.ProgressionResult.MAX_TIER;
      }

      if (targetTier != currentTier + 1) {
         return PlayerProgressManager.ProgressionResult.WRONG_TIER;
      }

      int requiredXp = targetTier == 1 ? 300 : 800;
      if (progress.classes.getOrDefault(normalizedClass, 0) < requiredXp) {
         return PlayerProgressManager.ProgressionResult.NOT_ENOUGH_XP;
      }

      int cost = getUpgradeCost(targetTier);
      if (progress.coins < cost) {
         return PlayerProgressManager.ProgressionResult.NOT_ENOUGH_COINS;
      }

      progress.coins -= cost;
      progress.classTiers.put(normalizedClass, targetTier);
      progress.classes.put(normalizedClass, Math.min(progress.classes.getOrDefault(normalizedClass, 0), getXpCapForTier(targetTier)));
      markDirty(key);
      return PlayerProgressManager.ProgressionResult.SUCCESS;
   }

   public static synchronized void addCoins(ServerPlayer player, int amount) {
      if (player != null && amount != 0) {
         String key = getPlayerKey(player);
         PlayerProgressManager.PlayerProgress progress = getOrLoad(player, key);
         progress.coins = Math.max(0, safeAdd(progress.coins, amount));
         markDirty(key);
      }
   }

   public static synchronized boolean addCoins(MinecraftServer server, UUID uuid, int amount) {
      if (server != null && uuid != null && amount != 0) {
         init(server);
         String key = uuid.toString().replace("-", "");
         PlayerProgressManager.PlayerProgress progress = cache.get(key);
         Path file = getPlayerFile(key);
         if (progress == null) {
            if (!Files.exists(file)) {
               return false;
            }

            try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
               progress = (PlayerProgressManager.PlayerProgress)GSON.fromJson(reader, PlayerProgressManager.PlayerProgress.class);
            } catch (JsonSyntaxException | IOException exception) {
               TacticalTabletMod.LOGGER.error("Failed to award offline player {}", uuid, exception);
               return false;
            }

            if (progress == null) {
               return false;
            }

            normalize(progress);
            cache.put(key, progress);
         }

         progress.coins = Math.max(0, safeAdd(progress.coins, amount));
         progress.lastSeen = Instant.now().toEpochMilli();

         try {
            writeJsonAtomically(file, progress);
            dirty.remove(key);
            return true;
         } catch (IOException exception) {
            TacticalTabletMod.LOGGER.error("Failed to save offline coin award for {}", uuid, exception);
            markDirty(key);
            return false;
         }
      } else {
         return false;
      }
   }

   public static synchronized int getCoins(ServerPlayer player) {
      if (player == null) {
         return 0;
      }

      PlayerProgressManager.PlayerProgress progress = getOrLoad(player, getPlayerKey(player));
      return progress.coins;
   }

   public static synchronized void setCoins(ServerPlayer player, int amount) {
      if (player != null) {
         String key = getPlayerKey(player);
         PlayerProgressManager.PlayerProgress progress = getOrLoad(player, key);
         progress.coins = Math.max(0, amount);
         markDirty(key);
      }
   }

   public static synchronized void addMatchPlayed(ServerPlayer player) {
      if (player != null) {
         String key = getPlayerKey(player);
         PlayerProgressManager.PlayerProgress progress = getOrLoad(player, key);
         progress.matchesPlayed = safeAdd(progress.matchesPlayed, 1);
         markDirty(key);
      }
   }

   public static synchronized int getMatchesPlayed(ServerPlayer player) {
      if (player == null) {
         return 0;
      }

      PlayerProgressManager.PlayerProgress progress = getOrLoad(player, getPlayerKey(player));
      return progress.matchesPlayed;
   }

   public static synchronized void addWin(ServerPlayer player) {
      if (player != null) {
         String key = getPlayerKey(player);
         PlayerProgressManager.PlayerProgress progress = getOrLoad(player, key);
         progress.wins = safeAdd(progress.wins, 1);
         markDirty(key);
      }
   }

   public static synchronized int getWins(ServerPlayer player) {
      if (player == null) {
         return 0;
      }

      PlayerProgressManager.PlayerProgress progress = getOrLoad(player, getPlayerKey(player));
      return progress.wins;
   }

   public static synchronized void addKill(ServerPlayer player) {
      if (player != null) {
         String key = getPlayerKey(player);
         PlayerProgressManager.PlayerProgress progress = getOrLoad(player, key);
         progress.kills = safeAdd(progress.kills, 1);
         markDirty(key);
      }
   }

   public static synchronized int getKills(ServerPlayer player) {
      if (player == null) {
         return 0;
      }

      PlayerProgressManager.PlayerProgress progress = getOrLoad(player, getPlayerKey(player));
      return progress.kills;
   }

   public static synchronized void addDeath(ServerPlayer player) {
      if (player != null) {
         String key = getPlayerKey(player);
         PlayerProgressManager.PlayerProgress progress = getOrLoad(player, key);
         progress.deaths = safeAdd(progress.deaths, 1);
         markDirty(key);
      }
   }

   public static synchronized int getDeaths(ServerPlayer player) {
      if (player == null) {
         return 0;
      }

      PlayerProgressManager.PlayerProgress progress = getOrLoad(player, getPlayerKey(player));
      return progress.deaths;
   }

   public static synchronized boolean isClassPurchased(ServerPlayer player, String clazz) {
      if (player == null || clazz == null) {
         return false;
      }

      if (MapSetManager.isCompetitiveSet()) {
         return false;
      }

      String normalizedClass = normalizeClass(clazz);
      if (!isShopClass(normalizedClass)) {
         return false;
      }

      PlayerProgressManager.PlayerProgress progress = getOrLoad(player, getPlayerKey(player));
      return progress.purchasedClasses.getOrDefault(normalizedClass, 0) > 0;
   }

   public static synchronized PlayerProgressManager.PurchaseResult purchaseClass(ServerPlayer player, String clazz) {
      if (player == null || clazz == null) {
         return PlayerProgressManager.PurchaseResult.NOT_PURCHASABLE;
      }

      if (MapSetManager.isCompetitiveSet()) {
         return PlayerProgressManager.PurchaseResult.NOT_PURCHASABLE;
      }

      String normalizedClass = normalizeClass(clazz);
      int price = getShopPrice(normalizedClass);
      if (price <= 0) {
         return PlayerProgressManager.PurchaseResult.NOT_PURCHASABLE;
      }

      String key = getPlayerKey(player);
      PlayerProgressManager.PlayerProgress progress = getOrLoad(player, key);
      if (progress.purchasedClasses.getOrDefault(normalizedClass, 0) > 0) {
         return PlayerProgressManager.PurchaseResult.ALREADY_OWNED;
      }

      if (progress.coins < price) {
         return PlayerProgressManager.PurchaseResult.NOT_ENOUGH_COINS;
      }

      progress.coins -= price;
      progress.purchasedClasses.put(normalizedClass, 1);
      progress.classes.put(normalizedClass, 0);
      markDirty(key);
      return PlayerProgressManager.PurchaseResult.PURCHASED;
   }

   public static synchronized boolean isExclusiveClassGranted(ServerPlayer player, String clazz) {
      if (player != null && clazz != null) {
         String normalizedClass = normalizeClass(clazz);
         if (!isExclusiveClass(normalizedClass)) {
            return false;
         }

         PlayerProgressManager.PlayerProgress progress = getOrLoad(player, getPlayerKey(player));
         return progress.purchasedClasses.getOrDefault(normalizedClass, 0) > 0;
      } else {
         return false;
      }
   }

   public static synchronized boolean grantExclusiveClass(ServerPlayer player, String clazz) {
      if (player != null && clazz != null) {
         String normalizedClass = normalizeClass(clazz);
         if (!isExclusiveClass(normalizedClass)) {
            return false;
         }

         String key = getPlayerKey(player);
         PlayerProgressManager.PlayerProgress progress = getOrLoad(player, key);
         if (progress.purchasedClasses.getOrDefault(normalizedClass, 0) > 0) {
            return false;
         }

         progress.purchasedClasses.put(normalizedClass, 1);
         progress.classes.putIfAbsent(normalizedClass, 0);
         markDirty(key);
         return true;
      } else {
         return false;
      }
   }

   public static synchronized Map<String, Integer> getPurchasedClasses(ServerPlayer player) {
      Map<String, Integer> result = new HashMap<>();
      if (player == null) {
         return result;
      }

      PlayerProgressManager.PlayerProgress progress = getOrLoad(player, getPlayerKey(player));

      for (String clazz : SHOP_CLASSES) {
         String normalizedClass = normalizeClass(clazz);
         result.put(normalizedClass, MapSetManager.isCompetitiveSet() ? 0 : (progress.purchasedClasses.getOrDefault(normalizedClass, 0) > 0 ? 1 : 0));
      }

      for (String clazz : EXCLUSIVE_CLASSES) {
         String normalizedClass = normalizeClass(clazz);
         result.put(normalizedClass, progress.purchasedClasses.getOrDefault(normalizedClass, 0) > 0 ? 1 : 0);
      }

      return result;
   }

   public static synchronized Map<String, Integer> getUnlockedBaseClasses(ServerPlayer player) {
      Map<String, Integer> result = new HashMap<>();
      if (player == null) {
         return result;
      }

      PlayerProgressManager.PlayerProgress progress = getOrLoad(player, getPlayerKey(player));

      for (String clazz : BASE_CLASSES) {
         String normalizedClass = normalizeClass(clazz);
         result.put(normalizedClass, !MapSetManager.isCompetitiveSet() && !isBaseClassUnlocked(progress, normalizedClass) ? 0 : 1);
      }

      return result;
   }

   public static synchronized Map<String, Integer> getClassTiers(ServerPlayer player) {
      Map<String, Integer> result = new HashMap<>();
      if (player == null) {
         return result;
      }

      PlayerProgressManager.PlayerProgress progress = getOrLoad(player, getPlayerKey(player));

      for (String clazz : BASE_CLASSES) {
         String normalizedClass = normalizeClass(clazz);
         result.put(normalizedClass, MapSetManager.isCompetitiveSet() ? 1 : getStoredTier(progress, normalizedClass));
      }

      for (String clazz : SHOP_CLASSES) {
         String normalizedClass = normalizeClass(clazz);
         result.put(
            normalizedClass, MapSetManager.isCompetitiveSet() ? 0 : (isClassPurchased(player, normalizedClass) ? getShopFixedLevel(normalizedClass) : 0)
         );
      }

      return result;
   }

   public static synchronized int getCareerProgressPercent(ServerPlayer player) {
      if (player == null) {
         return 0;
      }

      PlayerProgressManager.PlayerProgress progress = getOrLoad(player, getPlayerKey(player));
      double completed = 0.0;

      for (String clazz : BASE_CLASSES) {
         String normalizedClass = normalizeClass(clazz);
         if (isBaseClassUnlocked(progress, normalizedClass)) {
            int xp = progress.classes.getOrDefault(normalizeClass(clazz), 0);
            completed += Math.min(Math.max(xp, 0), 800) / 800.0;
         }
      }

      for (String clazz : SHOP_CLASSES) {
         if (progress.purchasedClasses.getOrDefault(normalizeClass(clazz), 0) > 0) {
            completed++;
         }
      }

      int totalGoals = BASE_CLASSES.length + SHOP_CLASSES.length;
      return totalGoals <= 0 ? 100 : Math.max(0, Math.min(100, (int)Math.round(completed * 100.0 / totalGoals)));
   }

   public static synchronized int getBattlePassXp(ServerPlayer player) {
      if (player == null) {
         return 0;
      }

      PlayerProgressManager.PlayerProgress progress = getOrLoad(player, getPlayerKey(player));
      return progress.battlePassXp;
   }

   public static synchronized void addBattlePassXp(ServerPlayer player, int amount) {
      if (player != null && amount > 0) {
         String key = getPlayerKey(player);
         PlayerProgressManager.PlayerProgress progress = getOrLoad(player, key);
         progress.battlePassXp = safeAdd(progress.battlePassXp, amount);
         markDirty(key);
      }
   }

   public static synchronized void tick(MinecraftServer server) {
      if (server != null) {
         init(server);
         autosaveTicks++;
         backupTicks++;
         if (autosaveTicks >= 1200) {
            autosaveTicks = 0;
            saveDirty();
         }

         if (backupTicks >= 36000) {
            backupTicks = 0;
            backupNow();
         }
      }
   }

   public static synchronized void unloadPlayer(ServerPlayer player) {
      if (player != null) {
         String key = getPlayerKey(player);
         cache.remove(key);
         dirty.remove(key);
      }
   }

   public static synchronized Map<String, Integer> getAllClassXP(ServerPlayer player) {
      Map<String, Integer> result = new HashMap<>();
      if (player == null) {
         return result;
      }

      PlayerProgressManager.PlayerProgress progress = getOrLoad(player, getPlayerKey(player));

      for (String clazz : ALL_CLASSES) {
         String normalizedClass = normalizeClass(clazz);
         int xp = isShopClass(normalizedClass) ? 0 : (MapSetManager.isCompetitiveSet() ? 300 : progress.classes.getOrDefault(normalizedClass, 0));
         result.put(normalizedClass, xp);
      }

      return result;
   }

   public static synchronized Map<String, Integer> getAllClassLevels(ServerPlayer player) {
      Map<String, Integer> result = new HashMap<>();
      if (player == null) {
         return result;
      }

      PlayerProgressManager.PlayerProgress progress = getOrLoad(player, getPlayerKey(player));

      for (String clazz : ALL_CLASSES) {
         String normalizedClass = normalizeClass(clazz);
         int level = MapSetManager.isCompetitiveSet()
            ? (isBaseProgressionClass(normalizedClass) ? 1 : 0)
            : (
               isShopClass(normalizedClass)
                  ? (progress.purchasedClasses.getOrDefault(normalizedClass, 0) > 0 ? getShopFixedLevel(normalizedClass) : 0)
                  : getStoredTier(progress, normalizedClass)
            );
         result.put(normalizedClass, level);
      }

      return result;
   }

   public static synchronized void backupNow() {
      if (playersRoot != null && backupsRoot != null) {
         saveAll();
         String timestamp = BACKUP_FORMAT.format(Instant.now());
         Path backupRoot = backupsRoot.resolve(timestamp);
         Path backupPlayersRoot = backupRoot.resolve("players");

         try {
            Files.createDirectories(backupPlayersRoot);
            copyJsonFiles(playersRoot, backupPlayersRoot);
            Path seasonFile = dataRoot.resolve("season.json");
            if (Files.exists(seasonFile)) {
               Files.copy(seasonFile, backupRoot.resolve("season.json"), StandardCopyOption.REPLACE_EXISTING);
            }

            cleanOldBackups();
         } catch (IOException exception) {
            TacticalTabletMod.LOGGER.error("Failed to create Tactical Tablet progress backup", exception);
         }
      }
   }

   public static synchronized Path getDataRoot() {
      return dataRoot;
   }

   private static PlayerProgressManager.PlayerProgress getOrLoad(ServerPlayer player, String key) {
      init(player.f_8924_);
      PlayerProgressManager.PlayerProgress cached = cache.get(key);
      if (cached != null) {
         updateIdentity(cached, player);
         return cached;
      } else {
         PlayerProgressManager.PlayerProgress progress = readOrCreateProgress(player, key);
         updateIdentity(progress, player);
         normalize(progress);
         cache.put(key, progress);
         return progress;
      }
   }

   private static PlayerProgressManager.PlayerProgress readOrCreateProgress(ServerPlayer player, String key) {
      Path file = getPlayerFile(key);
      if (Files.exists(file)) {
         try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            PlayerProgressManager.PlayerProgress progress = (PlayerProgressManager.PlayerProgress)GSON.fromJson(
               reader, PlayerProgressManager.PlayerProgress.class
            );
            return progress == null ? createProgress(player) : progress;
         } catch (JsonSyntaxException | IOException exception) {
            TacticalTabletMod.LOGGER.error("Failed to read Tactical Tablet progress file {}", file, exception);
            backupCorruptFile(file);
         }
      }

      Path legacyFile = getLegacyPlayerFile(player);
      if (legacyFile != null && !legacyFile.equals(file) && Files.exists(legacyFile)) {
         try (Reader reader = Files.newBufferedReader(legacyFile, StandardCharsets.UTF_8)) {
            PlayerProgressManager.PlayerProgress progress = (PlayerProgressManager.PlayerProgress)GSON.fromJson(
               reader, PlayerProgressManager.PlayerProgress.class
            );
            TacticalTabletMod.LOGGER
               .info(
                  "Migrating Tactical Tablet progress for {} from name key {} to UUID key {}",
                  new Object[]{player.m_36316_().getName(), legacyFile.getFileName(), file.getFileName()}
               );
            return progress == null ? createProgress(player) : progress;
         } catch (JsonSyntaxException | IOException exception) {
            TacticalTabletMod.LOGGER.error("Failed to read legacy Tactical Tablet progress file {}", legacyFile, exception);
            backupCorruptFile(legacyFile);
         }
      }

      return createProgress(player);
   }

   private static PlayerProgressManager.PlayerProgress createProgress(ServerPlayer player) {
      PlayerProgressManager.PlayerProgress progress = new PlayerProgressManager.PlayerProgress();
      progress.name = player.m_36316_().getName();
      progress.uuid = compactUuid(player);
      progress.firstSeen = Instant.now().toEpochMilli();
      progress.lastSeen = progress.firstSeen;
      normalize(progress);
      return progress;
   }

   private static boolean updateIdentity(PlayerProgressManager.PlayerProgress progress, ServerPlayer player) {
      boolean changed = false;
      String name = player.m_36316_().getName();
      String uuid = compactUuid(player);
      if (!Objects.equals(progress.name, name)) {
         progress.name = name;
         changed = true;
      }

      if (!Objects.equals(progress.uuid, uuid)) {
         progress.uuid = uuid;
         changed = true;
      }

      if (progress.firstSeen <= 0L) {
         progress.firstSeen = Instant.now().toEpochMilli();
         changed = true;
      }

      progress.lastSeen = Instant.now().toEpochMilli();
      return changed;
   }

   private static void init(MinecraftServer server) {
      if (server != null && dataRoot == null) {
         Path worldRoot = server.m_129843_(LevelResource.f_78182_).toAbsolutePath().normalize();
         Path serverRoot = worldRoot.getParent();
         if (serverRoot == null) {
            serverRoot = worldRoot;
         }

         dataRoot = serverRoot.resolve("tacticaltablet_data");
         playersRoot = dataRoot.resolve("players");
         backupsRoot = dataRoot.resolve("backups");

         try {
            Files.createDirectories(playersRoot);
            Files.createDirectories(backupsRoot);
            ensureSeasonFile();
         } catch (IOException exception) {
            TacticalTabletMod.LOGGER.error("Failed to initialize Tactical Tablet progress storage", exception);
         }
      }
   }

   private static void ensureSeasonFile() throws IOException {
      Path seasonFile = dataRoot.resolve("season.json");
      if (!Files.exists(seasonFile)) {
         Map<String, Object> season = new HashMap<>();
         season.put("dataVersion", 8);
         season.put("season", 1);
         season.put("createdAt", Instant.now().toString());
         writeJsonAtomically(seasonFile, season);
      }
   }

   private static void saveDirty() {
      if (!dirty.isEmpty()) {
         for (String key : dirty.keySet().toArray(new String[0])) {
            PlayerProgressManager.PlayerProgress progress = cache.get(key);
            if (progress == null) {
               dirty.remove(key);
            } else {
               progress.lastSeen = Instant.now().toEpochMilli();
               normalize(progress);

               try {
                  writeJsonAtomically(getPlayerFile(key), progress);
                  dirty.remove(key);
               } catch (IOException exception) {
                  TacticalTabletMod.LOGGER.error("Failed to autosave Tactical Tablet progress for {}", key, exception);
               }
            }
         }
      }
   }

   private static void markDirty(String key) {
      dirty.put(key, Boolean.TRUE);
   }

   private static void normalize(PlayerProgressManager.PlayerProgress progress) {
      int oldVersion = progress.dataVersion;
      progress.dataVersion = 8;
      if (progress.name == null) {
         progress.name = "";
      }

      if (progress.uuid == null) {
         progress.uuid = "";
      }

      progress.wins = Math.max(0, progress.wins);
      progress.kills = Math.max(0, progress.kills);
      progress.deaths = Math.max(0, progress.deaths);
      progress.matchesPlayed = Math.max(0, progress.matchesPlayed);
      progress.coins = Math.max(0, progress.coins);
      progress.battlePassXp = Math.max(0, progress.battlePassXp);
      progress.classes = normalizeIntegerMap(progress.classes);
      progress.classTiers = normalizeIntegerMap(progress.classTiers);
      progress.unlockedBaseClasses = normalizeIntegerMap(progress.unlockedBaseClasses);
      progress.purchasedClasses = normalizeIntegerMap(progress.purchasedClasses);
      progress.donations = normalizeIntegerMap(progress.donations);
      progress.stats = normalizeIntegerMap(progress.stats);

      for (String clazz : INITIAL_BASE_CLASSES) {
         progress.unlockedBaseClasses.put(normalizeClass(clazz), 1);
      }

      if (oldVersion < 5) {
         migrateLegacyBaseProgress(progress);
      }

      for (String clazz : ALL_CLASSES) {
         String normalizedClass = normalizeClass(clazz);
         progress.classes.putIfAbsent(normalizedClass, 0);
         if (isShopClass(normalizedClass)) {
            progress.classes.put(normalizedClass, 0);
         }
      }

      for (String clazz : BASE_CLASSES) {
         String normalizedClass = normalizeClass(clazz);
         progress.classes.put(normalizedClass, Math.min(Math.max(0, progress.classes.getOrDefault(normalizedClass, 0)), 800));
         progress.classTiers.put(normalizedClass, clampTier(progress.classTiers.getOrDefault(normalizedClass, 0)));
         progress.unlockedBaseClasses.put(normalizedClass, isBaseClassUnlocked(progress, normalizedClass) ? 1 : 0);
      }

      for (String clazz : SHOP_CLASSES) {
         progress.purchasedClasses.putIfAbsent(normalizeClass(clazz), 0);
      }

      for (String clazz : EXCLUSIVE_CLASSES) {
         progress.purchasedClasses.putIfAbsent(normalizeClass(clazz), 0);
      }
   }

   private static void migrateLegacyBaseProgress(PlayerProgressManager.PlayerProgress progress) {
      for (String clazz : BASE_CLASSES) {
         String normalizedClass = normalizeClass(clazz);
         int xp = Math.min(Math.max(0, progress.classes.getOrDefault(normalizedClass, 0)), 800);
         if (xp > 0 || isInitialBaseClass(normalizedClass)) {
            progress.unlockedBaseClasses.put(normalizedClass, 1);
         }

         if (!progress.classTiers.containsKey(normalizedClass)) {
            progress.classTiers.put(normalizedClass, getLevelForXP(xp));
         }
      }
   }

   private static boolean isBaseClassUnlocked(PlayerProgressManager.PlayerProgress progress, String clazz) {
      if (progress == null) {
         return false;
      }

      String normalizedClass = normalizeClass(clazz);
      return isInitialBaseClass(normalizedClass) || progress.unlockedBaseClasses.getOrDefault(normalizedClass, 0) > 0;
   }

   private static int getStoredTier(PlayerProgressManager.PlayerProgress progress, String clazz) {
      return progress == null ? 0 : clampTier(progress.classTiers.getOrDefault(normalizeClass(clazz), 0));
   }

   private static int clampTier(int tier) {
      return Math.max(0, Math.min(2, tier));
   }

   private static boolean containsClass(String[] classes, String clazz) {
      String normalizedClass = normalizeClass(clazz);

      for (String candidate : classes) {
         if (normalizeClass(candidate).equals(normalizedClass)) {
            return true;
         }
      }

      return false;
   }

   private static Map<String, Integer> normalizeIntegerMap(Map<String, Integer> input) {
      Map<String, Integer> result = new HashMap<>();
      if (input == null) {
         return result;
      }

      for (Entry<String, Integer> entry : input.entrySet()) {
         String key = normalizeClass(entry.getKey());
         if (!key.isBlank()) {
            result.put(key, Math.max(0, entry.getValue() == null ? 0 : entry.getValue()));
         }
      }

      return result;
   }

   private static String normalizeClass(String clazz) {
      return clazz == null ? "" : clazz.trim().toLowerCase(Locale.ROOT);
   }

   private static String getPlayerKey(ServerPlayer player) {
      return compactUuid(player);
   }

   private static Path getLegacyPlayerFile(ServerPlayer player) {
      String legacyKey = getLegacyPlayerKey(player);
      return legacyKey.isBlank() ? null : getPlayerFile(legacyKey);
   }

   private static String getLegacyPlayerKey(ServerPlayer player) {
      String name = player.m_36316_().getName();
      String normalized = name == null ? "" : name.trim().toLowerCase(Locale.ROOT);
      normalized = normalized.replaceAll("[^a-z0-9_.-]", "_");
      if (normalized.isBlank()) {
         normalized = compactUuid(player);
      }

      return normalized;
   }

   private static String compactUuid(ServerPlayer player) {
      return player.m_20148_().toString().replace("-", "");
   }

   private static Path getPlayerFile(String key) {
      return playersRoot.resolve(key + ".json");
   }

   private static void writeJsonAtomically(Path file, Object value) throws IOException {
      Files.createDirectories(file.getParent());
      Path tempFile = file.resolveSibling(file.getFileName().toString() + ".tmp");

      try (Writer writer = Files.newBufferedWriter(tempFile, StandardCharsets.UTF_8)) {
         GSON.toJson(value, writer);
      }

      try {
         Files.move(tempFile, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
      } catch (AtomicMoveNotSupportedException exception) {
         Files.move(tempFile, file, StandardCopyOption.REPLACE_EXISTING);
      }
   }

   private static void copyJsonFiles(Path sourceRoot, Path targetRoot) throws IOException {
      if (Files.isDirectory(sourceRoot)) {
         try (DirectoryStream<Path> stream = Files.newDirectoryStream(sourceRoot, "*.json")) {
            for (Path source : stream) {
               Files.copy(source, targetRoot.resolve(source.getFileName().toString()), StandardCopyOption.REPLACE_EXISTING);
            }
         }
      }
   }

   private static void backupCorruptFile(Path file) {
      if (file != null && Files.exists(file) && backupsRoot != null) {
         String timestamp = BACKUP_FORMAT.format(Instant.now());
         Path target = backupsRoot.resolve("corrupt_" + timestamp + "_" + file.getFileName());

         try {
            Files.copy(file, target, StandardCopyOption.REPLACE_EXISTING);
         } catch (IOException exception) {
            TacticalTabletMod.LOGGER.error("Failed to back up corrupt Tactical Tablet progress file {}", file, exception);
         }
      }
   }

   private static void cleanOldBackups() throws IOException {
      if (Files.isDirectory(backupsRoot)) {
         try (Stream<Path> stream = Files.list(backupsRoot)) {
            Path[] backups = stream.filter(x$0 -> Files.isDirectory(x$0))
               .sorted(Comparator.<Path, String>comparing(path -> path.getFileName().toString()).reversed())
               .toArray(Path[]::new);

            for (int i = 48; i < backups.length; i++) {
               deleteRecursively(backups[i]);
            }
         }
      }
   }

   private static void deleteRecursively(Path root) throws IOException {
      if (Files.exists(root)) {
         try (Stream<Path> stream = Files.walk(root)) {
            Path[] paths = stream.sorted(Comparator.reverseOrder()).toArray(Path[]::new);

            for (Path path : paths) {
               Files.deleteIfExists(path);
            }
         }
      }
   }

   private static int safeAdd(int current, int amount) {
      if (amount > 0 && current > Integer.MAX_VALUE - amount) {
         return Integer.MAX_VALUE;
      } else {
         return amount < 0 && current < Integer.MIN_VALUE - amount ? Integer.MIN_VALUE : current + amount;
      }
   }

   private static final class PlayerProgress {
      private int dataVersion = 8;
      private String name = "";
      private String uuid = "";
      private Map<String, Integer> classes = new HashMap<>();
      private Map<String, Integer> classTiers = new HashMap<>();
      private Map<String, Integer> unlockedBaseClasses = new HashMap<>();
      private int wins;
      private int kills;
      private int deaths;
      private int matchesPlayed;
      private int coins;
      private int battlePassXp;
      private boolean xpBoost;
      private boolean sadTromboneKills;
      private Map<String, Integer> purchasedClasses = new HashMap<>();
      private Map<String, Integer> donations = new HashMap<>();
      private Map<String, Integer> stats = new HashMap<>();
      private long firstSeen;
      private long lastSeen;
   }

   public enum ProgressionResult {
      SUCCESS,
      ALREADY_UNLOCKED,
      LOCKED,
      NOT_ENOUGH_COINS,
      NOT_ENOUGH_XP,
      INVALID_CLASS,
      MAX_TIER,
      WRONG_TIER;
   }

   public enum PurchaseResult {
      PURCHASED,
      ALREADY_OWNED,
      NOT_ENOUGH_COINS,
      NOT_PURCHASABLE;
   }
}
