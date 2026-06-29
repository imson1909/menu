package com.makar.tacticaltablet.game.clanwar;

import com.makar.tacticaltablet.clan.ClanManager;
import com.makar.tacticaltablet.tablet.PlayerTabletState;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;

public final class ClanWarManager {
   public static final int PRE_START_WAIT_SECONDS = 60;
   public static final int CLAN_LIVES_PER_GAME = 2;
   public static final String TAG_SPECTATING = "war.clan_spectating";
   public static final String TAG_REGROUP_PENDING = "war.clan_regroup_pending";
   private static final Map<String, Integer> clanLives = new HashMap<>();
   private static final Set<String> eliminatedClans = new HashSet<>();
   private static int preStartWaitLeft = -1;
   private static boolean soloDebug;

   private ClanWarManager() {
   }

   public static void resetRuntime() {
      clanLives.clear();
      eliminatedClans.clear();
      preStartWaitLeft = -1;
   }

   public static boolean tickPreStartWait(MinecraftServer server) {
      if (server == null) {
         return false;
      }

      if (preStartWaitLeft < 0) {
         preStartWaitLeft = 60;
         broadcast(server, "[WAR] Война кланов: ожидание игроков 60 секунд.");
      }

      if (preStartWaitLeft <= 0) {
         return false;
      }

      if (preStartWaitLeft == 60 || preStartWaitLeft <= 5 || preStartWaitLeft % 15 == 0) {
         broadcast(server, "[WAR] Война кланов начнет подготовку через " + preStartWaitLeft + " сек.");
      }

      preStartWaitLeft--;
      return true;
   }

   public static void skipPreStartWait() {
      preStartWaitLeft = 0;
   }

   public static void startMatch(MinecraftServer server) {
      resetRuntime();
      if (server != null) {
         for (ServerPlayer player : server.m_6846_().m_11314_()) {
            player.m_20137_("war.clan_spectating");
            player.m_20137_("war.clan_regroup_pending");
            String clanId = ClanManager.getClanIdForPlayer(player);
            if (clanId.isBlank()) {
               showNeedClan(player);
            } else {
               clanLives.putIfAbsent(clanId, 2);
            }
         }
      }
   }

   public static boolean isSoloDebugEnabled() {
      return soloDebug;
   }

   public static void setSoloDebugEnabled(boolean enabled) {
      soloDebug = enabled;
   }

   public static boolean hasClan(ServerPlayer player) {
      return player != null && !ClanManager.getClanIdForPlayer(player).isBlank();
   }

   public static int getClanLives(String clanId) {
      return clanLives.getOrDefault(clanId, 0);
   }

   public static boolean isClanEliminated(String clanId) {
      return clanId == null || clanId.isBlank() || eliminatedClans.contains(clanId) || getClanLives(clanId) <= 0;
   }

   public static void showNeedClan(ServerPlayer player) {
      if (player != null) {
         player.m_213846_(Component.m_237113_("[WAR] Нужно вступить в клан."));
         player.f_8906_.m_9829_(new ClientboundSetTitlesAnimationPacket(5, 50, 10));
         player.f_8906_.m_9829_(new ClientboundSetTitleTextPacket(Component.m_237113_("Нужно вступить в клан")));
         player.f_8906_.m_9829_(new ClientboundSetSubtitleTextPacket(Component.m_237113_("Война кланов доступна только участникам кланов")));
      }
   }

   public static boolean shouldKeepSpectating(ServerPlayer player) {
      return player != null && player.m_19880_().contains("war.clan_spectating");
   }

   public static boolean shouldMoveToLobbyAfterDeath(ServerPlayer player) {
      return player != null && player.m_19880_().contains("war.clan_regroup_pending");
   }

   public static boolean isClanWiped(MinecraftServer server, String clanId) {
      if (server != null && clanId != null && !clanId.isBlank()) {
         for (ServerPlayer player : server.m_6846_().m_11314_()) {
            if (clanId.equals(ClanManager.getClanIdForPlayer(player)) && player.m_19880_().contains("war.playing") && player.m_6084_() && !player.m_21224_()) {
               return false;
            }
         }

         return true;
      } else {
         return false;
      }
   }

   public static int consumeClanLife(MinecraftServer server, String clanId) {
      if (server != null && clanId != null && !clanId.isBlank()) {
         int remaining = Math.max(0, clanLives.getOrDefault(clanId, 2) - 1);
         clanLives.put(clanId, remaining);
         broadcast(server, "[WAR] Клан " + ClanManager.getClanNameById(server, clanId) + " потерял жизнь. Осталось: " + remaining + ".");
         if (remaining <= 0) {
            eliminatedClans.add(clanId);
         }

         return remaining;
      } else {
         return 0;
      }
   }

   public static int getAliveClanCount(MinecraftServer server) {
      if (server == null) {
         return 0;
      }

      Set<String> alive = new HashSet<>();

      for (ServerPlayer player : server.m_6846_().m_11314_()) {
         String clanId = ClanManager.getClanIdForPlayer(player);
         if (!clanId.isBlank() && !isClanEliminated(clanId)) {
            alive.add(clanId);
         }
      }

      return alive.size();
   }

   public static ServerPlayer findWinningClanRepresentative(MinecraftServer server) {
      if (server == null) {
         return null;
      }

      String winnerClan = "";
      ServerPlayer winner = null;

      for (ServerPlayer player : server.m_6846_().m_11314_()) {
         String clanId = ClanManager.getClanIdForPlayer(player);
         if (!clanId.isBlank() && !isClanEliminated(clanId)) {
            if (winnerClan.isBlank()) {
               winnerClan = clanId;
               winner = player;
            } else if (!winnerClan.equals(clanId)) {
               return null;
            }
         }
      }

      return winner;
   }

   public static void markClanForRegroup(MinecraftServer server, String clanId) {
      if (server != null && clanId != null && !clanId.isBlank()) {
         for (ServerPlayer player : server.m_6846_().m_11314_()) {
            if (clanId.equals(ClanManager.getClanIdForPlayer(player))) {
               player.m_20137_("war.clan_spectating");
               player.m_20049_("war.clan_regroup_pending");
            }
         }
      }
   }

   public static void preparePlayerForRegroup(ServerPlayer player) {
      if (player != null) {
         player.m_20137_("war.clan_spectating");
         player.m_20137_("war.clan_regroup_pending");
         player.m_20137_("war.playing");
         PlayerTabletState.reset(player);
      }
   }

   public static void keepSpectator(ServerPlayer player) {
      if (player != null) {
         player.m_143403_(GameType.SPECTATOR);
         player.m_20137_("war.playing");
         player.m_20049_("war.clan_spectating");
      }
   }

   private static void broadcast(MinecraftServer server, String message) {
      Component component = Component.m_237113_(message);

      for (ServerPlayer player : server.m_6846_().m_11314_()) {
         player.m_213846_(component);
      }
   }
}
