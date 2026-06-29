package com.makar.tacticaltablet.game.respawn;

import com.makar.tacticaltablet.game.MapSetManager;
import com.makar.tacticaltablet.game.clanwar.ClanWarManager;
import com.makar.tacticaltablet.game.extraction.ExtractionPointManager;
import com.makar.tacticaltablet.game.lives.LivesManager;
import com.makar.tacticaltablet.game.lobby.LobbyManager;
import com.makar.tacticaltablet.progression.ClassXPManager;
import com.makar.tacticaltablet.progression.PlayerProgressManager;
import com.makar.tacticaltablet.tablet.net.DeathScreenPacket;
import com.makar.tacticaltablet.tablet.net.PacketHandler;
import com.makar.tacticaltablet.voice.VoiceChatTeamManager;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.Map.Entry;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.level.GameType;

public final class DeathTransitionManager {
   private static final int SCREEN_TICKS = 60;
   private static final Map<UUID, DeathTransitionManager.DeathMessage> pendingMessages = new HashMap<>();
   private static final Map<UUID, Integer> activeTransitions = new HashMap<>();

   private DeathTransitionManager() {
   }

   public static void recordDeath(ServerPlayer victim, DamageSource source) {
      if (victim != null) {
         if ((source == null ? null : source.m_7639_()) instanceof ServerPlayer killer && !killer.m_20148_().equals(victim.m_20148_())) {
            pendingMessages.put(
               victim.m_20148_(),
               new DeathTransitionManager.DeathMessage(
                  "Тебя убили :(", "Причина этому — \"" + killer.m_36316_().getName() + "\"", PlayerProgressManager.isSadTromboneKillsEnabled(killer)
               )
            );
         } else {
            pendingMessages.put(victim.m_20148_(), new DeathTransitionManager.DeathMessage("Зачем ты это...", "Умер...", false));
         }
      }
   }

   public static boolean begin(ServerPlayer player) {
      if (player == null) {
         return false;
      }

      DeathTransitionManager.DeathMessage message = pendingMessages.remove(player.m_20148_());
      if (message == null) {
         return false;
      }

      activeTransitions.put(player.m_20148_(), 60);
      player.m_143403_(GameType.SPECTATOR);
      PacketHandler.sendToPlayer(player, new DeathScreenPacket(message.title(), message.subtitle(), 60, message.playSadTrombone()));
      return true;
   }

   public static void tick(MinecraftServer server) {
      if (server != null && !activeTransitions.isEmpty()) {
         Iterator<Entry<UUID, Integer>> iterator = activeTransitions.entrySet().iterator();

         while (iterator.hasNext()) {
            Entry<UUID, Integer> entry = iterator.next();
            int ticksLeft = entry.getValue() - 1;
            if (ticksLeft > 0) {
               entry.setValue(ticksLeft);
            } else {
               iterator.remove();
               ServerPlayer player = server.m_6846_().m_11259_(entry.getKey());
               if (player != null) {
                  finishRespawn(player);
               }
            }
         }
      }
   }

   public static void clear(ServerPlayer player) {
      if (player != null) {
         pendingMessages.remove(player.m_20148_());
         activeTransitions.remove(player.m_20148_());
      }
   }

   public static void clearAll() {
      pendingMessages.clear();
      activeTransitions.clear();
   }

   private static void finishRespawn(ServerPlayer player) {
      if (LivesManager.ensureEliminatedIfOutOfLives(player)) {
         ClassXPManager.sync(player);
      } else {
         if (MapSetManager.isClanWarSet()) {
            if (ClanWarManager.shouldMoveToLobbyAfterDeath(player)) {
               ClanWarManager.preparePlayerForRegroup(player);
               LobbyManager.moveToLobby(player);
               ExtractionPointManager.onPlayerRespawn(player);
               ClassXPManager.sync(player);
               return;
            }

            if (ClanWarManager.shouldKeepSpectating(player)) {
               ClanWarManager.keepSpectator(player);
               ClassXPManager.sync(player);
               return;
            }
         }

         LobbyManager.moveToLobby(player);
         ExtractionPointManager.onPlayerRespawn(player);
         VoiceChatTeamManager.assignPlayerToVoiceGroup(player);
         ClassXPManager.sync(player);
      }
   }

   private record DeathMessage(String title, String subtitle, boolean playSadTrombone) {
   }
}
