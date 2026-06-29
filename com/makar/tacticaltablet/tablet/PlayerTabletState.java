package com.makar.tacticaltablet.tablet;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;

public class PlayerTabletState {
   private static final Map<UUID, PlayerTabletState.State> states = new HashMap<>();

   public static void setSelectedClass(ServerPlayer player, String clazz) {
      if (player != null) {
         getState(player).selectedClass = clazz == null ? "" : clazz;
      }
   }

   public static String getSelectedClass(ServerPlayer player) {
      if (player == null) {
         return "";
      }

      PlayerTabletState.State state = states.get(player.m_20148_());
      return state == null ? "" : state.selectedClass;
   }

   public static boolean hasSelectedClass(ServerPlayer player) {
      return !getSelectedClass(player).isBlank();
   }

   public static boolean isKitUsed(ServerPlayer player) {
      if (player == null) {
         return false;
      }

      PlayerTabletState.State state = states.get(player.m_20148_());
      return state != null && state.kitUsed;
   }

   public static boolean isRtpUsed(ServerPlayer player) {
      if (player == null) {
         return false;
      }

      PlayerTabletState.State state = states.get(player.m_20148_());
      return state != null && state.rtpUsed;
   }

   public static void setKitUsed(ServerPlayer player) {
      if (player != null) {
         getState(player).kitUsed = true;
      }
   }

   public static void setRtpUsed(ServerPlayer player) {
      if (player != null) {
         getState(player).rtpUsed = true;
      }
   }

   public static void reset(ServerPlayer player) {
      if (player != null) {
         states.remove(player.m_20148_());
      }
   }

   public static void resetAll() {
      states.clear();
   }

   private static PlayerTabletState.State getState(ServerPlayer player) {
      return states.computeIfAbsent(player.m_20148_(), uuid -> new PlayerTabletState.State());
   }

   private static final class State {
      private boolean kitUsed;
      private boolean rtpUsed;
      private String selectedClass = "";
   }
}
