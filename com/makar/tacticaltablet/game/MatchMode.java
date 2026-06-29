package com.makar.tacticaltablet.game;

import java.util.ArrayList;
import java.util.List;

public enum MatchMode {
   SOLO("Соло", 1, 3, 2),
   DUO("Дуо", 2, 1, 4),
   TRIO("Трио", 3, 1, 5),
   SQUADS("Отряды", 5, 1, 13);

   public static final int MAX_DUO_PLAYERS = 8;
   private final String displayName;
   private final int teamSize;
   private final int livesPerPlayer;
   private final int minPlayers;

   MatchMode(String displayName, int teamSize, int livesPerPlayer, int minPlayers) {
      this.displayName = displayName;
      this.teamSize = teamSize;
      this.livesPerPlayer = livesPerPlayer;
      this.minPlayers = minPlayers;
   }

   public String displayName() {
      return this.displayName;
   }

   public int teamSize() {
      return this.teamSize;
   }

   public int livesPerPlayer() {
      return this.livesPerPlayer;
   }

   public int minPlayers() {
      return this.minPlayers;
   }

   public boolean isTeamMode() {
      return this.teamSize > 1;
   }

   public boolean isSelectableFor(int onlinePlayers, boolean includeDebugModes) {
      if (this == DUO && onlinePlayers > 8) {
         return false;
      } else if (includeDebugModes) {
         return true;
      } else if (this == SOLO) {
         return true;
      } else if (onlinePlayers >= SQUADS.minPlayers()) {
         return this == SQUADS;
      } else if (onlinePlayers < TRIO.minPlayers()) {
         return onlinePlayers >= DUO.minPlayers() ? this == DUO : false;
      } else {
         return this == TRIO || this == DUO && onlinePlayers <= 8;
      }
   }

   public static List<MatchMode> selectableModes(int onlinePlayers, boolean includeDebugModes) {
      List<MatchMode> result = new ArrayList<>();

      for (MatchMode mode : values()) {
         if (mode.isSelectableFor(onlinePlayers, includeDebugModes)) {
            result.add(mode);
         }
      }

      return List.copyOf(result);
   }

   public static int voteMaskFor(int onlinePlayers, boolean includeDebugModes) {
      int mask = 0;

      for (MatchMode mode : selectableModes(onlinePlayers, includeDebugModes)) {
         mask |= 1 << mode.ordinal();
      }

      return mask;
   }

   public static MatchMode sanitizeForOnlineCount(int onlinePlayers, MatchMode selected, boolean includeDebugModes) {
      if (selected == null) {
         return SOLO;
      }

      if (selected.isSelectableFor(onlinePlayers, includeDebugModes)) {
         return selected;
      }

      if (!includeDebugModes || selected == DUO && onlinePlayers > 8) {
         if (selected.isTeamMode()) {
            if (onlinePlayers >= SQUADS.minPlayers()) {
               return SQUADS;
            }

            if (onlinePlayers >= TRIO.minPlayers()) {
               return TRIO;
            }

            if (onlinePlayers >= DUO.minPlayers()) {
               return DUO;
            }
         }

         return SOLO;
      } else {
         return selected;
      }
   }

   public static MatchMode byId(int id) {
      MatchMode[] values = values();
      if (id >= 0 && id < values.length) {
         return values[id];
      } else {
         throw new IllegalArgumentException("Invalid match mode id: " + id);
      }
   }
}
