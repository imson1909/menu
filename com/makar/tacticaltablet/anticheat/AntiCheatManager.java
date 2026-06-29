package com.makar.tacticaltablet.anticheat;

import com.makar.tacticaltablet.core.TacticalTabletMod;
import com.makar.tacticaltablet.game.GameStateManager;
import com.makar.tacticaltablet.game.lives.LivesManager;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.server.level.ServerPlayer;

public final class AntiCheatManager {
   private static final long ALERT_THROTTLE_MS = 5000L;
   private static final String TAG_IN_LOBBY = "in_lobby";
   private static final String TAG_WAR_PLAYING = "war.playing";
   private static final Map<UUID, Map<ViolationType, Integer>> violations = new ConcurrentHashMap<>();
   private static final Map<UUID, Map<ViolationType, Long>> lastAlertTimes = new ConcurrentHashMap<>();

   private AntiCheatManager() {
   }

   public static void record(ServerPlayer player, ViolationType type, Severity severity, String details) {
      if (player != null && type != null && severity != null) {
         UUID uuid = player.m_20148_();
         int count = violations.computeIfAbsent(uuid, key -> new ConcurrentHashMap<>()).merge(type, 1, Integer::sum);
         String phase = getPhase(player);
         String safeDetails = sanitize(details);
         String logLine = "[AC] player="
            + player.m_36316_().getName()
            + " type="
            + type
            + " severity="
            + severity
            + " phase="
            + phase
            + " details="
            + safeDetails;
         if (!shouldAlert(type, severity, count)) {
            if (severity == Severity.LOW) {
               TacticalTabletMod.LOGGER.debug(logLine);
            } else {
               TacticalTabletMod.LOGGER.info(logLine);
            }
         } else if (shouldNotify(uuid, type)) {
            TacticalTabletMod.LOGGER.warn(logLine);
         }
      }
   }

   public static int getViolationCount(ServerPlayer player, ViolationType type) {
      if (player != null && type != null) {
         Map<ViolationType, Integer> playerViolations = violations.get(player.m_20148_());
         return playerViolations == null ? 0 : playerViolations.getOrDefault(type, 0);
      } else {
         return 0;
      }
   }

   public static void reset(ServerPlayer player) {
      if (player != null) {
         UUID uuid = player.m_20148_();
         violations.remove(uuid);
         lastAlertTimes.remove(uuid);
      }
   }

   public static void resetAll() {
      violations.clear();
      lastAlertTimes.clear();
   }

   public static String getPhase(ServerPlayer player) {
      if (player == null) {
         return "неизвестно";
      } else if (LivesManager.isEliminated(player)) {
         return "выбыл";
      } else if (GameStateManager.isInLobby(player) || player.m_19880_().contains("in_lobby")) {
         return "лобби";
      } else if (player.m_19880_().contains("war.playing")) {
         return "бой";
      } else {
         return !GameStateManager.isRunning(player.f_8924_) ? "ожидание" : "неизвестно";
      }
   }

   private static boolean shouldAlert(ViolationType type, Severity severity, int count) {
      return switch (type) {
         case ILLEGAL_PICKUP, ILLEGAL_CONTAINER -> false;
         case INVALID_RTP -> severity == Severity.HIGH || severity == Severity.CRITICAL;
         case ILLEGAL_INVENTORY -> severity == Severity.MEDIUM || severity == Severity.HIGH || severity == Severity.CRITICAL || count >= 3;
         case INVALID_TABLET_PACKET, PACKET_SPAM, MOVEMENT_ANOMALY, COMBAT_REACH -> true;
      };
   }

   private static boolean shouldNotify(UUID uuid, ViolationType type) {
      long now = System.currentTimeMillis();
      Map<ViolationType, Long> playerAlerts = lastAlertTimes.computeIfAbsent(uuid, key -> new ConcurrentHashMap<>());
      long last = playerAlerts.getOrDefault(type, 0L);
      if (now - last < 5000L) {
         return false;
      }

      playerAlerts.put(type, now);
      return true;
   }

   private static String sanitize(String details) {
      if (details != null && !details.isBlank()) {
         StringBuilder safe = new StringBuilder(details.length());

         for (int index = 0; index < details.length(); index++) {
            char character = details.charAt(index);
            if (character == ',') {
               safe.append(';');
            } else if (Character.isISOControl(character)) {
               safe.append("\\u").append(String.format(Locale.ROOT, "%04x", Integer.valueOf(character)));
            } else {
               safe.append(character);
            }
         }

         return safe.toString();
      } else {
         return "нет";
      }
   }
}
