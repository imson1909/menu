package com.makar.tacticaltablet.progression;

import com.makar.tacticaltablet.tablet.PlayerTabletState;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.Map.Entry;
import net.minecraft.server.level.ServerPlayer;

public class ClassCooldownManager {
   private static final int SNIPER_CLASS_ID = 1;
   private static final long[] COOLDOWNS = new long[]{
      minutes(2),
      minutes(2),
      minutes(4),
      minutes(15),
      minutes(10),
      minutes(6),
      minutes(15),
      0L,
      minutes(10),
      minutes(13),
      minutes(15),
      minutes(15),
      minutes(15),
      minutes(5),
      minutes(15),
      minutes(10),
      minutes(10),
      minutes(20),
      minutes(10),
      minutes(10)
   };
   private static final Map<UUID, Map<Integer, Long>> data = new HashMap<>();

   private static long getCooldownTime(int classId) {
      return classId >= 0 && classId < COOLDOWNS.length ? COOLDOWNS[classId] : 0L;
   }

   private static long getCooldownTime(ServerPlayer player, int classId) {
      if (classId != 1) {
         return getCooldownTime(classId);
      } else {
         int tier = PlayerProgressManager.getLevel(player, "sniper");
         if (tier >= 2) {
            return minutes(15);
         } else {
            return tier >= 1 ? minutes(10) : minutes(2);
         }
      }
   }

   private static long minutes(int minutes) {
      return minutes * 60L * 1000L;
   }

   public static void setCooldownForSelectedClass(ServerPlayer player) {
      if (player != null && PlayerTabletState.isKitUsed(player)) {
         int classId = classIdForClass(PlayerTabletState.getSelectedClass(player));
         if (classId >= 0) {
            setCooldown(player, classId);
         }
      }
   }

   private static int classIdForClass(String clazz) {
      if (clazz != null && !clazz.isBlank()) {
         return switch (clazz) {
            case "stormtrooper" -> 0;
            case "sniper" -> 1;
            case "scout" -> 2;
            case "droneoperator" -> 3;
            case "boomguy" -> 4;
            case "mortarman" -> 5;
            case "dream" -> 6;
            case "machinegunner" -> 8;
            case "rpgtrooper" -> 9;
            case "tagilla" -> 10;
            case "blackops" -> 11;
            case "cowboy" -> 12;
            case "solider" -> 13;
            case "rebel" -> 14;
            case "saboteur" -> 15;
            case "killer" -> 16;
            case "miniboss" -> 17;
            case "shahed" -> 18;
            case "krot" -> 19;
            default -> -1;
         };
      } else {
         return -1;
      }
   }

   public static void setCooldown(ServerPlayer player, int classId) {
      if (player != null && classId >= 0 && classId < COOLDOWNS.length) {
         long cooldownTime = getCooldownTime(player, classId);
         if (cooldownTime > 0L) {
            data.computeIfAbsent(player.m_20148_(), key -> new HashMap<>()).put(classId, System.currentTimeMillis() + cooldownTime);
         }
      }
   }

   public static long getRemaining(ServerPlayer player, int classId) {
      if (player != null && classId >= 0 && classId < COOLDOWNS.length) {
         Map<Integer, Long> map = data.get(player.m_20148_());
         if (map == null) {
            return 0L;
         }

         Long endTime = map.get(classId);
         if (endTime == null) {
            return 0L;
         }

         long left = endTime - System.currentTimeMillis();
         if (left <= 0L) {
            map.remove(classId);
            if (map.isEmpty()) {
               data.remove(player.m_20148_());
            }

            return 0L;
         } else {
            return left;
         }
      } else {
         return 0L;
      }
   }

   public static boolean isOnCooldown(ServerPlayer player, int classId) {
      return getRemaining(player, classId) > 0L;
   }

   public static Map<Integer, Long> getCooldowns(ServerPlayer player) {
      Map<Integer, Long> result = new HashMap<>();
      if (player == null) {
         return result;
      }

      Map<Integer, Long> map = data.get(player.m_20148_());
      if (map != null && !map.isEmpty()) {
         long now = System.currentTimeMillis();
         Iterator<Entry<Integer, Long>> iterator = map.entrySet().iterator();

         while (iterator.hasNext()) {
            Entry<Integer, Long> entry = iterator.next();
            int classId = entry.getKey();
            long left = entry.getValue() - now;
            if (left <= 0L) {
               iterator.remove();
            } else {
               result.put(classId, left);
            }
         }

         if (map.isEmpty()) {
            data.remove(player.m_20148_());
         }

         return result;
      } else {
         return result;
      }
   }

   public static void reset(ServerPlayer player) {
      if (player != null) {
         data.remove(player.m_20148_());
      }
   }

   public static void resetAll() {
      data.clear();
   }
}
