package com.makar.tacticaltablet.game.respawn;

import com.makar.tacticaltablet.admin.TestModeManager;
import com.makar.tacticaltablet.airdrop.AirdropManager;
import com.makar.tacticaltablet.clan.ClanManager;
import com.makar.tacticaltablet.game.GameStateManager;
import com.makar.tacticaltablet.game.MapSetManager;
import com.makar.tacticaltablet.game.clanwar.ClanWarManager;
import com.makar.tacticaltablet.game.extraction.ExtractionPointManager;
import com.makar.tacticaltablet.game.lives.LivesManager;
import com.makar.tacticaltablet.game.lobby.LobbyManager;
import com.makar.tacticaltablet.game.team.TeamId;
import com.makar.tacticaltablet.game.team.TeamMatchManager;
import com.makar.tacticaltablet.game.teleport.SafeTeleport;
import com.makar.tacticaltablet.inventory.InventoryManager;
import com.makar.tacticaltablet.tablet.PlayerTabletState;
import com.makar.tacticaltablet.voice.VoiceChatTeamManager;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.UUID;
import java.util.Map.Entry;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

public class RtpTimerManager {
   private static final Map<UUID, Integer> timers = new HashMap<>();
   private static final Queue<UUID> rtpQueue = new ArrayDeque<>();
   private static final Set<UUID> queuedPlayers = new HashSet<>();
   private static final int RTP_DELAY = 600;
   private static final int RETRY_DELAY = 60;
   private static final int RTP_PER_TICK = 1;
   private static final int POST_RTP_INVULNERABILITY_TICKS = 100;

   public static void start(ServerPlayer player) {
      if (player != null) {
         if (GameStateManager.isRunning(player.f_8924_)) {
            if (!PlayerTabletState.isRtpUsed(player)) {
               if (LivesManager.canContinueMatch(player)) {
                  if (!MapSetManager.isClanWarSet() || ClanWarManager.hasClan(player)) {
                     UUID uuid = player.m_20148_();
                     timers.put(uuid, 600);
                     dequeue(uuid);
                     LobbyManager.sync(player);
                  }
               }
            }
         }
      }
   }

   public static void cancel(ServerPlayer player) {
      if (player != null) {
         UUID uuid = player.m_20148_();
         timers.remove(uuid);
         dequeue(uuid);
      }
   }

   public static void clearAll() {
      timers.clear();
      rtpQueue.clear();
      queuedPlayers.clear();
   }

   public static int getTimeLeft(ServerPlayer player) {
      return player == null ? 0 : timers.getOrDefault(player.m_20148_(), 0);
   }

   public static boolean canRtp(ServerPlayer player, boolean notify) {
      if (player == null) {
         return false;
      }

      if (!GameStateManager.isRunning(player.f_8924_)) {
         if (notify) {
            player.m_213846_(Component.m_237113_("[WAR] Матч ещё не идёт."));
         }

         return false;
      } else if (!LivesManager.canContinueMatch(player)) {
         if (notify) {
            player.m_213846_(Component.m_237113_("[WAR] Ты выбыл из матча."));
         }

         return false;
      } else if (!GameStateManager.isInLobby(player)) {
         if (notify) {
            player.m_213846_(Component.m_237113_("[WAR] RTP доступен только в лобби."));
         }

         return false;
      } else if (!player.m_19880_().contains("in_lobby")) {
         if (notify) {
            player.m_213846_(Component.m_237113_("[WAR] Ты не отмечен как игрок лобби."));
         }

         return false;
      } else if (PlayerTabletState.isRtpUsed(player)) {
         if (notify) {
            player.m_213846_(Component.m_237113_("[WAR] RTP уже использован."));
         }

         return false;
      } else {
         if (MapSetManager.isClanWarSet()) {
            String clanId = ClanManager.getClanIdForPlayer(player);
            if (clanId.isBlank()) {
               if (notify) {
                  ClanWarManager.showNeedClan(player);
               }

               return false;
            }

            if (ClanWarManager.isClanEliminated(clanId)) {
               if (notify) {
                  player.m_213846_(Component.m_237113_("[WAR] Клан выбыл из войны кланов."));
               }

               return false;
            }
         }

         int requiredPlayers = TestModeManager.getRequiredPlayers(2);
         if (GameStateManager.onlinePlayers(player.f_8924_) < requiredPlayers) {
            if (notify) {
               player.m_213846_(Component.m_237113_("[WAR] Для RTP нужно игроков: " + requiredPlayers + "."));
            }

            return false;
         } else {
            return true;
         }
      }
   }

   public static void forceRtp(ServerPlayer player) {
      if (!canRtp(player, true)) {
         LobbyManager.sync(player);
      } else {
         UUID uuid = player.m_20148_();
         timers.remove(uuid);
         enqueue(uuid);
         LobbyManager.sync(player);
      }
   }

   public static void tick(MinecraftServer server) {
      if (server != null) {
         SafeTeleport.tickPool(server);
         tickTimers(server);
         processQueue(server);
      }
   }

   private static void tickTimers(MinecraftServer server) {
      if (!timers.isEmpty()) {
         Iterator<Entry<UUID, Integer>> iterator = timers.entrySet().iterator();

         while (iterator.hasNext()) {
            Entry<UUID, Integer> entry = iterator.next();
            UUID uuid = entry.getKey();
            ServerPlayer player = server.m_6846_().m_11259_(uuid);
            if (player == null || PlayerTabletState.isRtpUsed(player) || !LivesManager.canContinueMatch(player)) {
               iterator.remove();
               dequeue(uuid);
            } else if (!canRtp(player, false)) {
               if (GameStateManager.isRunning(server) && GameStateManager.isInLobby(player) && player.m_19880_().contains("in_lobby")) {
                  entry.setValue(600);
               } else {
                  iterator.remove();
                  dequeue(uuid);
               }
            } else {
               int time = entry.getValue() - 1;
               if (time <= 0) {
                  enqueue(uuid);
                  iterator.remove();
               } else {
                  entry.setValue(time);
               }
            }
         }
      }
   }

   private static void processQueue(MinecraftServer server) {
      for (int i = 0; i < 1 && !rtpQueue.isEmpty(); i++) {
         UUID uuid = rtpQueue.poll();
         queuedPlayers.remove(uuid);
         ServerPlayer player = server.m_6846_().m_11259_(uuid);
         if (canRtp(player, false) && (!GameStateManager.getCurrentMode().isTeamMode() || !processTeamRtp(server, player))) {
            boolean success = SafeTeleport.teleport(player);
            if (success) {
               finishRtp(player);
            } else {
               timers.put(uuid, 60);
               player.m_213846_(Component.m_237113_("[WAR] Безопасная точка RTP не найдена. Повторяем..."));
               LobbyManager.sync(player);
            }
         }
      }
   }

   private static boolean processTeamRtp(MinecraftServer server, ServerPlayer trigger) {
      TeamId teamId = TeamMatchManager.getTeam(trigger);
      if (teamId == null) {
         return false;
      }

      List<ServerPlayer> members = TeamMatchManager.getOnlineTeamMembers(server, teamId).stream().filter(player -> canRtp(player, false)).toList();
      if (members.isEmpty()) {
         return false;
      }

      boolean success = SafeTeleport.teleportTeam(members);
      if (!success) {
         for (ServerPlayer member : members) {
            timers.put(member.m_20148_(), 60);
            member.m_213846_(Component.m_237113_("[WAR] Безопасная командная точка RTP не найдена. Повторяем..."));
            LobbyManager.sync(member);
         }

         return true;
      } else {
         for (ServerPlayer member : members) {
            timers.remove(member.m_20148_());
            dequeue(member.m_20148_());
            finishRtp(member);
         }

         return true;
      }
   }

   private static void finishRtp(ServerPlayer player) {
      LivesManager.ensureStarted(player);
      PlayerTabletState.setRtpUsed(player);
      player.m_20137_("in_lobby");
      player.m_20049_("war.playing");
      VoiceChatTeamManager.assignPlayerToVoiceGroup(player);
      player.m_21195_(MobEffects.f_19606_);
      player.m_7292_(new MobEffectInstance(MobEffects.f_19606_, 100, 255, false, false, true));
      AirdropManager.giveCompassToJoiningPlayer(player);
      ExtractionPointManager.giveCompassToActiveParticipant(player);
      if (PlayerTabletState.isKitUsed(player)) {
         InventoryManager.clearTablets(player);
      } else {
         InventoryManager.giveTabletIfMissing(player);
      }

      LobbyManager.sync(player);
   }

   private static void enqueue(UUID uuid) {
      if (queuedPlayers.add(uuid)) {
         rtpQueue.add(uuid);
      }
   }

   private static void dequeue(UUID uuid) {
      if (queuedPlayers.remove(uuid)) {
         rtpQueue.remove(uuid);
      }
   }
}
