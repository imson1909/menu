package com.makar.tacticaltablet.anticheat;

import com.makar.tacticaltablet.game.GameStateManager;
import com.makar.tacticaltablet.game.lives.LivesManager;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;

public final class MovementAntiCheat {
   private static final long GRACE_MS = 5000L;
   private static final long MIN_SAMPLE_MS = 250L;
   private static final double MAX_BLOCKS_PER_SECOND = 18.0;
   private static final double EXTRA_DISTANCE_ALLOWANCE = 6.0;
   private static final String TAG_IN_LOBBY = "in_lobby";
   private static final String TAG_WAR_PLAYING = "war.playing";
   private static final Map<UUID, MovementAntiCheat.MovementState> states = new ConcurrentHashMap<>();

   private MovementAntiCheat() {
   }

   public static void tick(MinecraftServer server) {
      if (server != null) {
         long now = System.currentTimeMillis();

         for (ServerPlayer player : server.m_6846_().m_11314_()) {
            check(player, now);
         }

         Set<UUID> onlinePlayers = server.m_6846_().m_11314_().stream().<UUID>map(Entity::m_20148_).collect(Collectors.toSet());
         states.keySet().removeIf(uuid -> !onlinePlayers.contains(uuid));
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

   private static void check(ServerPlayer player, long now) {
      UUID uuid = player.m_20148_();
      if (!shouldCheck(player)) {
         states.remove(uuid);
      } else {
         ResourceKey<Level> dimension = player.m_9236_().m_46472_();
         MovementAntiCheat.MovementState state = states.get(uuid);
         if (state != null && state.dimension.equals(dimension)) {
            long elapsedMs = now - state.lastTime;
            if (elapsedMs >= 250L) {
               double dx = player.m_20185_() - state.x;
               double dy = player.m_20186_() - state.y;
               double dz = player.m_20189_() - state.z;
               double distanceSq = dx * dx + dy * dy + dz * dz;
               double elapsedSeconds = elapsedMs / 1000.0;
               double allowed = 18.0 * elapsedSeconds + 6.0;
               if (now > state.graceUntil && distanceSq > allowed * allowed) {
                  AntiCheatManager.record(
                     player,
                     ViolationType.MOVEMENT_ANOMALY,
                     Severity.HIGH,
                     "distance=" + format(Math.sqrt(distanceSq)) + " allowed=" + format(allowed) + " elapsedMs=" + elapsedMs
                  );
               }

               state.update(player, now, dimension);
            }
         } else {
            states.put(uuid, MovementAntiCheat.MovementState.create(player, now, dimension, now + 5000L));
         }
      }
   }

   private static boolean shouldCheck(ServerPlayer player) {
      if (player == null) {
         return false;
      } else if (player.m_5833_()) {
         return false;
      } else if (LivesManager.isEliminated(player)) {
         return false;
      } else {
         return !GameStateManager.isInLobby(player) && !player.m_19880_().contains("in_lobby") ? player.m_19880_().contains("war.playing") : false;
      }
   }

   private static String format(double value) {
      return String.format(Locale.ROOT, "%.2f", value);
   }

   private static final class MovementState {
      private double x;
      private double y;
      private double z;
      private long lastTime;
      private long graceUntil;
      private ResourceKey<Level> dimension;

      private static MovementAntiCheat.MovementState create(ServerPlayer player, long now, ResourceKey<Level> dimension, long graceUntil) {
         MovementAntiCheat.MovementState state = new MovementAntiCheat.MovementState();
         state.graceUntil = graceUntil;
         state.update(player, now, dimension);
         return state;
      }

      private void update(ServerPlayer player, long now, ResourceKey<Level> newDimension) {
         this.x = player.m_20185_();
         this.y = player.m_20186_();
         this.z = player.m_20189_();
         this.lastTime = now;
         this.dimension = newDimension;
      }
   }
}
