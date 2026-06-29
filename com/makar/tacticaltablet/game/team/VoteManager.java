package com.makar.tacticaltablet.game.team;

import com.makar.tacticaltablet.admin.TestModeManager;
import com.makar.tacticaltablet.game.MatchMode;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

public final class VoteManager {
   private static final int VOTING_SECONDS = 12;
   private static final Random RANDOM = new Random();
   private static final Map<UUID, MatchMode> votes = new HashMap<>();
   private static int secondsLeft = 0;
   private static boolean active = false;

   private VoteManager() {
   }

   public static void start() {
      votes.clear();
      secondsLeft = 12;
      active = true;
   }

   public static void reset() {
      votes.clear();
      secondsLeft = 0;
      active = false;
   }

   public static boolean isActive() {
      return active;
   }

   public static int getSecondsLeft() {
      return Math.max(0, secondsLeft);
   }

   public static void tickSecond() {
      if (active && secondsLeft > 0) {
         secondsLeft--;
      }
   }

   public static boolean isComplete() {
      return active && secondsLeft <= 0;
   }

   public static boolean vote(ServerPlayer player, MatchMode mode) {
      if (player != null && mode != null && active) {
         int online = player.f_8924_ == null ? 0 : player.f_8924_.m_6846_().m_11309_();
         if (!mode.isSelectableFor(online, TestModeManager.canBypassTeamModeMinimums())) {
            return false;
         }

         votes.put(player.m_20148_(), mode);
         return true;
      } else {
         return false;
      }
   }

   public static MatchMode getVote(ServerPlayer player) {
      return player == null ? null : votes.get(player.m_20148_());
   }

   public static Map<MatchMode, Integer> getVoteCounts() {
      Map<MatchMode, Integer> counts = new EnumMap<>(MatchMode.class);

      for (MatchMode mode : MatchMode.values()) {
         counts.put(mode, 0);
      }

      for (MatchMode mode : votes.values()) {
         counts.merge(mode, 1, Integer::sum);
      }

      return counts;
   }

   public static MatchMode resolve(MinecraftServer server) {
      active = false;
      Map<MatchMode, Integer> counts = getVoteCounts();
      int best = 0;
      List<MatchMode> leaders = new ArrayList<>();
      int online = server == null ? 0 : server.m_6846_().m_11309_();

      for (MatchMode mode : MatchMode.selectableModes(online, TestModeManager.canBypassTeamModeMinimums())) {
         int count = counts.getOrDefault(mode, 0);
         if (count > best) {
            best = count;
            leaders.clear();
            leaders.add(mode);
         } else if (count == best && count > 0) {
            leaders.add(mode);
         }
      }

      MatchMode selected = leaders.isEmpty() ? MatchMode.SOLO : leaders.get(RANDOM.nextInt(leaders.size()));
      return sanitizeForOnlineCount(server, selected);
   }

   private static MatchMode sanitizeForOnlineCount(MinecraftServer server, MatchMode selected) {
      int online = server == null ? 0 : server.m_6846_().m_11309_();
      return MatchMode.sanitizeForOnlineCount(online, selected, TestModeManager.canBypassTeamModeMinimums());
   }

   public static int getVoteOptionsMask(MinecraftServer server) {
      int online = server == null ? 0 : server.m_6846_().m_11309_();
      return MatchMode.voteMaskFor(online, TestModeManager.canBypassTeamModeMinimums());
   }
}
