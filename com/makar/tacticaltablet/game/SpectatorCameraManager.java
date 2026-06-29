package com.makar.tacticaltablet.game;

import com.makar.tacticaltablet.game.lives.LivesManager;
import com.makar.tacticaltablet.game.team.TeamId;
import com.makar.tacticaltablet.game.team.TeamMatchManager;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

public final class SpectatorCameraManager {
   private static final int ENFORCE_INTERVAL_TICKS = 10;
   private static final Map<UUID, UUID> spectatorTargets = new ConcurrentHashMap<>();
   private static int tickCounter = 0;

   private SpectatorCameraManager() {
   }

   public static void onPlayerEliminated(ServerPlayer player) {
      if (player != null) {
         ServerPlayer target = findTargetFor(player);
         if (target == null) {
            spectatorTargets.remove(player.m_20148_());
         } else {
            spectatorTargets.put(player.m_20148_(), target.m_20148_());
            forceCamera(player, target);
         }
      }
   }

   public static void onServerTick(MinecraftServer server) {
      if (server != null) {
         if (++tickCounter >= 10) {
            tickCounter = 0;
            if (!isActiveMatch(server)) {
               clear(server);
            } else {
               for (ServerPlayer player : server.m_6846_().m_11314_()) {
                  if (!shouldLockSpectator(player)) {
                     spectatorTargets.remove(player.m_20148_());
                  } else {
                     ServerPlayer target = getValidTarget(server, player);
                     if (target == null) {
                        target = findTargetFor(player);
                     }

                     if (target == null) {
                        spectatorTargets.remove(player.m_20148_());
                     } else {
                        spectatorTargets.put(player.m_20148_(), target.m_20148_());
                        forceCamera(player, target);
                     }
                  }
               }
            }
         }
      }
   }

   public static void onPlayerDeath(ServerPlayer player) {
      if (player != null) {
         spectatorTargets.remove(player.m_20148_());
         retargetViewersOf(player.f_8924_, player.m_20148_());
      }
   }

   public static void onPlayerTeamChanged(ServerPlayer player) {
      if (player != null) {
         if (shouldLockSpectator(player)) {
            onPlayerEliminated(player);
         }

         retargetViewersOf(player.f_8924_, player.m_20148_());
      }
   }

   public static void onMatchEnd(MinecraftServer server) {
      clear(server);
   }

   public static void clear(MinecraftServer server) {
      if (server != null) {
         for (UUID spectatorUuid : spectatorTargets.keySet()) {
            ServerPlayer spectator = server.m_6846_().m_11259_(spectatorUuid);
            if (spectator != null && spectator.m_8954_() != spectator) {
               spectator.m_9213_(spectator);
            }
         }
      }

      spectatorTargets.clear();
      tickCounter = 0;
   }

   private static void retargetViewersOf(MinecraftServer server, UUID oldTargetUuid) {
      if (server != null && oldTargetUuid != null) {
         Iterator<Entry<UUID, UUID>> iterator = spectatorTargets.entrySet().iterator();

         while (iterator.hasNext()) {
            Entry<UUID, UUID> entry = iterator.next();
            if (oldTargetUuid.equals(entry.getValue())) {
               ServerPlayer spectator = server.m_6846_().m_11259_(entry.getKey());
               if (spectator != null && shouldLockSpectator(spectator)) {
                  ServerPlayer replacement = findTargetFor(spectator);
                  if (replacement == null) {
                     iterator.remove();
                  } else {
                     entry.setValue(replacement.m_20148_());
                     forceCamera(spectator, replacement);
                  }
               } else {
                  iterator.remove();
               }
            }
         }
      }
   }

   private static ServerPlayer getValidTarget(MinecraftServer server, ServerPlayer spectator) {
      UUID targetUuid = spectatorTargets.get(spectator.m_20148_());
      if (targetUuid == null) {
         return null;
      }

      ServerPlayer target = server.m_6846_().m_11259_(targetUuid);
      return isValidTargetFor(spectator, target) ? target : null;
   }

   private static ServerPlayer findTargetFor(ServerPlayer spectator) {
      if (spectator != null && spectator.f_8924_ != null) {
         if (GameStateManager.getCurrentMode().isTeamMode()) {
            TeamId teamId = TeamMatchManager.getTeam(spectator);
            if (teamId == null) {
               return null;
            }

            for (ServerPlayer candidate : TeamMatchManager.getOnlineTeamMembers(spectator.f_8924_, teamId)) {
               if (isValidTargetFor(spectator, candidate)) {
                  return candidate;
               }
            }

            return null;
         } else {
            for (ServerPlayer candidate : spectator.f_8924_.m_6846_().m_11314_()) {
               if (isValidTargetFor(spectator, candidate)) {
                  return candidate;
               }
            }

            return null;
         }
      } else {
         return null;
      }
   }

   private static boolean isValidTargetFor(ServerPlayer spectator, ServerPlayer target) {
      if (spectator != null && target != null) {
         if (spectator.m_20148_().equals(target.m_20148_())) {
            return false;
         } else if (!LivesManager.isAliveParticipant(target)) {
            return false;
         } else if (!target.m_19880_().contains("war.playing")) {
            return false;
         } else if (target.m_19880_().contains("in_lobby")) {
            return false;
         } else if (target.m_21224_()) {
            return false;
         } else if (target.m_5833_()) {
            return false;
         } else if (LivesManager.isEliminated(target)) {
            return false;
         } else {
            return GameStateManager.getCurrentMode().isTeamMode() ? TeamMatchManager.areTeammates(spectator, target) : true;
         }
      } else {
         return false;
      }
   }

   private static boolean shouldLockSpectator(ServerPlayer player) {
      return player != null && isActiveMatch(player.f_8924_) && LivesManager.isEliminated(player) && player.m_5833_();
   }

   private static boolean isActiveMatch(MinecraftServer server) {
      return server != null && GameStateManager.isRunning(server) && GameStateManager.getMatchPhase() == MatchPhase.RUNNING;
   }

   private static void forceCamera(ServerPlayer spectator, Entity target) {
      if (spectator != null && target != null) {
         if (spectator.m_8954_() != target) {
            spectator.m_9213_(target);
         }
      }
   }
}
