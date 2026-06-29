package com.makar.tacticaltablet.game.teleport;

import com.makar.tacticaltablet.core.TacticalTabletMod;
import com.makar.tacticaltablet.game.GameStateManager;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.level.levelgen.Heightmap.Types;

public class SafeTeleport {
   private static final int POOL_POINTS_PER_PLAYER = 8;
   private static final int MIN_POOL_SIZE = 32;
   private static final int MAX_POOL_SIZE = 256;
   private static final int POOL_ATTEMPT_MULTIPLIER = 80;
   private static final int POOL_ATTEMPTS_PER_TICK = 48;
   private static final int FALLBACK_ATTEMPTS_PER_CALL = 96;
   private static final int POOL_CHUNK_LOADS_PER_TICK = 4;
   private static final int FALLBACK_CHUNK_LOADS_PER_CALL = 8;
   private static final int TEAM_CLUSTER_CHUNK_LOADS_PER_CALL = 4;
   private static final int TEST_CHUNK_LOADS_PER_CALL = 16;
   private static final int[][] TEAM_OFFSETS = new int[][]{{0, 0}, {3, 0}, {-3, 0}, {0, 3}, {0, -3}, {4, 3}, {-4, -3}};
   private static final int[] LOCAL_SEARCH_RADII = new int[]{0, 3, 6, 10, 14};
   private static final double MIN_BORDER_MARGIN = 6.0;
   private static final double PREFERRED_BORDER_MARGIN = 24.0;
   private static final double MIN_PLAYER_DISTANCE = 28.0;
   private static final double MAX_PLAYER_DISTANCE = 96.0;
   private static final double PLAYER_DISTANCE_BORDER_FACTOR = 0.45;
   private static final int RECENT_SPAWN_MEMORY = 64;
   private static final double TWO_PI = Math.PI * 2;
   private static final List<BlockPos> preparedSpawns = new ArrayList<>();
   private static final Set<Long> preparedSpawnKeys = new HashSet<>();
   private static final ArrayDeque<BlockPos> recentSpawns = new ArrayDeque<>();
   private static RandomSource poolRandom = RandomSource.m_216327_();
   private static boolean poolPreparing = false;
   private static int poolTarget = 0;
   private static int poolAttempts = 0;
   private static int poolMaxAttempts = 0;
   private static int chunkLoadBudget = 0;

   public static synchronized SafeTeleport.PoolStatus preparePool(MinecraftServer server) {
      return beginPoolPreparation(server);
   }

   public static synchronized SafeTeleport.PoolStatus beginPoolPreparation(MinecraftServer server) {
      preparedSpawns.clear();
      preparedSpawnKeys.clear();
      recentSpawns.clear();
      poolPreparing = false;
      poolTarget = 0;
      poolAttempts = 0;
      poolMaxAttempts = 0;
      poolRandom = RandomSource.m_216327_();
      ServerLevel overworld = GameStateManager.getOverworld(server);
      if (overworld == null) {
         return new SafeTeleport.PoolStatus(0, 0, 0, 0.0, 0.0);
      }

      int players = Math.max(1, server.m_6846_().m_11309_());
      poolTarget = Math.min(256, Math.max(32, players * 8));
      poolMaxAttempts = poolTarget * 80;
      poolPreparing = true;
      WorldBorder border = overworld.m_6857_();
      TacticalTabletMod.LOGGER
         .info(
            "Started RTP spawn pool preparation. target={}, maxAttempts={}, borderSize={}, margin={}",
            new Object[]{poolTarget, poolMaxAttempts, border.m_61959_(), getSpawnBorderMargin(border)}
         );
      return getPoolStatus(overworld);
   }

   public static synchronized SafeTeleport.PoolStatus tickPool(MinecraftServer server) {
      ServerLevel overworld = GameStateManager.getOverworld(server);
      if (overworld == null) {
         poolPreparing = false;
         return new SafeTeleport.PoolStatus(poolTarget, preparedSpawns.size(), poolAttempts, 0.0, 0.0);
      }

      if (!poolPreparing) {
         return getPoolStatus(overworld);
      }

      fillPoolBatch(overworld, 48);
      if (preparedSpawns.size() >= poolTarget || poolAttempts >= poolMaxAttempts) {
         poolPreparing = false;
         WorldBorder border = overworld.m_6857_();
         TacticalTabletMod.LOGGER
            .info(
               "Finished RTP spawn pool preparation. prepared={} target={} attempts={} borderSize={}, margin={}",
               new Object[]{preparedSpawns.size(), poolTarget, poolAttempts, border.m_61959_(), getSpawnBorderMargin(border)}
            );
      }

      return getPoolStatus(overworld);
   }

   public static synchronized boolean isPoolPreparing() {
      return poolPreparing;
   }

   public static synchronized SafeTeleport.TestResult testPoints(MinecraftServer server, int requested) {
      ServerLevel overworld = GameStateManager.getOverworld(server);
      if (overworld == null) {
         return new SafeTeleport.TestResult(Math.max(0, requested), 0, 0, 0.0, 0.0, List.of());
      }

      int target = Math.min(256, Math.max(1, requested));
      int maxAttempts = target * 80;
      RandomSource random = RandomSource.m_216327_();
      List<BlockPos> valid = new ArrayList<>();
      List<SafeTeleport.PlayerPosition> validPositions = new ArrayList<>();
      Set<Long> keys = new HashSet<>();
      int attempts = 0;
      chunkLoadBudget = 16;

      try {
         while (valid.size() < target && attempts < maxAttempts) {
            attempts++;
            boolean strictDistance = attempts < maxAttempts * 3 / 4;
            BlockPos position = createRandomSafePoint(overworld, random, validPositions, strictDistance);
            if (position != null && keys.add(position.m_121878_())) {
               valid.add(position);
               validPositions.add(toPlayerPosition(position));
            }
         }
      } finally {
         chunkLoadBudget = 0;
      }

      WorldBorder var15 = overworld.m_6857_();
      return new SafeTeleport.TestResult(
         target, valid.size(), attempts, var15.m_61959_(), getSpawnBorderMargin(var15), List.copyOf(valid.subList(0, Math.min(8, valid.size())))
      );
   }

   public static synchronized int getPreparedCount() {
      return preparedSpawns.size();
   }

   public static synchronized SafeTeleport.PoolStatus getPoolStatus(MinecraftServer server) {
      return getPoolStatus(GameStateManager.getOverworld(server));
   }

   public static synchronized void clearPool() {
      preparedSpawns.clear();
      preparedSpawnKeys.clear();
      recentSpawns.clear();
      poolPreparing = false;
      poolTarget = 0;
      poolAttempts = 0;
      poolMaxAttempts = 0;
   }

   public static boolean teleport(ServerPlayer player) {
      if (player == null) {
         return false;
      }

      ServerLevel overworld = GameStateManager.getOverworld(player.f_8924_);
      if (overworld == null) {
         return false;
      }

      RandomSource random = RandomSource.m_216327_();
      BlockPos safePos = takeBestPreparedPoint(player, overworld, random);
      if (safePos == null) {
         safePos = findBestFallbackPoint(player, overworld, random);
      }

      if (safePos == null) {
         WorldBorder border = overworld.m_6857_();
         TacticalTabletMod.LOGGER
            .warn(
               "Safe RTP point not found for {}. borderCenter=({}, {}), borderSize={}, attempts={}, preparedPool={}",
               new Object[]{player.m_36316_().getName(), border.m_6347_(), border.m_6345_(), border.m_61959_(), 96, getPreparedCount()}
            );
         return false;
      } else {
         player.m_5489_(overworld);
         player.m_8999_(overworld, safePos.m_123341_() + 0.5, safePos.m_123342_(), safePos.m_123343_() + 0.5, random.m_188501_() * 360.0F, 0.0F);
         rememberSpawn(safePos);
         return true;
      }
   }

   public static boolean teleportTeam(List<ServerPlayer> players) {
      if (players != null && !players.isEmpty()) {
         if (players.size() == 1) {
            return teleport(players.get(0));
         }

         ServerPlayer anchorPlayer = players.get(0);
         if (anchorPlayer == null) {
            return false;
         }

         ServerLevel overworld = GameStateManager.getOverworld(anchorPlayer.f_8924_);
         if (overworld == null) {
            return false;
         }

         RandomSource random = RandomSource.m_216327_();
         BlockPos anchor = takeBestPreparedPoint(anchorPlayer, overworld, random);
         if (anchor == null) {
            anchor = findBestFallbackPoint(anchorPlayer, overworld, random);
         }

         if (anchor == null) {
            return false;
         }

         chunkLoadBudget = 4;

         List<BlockPos> positions;
         try {
            positions = findTeamCluster(overworld, anchor, players.size());
         } finally {
            chunkLoadBudget = 0;
         }

         if (positions.size() < players.size()) {
            TacticalTabletMod.LOGGER
               .warn(
                  "Safe team RTP cluster not found. requested={}, found={}, anchor={}, preparedPool={}",
                  new Object[]{players.size(), positions.size(), anchor, getPreparedCount()}
               );
            return false;
         }

         for (int i = 0; i < players.size(); i++) {
            ServerPlayer player = players.get(i);
            if (player != null) {
               BlockPos safePos = positions.get(i);
               player.m_5489_(overworld);
               player.m_8999_(overworld, safePos.m_123341_() + 0.5, safePos.m_123342_(), safePos.m_123343_() + 0.5, random.m_188501_() * 360.0F, 0.0F);
               rememberSpawn(safePos);
            }
         }

         return true;
      } else {
         return false;
      }
   }

   private static List<BlockPos> findTeamCluster(ServerLevel overworld, BlockPos anchor, int count) {
      List<BlockPos> positions = new ArrayList<>();

      for (int[] offset : TEAM_OFFSETS) {
         if (positions.size() >= count) {
            break;
         }

         BlockPos base = anchor.m_7918_(offset[0], 0, offset[1]);
         BlockPos safe = findSafeNear(overworld, base.m_123341_(), base.m_123343_(), getSpawnBorderMargin(overworld.m_6857_()));
         if (safe != null && !positions.contains(safe)) {
            positions.add(safe);
         }
      }

      return positions;
   }

   private static int fillPoolBatch(ServerLevel overworld, int maxBatchAttempts) {
      List<SafeTeleport.PlayerPosition> preparedPositions = blockPositionsToPlayerPositions(preparedSpawns);
      int batchAttempts = 0;
      chunkLoadBudget = 4;

      try {
         while (preparedSpawns.size() < poolTarget && poolAttempts < poolMaxAttempts && batchAttempts < maxBatchAttempts) {
            batchAttempts++;
            poolAttempts++;
            boolean strictDistance = poolAttempts < poolMaxAttempts * 3 / 4;
            BlockPos position = createRandomSafePoint(overworld, poolRandom, preparedPositions, strictDistance);
            if (position != null && preparedSpawnKeys.add(position.m_121878_())) {
               preparedSpawns.add(position);
               preparedPositions.add(toPlayerPosition(position));
            }
         }
      } finally {
         chunkLoadBudget = 0;
      }

      return batchAttempts;
   }

   private static SafeTeleport.PoolStatus getPoolStatus(ServerLevel overworld) {
      if (overworld == null) {
         return new SafeTeleport.PoolStatus(poolTarget, preparedSpawns.size(), poolAttempts, 0.0, 0.0);
      }

      WorldBorder border = overworld.m_6857_();
      return new SafeTeleport.PoolStatus(poolTarget, preparedSpawns.size(), poolAttempts, border.m_61959_(), getSpawnBorderMargin(border));
   }

   private static synchronized BlockPos takeBestPreparedPoint(ServerPlayer player, ServerLevel overworld, RandomSource random) {
      pruneInvalidPreparedSpawns(overworld);
      if (preparedSpawns.isEmpty()) {
         return null;
      }

      List<SafeTeleport.PlayerPosition> occupiedPositions = snapshotOccupiedPositions(player, overworld);
      List<SafeTeleport.PlayerPosition> recentPositions = snapshotRecentSpawnPositions();
      double minPlayerDistance = getMinPlayerDistance(overworld.m_6857_());
      if (occupiedPositions.isEmpty() && recentPositions.isEmpty()) {
         int index = random.m_188503_(preparedSpawns.size());
         BlockPos position = preparedSpawns.remove(index);
         preparedSpawnKeys.remove(position.m_121878_());
         return isSafeSpawn(overworld, position) ? position : null;
      }

      BlockPos bestStrictPosition = null;
      double bestStrictScore = -1.0;
      BlockPos bestRelaxedPosition = null;
      double bestRelaxedScore = -1.0;

      for (BlockPos position : preparedSpawns) {
         double score = scoreSpawnPosition(occupiedPositions, recentPositions, position);
         if (score > bestRelaxedScore) {
            bestRelaxedScore = score;
            bestRelaxedPosition = position;
         }

         if ((occupiedPositions.isEmpty() || isFarEnough(occupiedPositions, position, minPlayerDistance)) && score > bestStrictScore) {
            bestStrictScore = score;
            bestStrictPosition = position;
         }
      }

      BlockPos selected = bestStrictPosition;
      if (selected == null && occupiedPositions.isEmpty()) {
         selected = bestRelaxedPosition;
      }

      if (selected == null) {
         return null;
      }

      BlockPos position = selected;
      preparedSpawns.remove(position);
      preparedSpawnKeys.remove(position.m_121878_());
      return position;
   }

   private static void pruneInvalidPreparedSpawns(ServerLevel overworld) {
      for (int i = preparedSpawns.size() - 1; i >= 0; i--) {
         BlockPos position = preparedSpawns.get(i);
         if (!isSafeSpawn(overworld, position)) {
            preparedSpawns.remove(i);
            preparedSpawnKeys.remove(position.m_121878_());
         }
      }
   }

   private static BlockPos findBestFallbackPoint(ServerPlayer player, ServerLevel overworld, RandomSource random) {
      List<SafeTeleport.PlayerPosition> occupiedPositions = snapshotOccupiedPositions(player, overworld);
      List<SafeTeleport.PlayerPosition> recentPositions = snapshotRecentSpawnPositions();
      double minPlayerDistance = getMinPlayerDistance(overworld.m_6857_());
      BlockPos bestStrictPosition = null;
      double bestStrictScore = -1.0;
      BlockPos bestRelaxedPosition = null;
      double bestRelaxedScore = -1.0;
      chunkLoadBudget = 8;

      try {
         for (int i = 0; i < 96; i++) {
            BlockPos position = createRandomSafePoint(overworld, random, List.of(), false);
            if (position != null) {
               double score = scoreSpawnPosition(occupiedPositions, recentPositions, position);
               if (score > bestRelaxedScore) {
                  bestRelaxedScore = score;
                  bestRelaxedPosition = position;
               }

               if ((occupiedPositions.isEmpty() || isFarEnough(occupiedPositions, position, minPlayerDistance)) && score > bestStrictScore) {
                  bestStrictScore = score;
                  bestStrictPosition = position;
               }
            }
         }
      } finally {
         chunkLoadBudget = 0;
      }

      return bestStrictPosition != null ? bestStrictPosition : bestRelaxedPosition;
   }

   private static synchronized void rememberSpawn(BlockPos position) {
      recentSpawns.addLast(position.m_7949_());

      while (recentSpawns.size() > 64) {
         recentSpawns.removeFirst();
      }
   }

   private static synchronized List<SafeTeleport.PlayerPosition> snapshotRecentSpawnPositions() {
      List<SafeTeleport.PlayerPosition> positions = new ArrayList<>();

      for (BlockPos position : recentSpawns) {
         positions.add(toPlayerPosition(position));
      }

      return positions;
   }

   private static List<SafeTeleport.PlayerPosition> blockPositionsToPlayerPositions(List<BlockPos> positions) {
      List<SafeTeleport.PlayerPosition> result = new ArrayList<>();

      for (BlockPos position : positions) {
         result.add(toPlayerPosition(position));
      }

      return result;
   }

   private static SafeTeleport.PlayerPosition toPlayerPosition(BlockPos position) {
      return new SafeTeleport.PlayerPosition(position.m_123341_() + 0.5, position.m_123343_() + 0.5);
   }

   private static double scoreSpawnPosition(
      List<SafeTeleport.PlayerPosition> occupiedPositions, List<SafeTeleport.PlayerPosition> recentPositions, BlockPos position
   ) {
      double x = position.m_123341_() + 0.5;
      double z = position.m_123343_() + 0.5;
      double score = 0.0;
      if (!occupiedPositions.isEmpty()) {
         score += nearestDistanceSq(occupiedPositions, x, z) * 4.0;
      }

      if (!recentPositions.isEmpty()) {
         score += nearestDistanceSq(recentPositions, x, z);
      }

      return score;
   }

   private static BlockPos createRandomSafePoint(
      ServerLevel overworld, RandomSource random, List<SafeTeleport.PlayerPosition> occupiedPositions, boolean strictDistance
   ) {
      WorldBorder border = overworld.m_6857_();
      double centerX = border.m_6347_();
      double centerZ = border.m_6345_();
      double borderRadius = border.m_61959_() / 2.0;
      double margin = getSpawnBorderMargin(border);
      if (borderRadius <= margin + 2.0) {
         return null;
      } else {
         double minRadius = 0.0;
         double maxRadius = Math.max(1.0, borderRadius - margin);
         double minDistance = getMinPlayerDistance(border);
         double angle = random.m_188500_() * (Math.PI * 2);
         double radius = Math.sqrt(random.m_188500_()) * (maxRadius - minRadius) + minRadius;
         double x = centerX + Math.cos(angle) * radius;
         double z = centerZ + Math.sin(angle) * radius;
         BlockPos position = findSafeNear(overworld, x, z, margin);
         if (position == null) {
            return null;
         } else {
            return strictDistance && !isFarEnough(occupiedPositions, position, minDistance) ? null : position;
         }
      }
   }

   private static BlockPos findSafeNear(ServerLevel level, double x, double z, double margin) {
      int baseX = (int)Math.floor(x);
      int baseZ = (int)Math.floor(z);

      for (int radius : LOCAL_SEARCH_RADII) {
         if (radius == 0) {
            BlockPos position = getSafeColumnPosition(level, baseX, baseZ, margin);
            if (position != null) {
               return position;
            }
         } else {
            for (int dx = -radius; dx <= radius; dx += 3) {
               for (int dz = -radius; dz <= radius; dz += 3) {
                  if (Math.abs(dx) == radius || Math.abs(dz) == radius) {
                     BlockPos position = getSafeColumnPosition(level, baseX + dx, baseZ + dz, margin);
                     if (position != null) {
                        return position;
                     }
                  }
               }
            }
         }
      }

      return null;
   }

   private static BlockPos getSafeColumnPosition(ServerLevel level, int x, int z, double margin) {
      BlockPos column = new BlockPos(x, 0, z);
      if (!isInsideBorder(level.m_6857_(), x + 0.5, z + 0.5, margin)) {
         return null;
      } else if (!ensureChunkAvailable(level, x >> 4, z >> 4)) {
         return null;
      } else {
         BlockPos pos = level.m_5452_(Types.MOTION_BLOCKING_NO_LEAVES, column);
         if (!isInsideBorder(level.m_6857_(), pos.m_123341_() + 0.5, pos.m_123343_() + 0.5, margin)) {
            return null;
         } else {
            return isSafeSpawn(level, pos) ? pos : null;
         }
      }
   }

   private static boolean ensureChunkAvailable(ServerLevel level, int chunkX, int chunkZ) {
      if (level.m_7232_(chunkX, chunkZ)) {
         return true;
      }

      if (chunkLoadBudget <= 0) {
         return false;
      }

      chunkLoadBudget--;
      level.m_6325_(chunkX, chunkZ);
      return true;
   }

   private static double getSpawnBorderMargin(WorldBorder border) {
      double borderRadius = border.m_61959_() / 2.0;
      return borderRadius <= 40.0 ? Math.max(6.0, borderRadius * 0.15) : 24.0;
   }

   private static double getMinPlayerDistance(WorldBorder border) {
      double playableRadius = Math.max(1.0, border.m_61959_() / 2.0 - getSpawnBorderMargin(border));
      double scaledDistance = playableRadius * 0.45;
      return Math.min(96.0, Math.max(28.0, scaledDistance));
   }

   private static boolean isInsideBorder(WorldBorder border, double x, double z, double margin) {
      return x > border.m_61955_() + margin && x < border.m_61957_() - margin && z > border.m_61956_() + margin && z < border.m_61958_() - margin;
   }

   private static List<SafeTeleport.PlayerPosition> snapshotOccupiedPositions(ServerPlayer player, ServerLevel overworld) {
      List<SafeTeleport.PlayerPosition> positions = new ArrayList<>();

      for (ServerPlayer other : player.f_8924_.m_6846_().m_11314_()) {
         if (other != player && other.m_9236_().m_46472_().equals(overworld.m_46472_()) && other.m_19880_().contains("war.playing")) {
            positions.add(new SafeTeleport.PlayerPosition(other.m_20185_(), other.m_20189_()));
         }
      }

      return positions;
   }

   private static boolean isFarEnough(List<SafeTeleport.PlayerPosition> positions, double x, double z, double minDistance) {
      return nearestDistanceSq(positions, x, z) >= minDistance * minDistance;
   }

   private static boolean isFarEnough(List<SafeTeleport.PlayerPosition> positions, BlockPos position, double minDistance) {
      return isFarEnough(positions, position.m_123341_() + 0.5, position.m_123343_() + 0.5, minDistance);
   }

   private static double nearestDistanceSq(List<SafeTeleport.PlayerPosition> positions, double x, double z) {
      if (positions.isEmpty()) {
         return Double.MAX_VALUE;
      }

      double nearest = Double.MAX_VALUE;

      for (SafeTeleport.PlayerPosition position : positions) {
         double dx = position.x - x;
         double dz = position.z - z;
         nearest = Math.min(nearest, dx * dx + dz * dz);
      }

      return nearest;
   }

   private static boolean isSafeSpawn(ServerLevel level, BlockPos pos) {
      if (!level.m_7232_(pos.m_123341_() >> 4, pos.m_123343_() >> 4)) {
         return false;
      } else if (pos.m_123342_() <= level.m_141937_() + 2) {
         return false;
      } else if (pos.m_123342_() >= level.m_151558_() - 2) {
         return false;
      } else {
         WorldBorder border = level.m_6857_();
         if (!isInsideBorder(border, pos.m_123341_() + 0.5, pos.m_123343_() + 0.5, getSpawnBorderMargin(border))) {
            return false;
         } else {
            BlockPos groundPos = pos.m_7495_();
            BlockState ground = level.m_8055_(groundPos);
            BlockState feet = level.m_8055_(pos);
            BlockState head = level.m_8055_(pos.m_7494_());
            if (level.m_7702_(groundPos) != null) {
               return false;
            } else if (level.m_7702_(pos) != null) {
               return false;
            } else if (level.m_7702_(pos.m_7494_()) != null) {
               return false;
            } else if (!isStandable(level, groundPos, ground)) {
               return false;
            } else if (!hasStableGroundBelow(level, groundPos)) {
               return false;
            } else if (!isPassable(level, pos, feet)) {
               return false;
            } else {
               return !isPassable(level, pos.m_7494_(), head) ? false : !isHazard(ground) && !isHazard(feet) && !isHazard(head);
            }
         }
      }
   }

   private static boolean hasStableGroundBelow(ServerLevel level, BlockPos groundPos) {
      for (int i = 1; i <= 3; i++) {
         BlockPos belowPos = groundPos.m_6625_(i);
         BlockState below = level.m_8055_(belowPos);
         if (below.m_60795_()) {
            return false;
         }

         if (!below.m_60819_().m_76178_()) {
            return false;
         }

         if (below.m_60812_(level, belowPos).m_83281_()) {
            return false;
         }
      }

      return true;
   }

   private static boolean isStandable(ServerLevel level, BlockPos pos, BlockState state) {
      if (state.m_60795_()) {
         return false;
      } else if (state.m_204336_(BlockTags.f_13035_)) {
         return false;
      } else {
         return !state.m_60819_().m_76178_() ? false : !state.m_60812_(level, pos).m_83281_();
      }
   }

   private static boolean isPassable(ServerLevel level, BlockPos pos, BlockState state) {
      return !state.m_60819_().m_76178_() ? false : state.m_60795_() || state.m_60812_(level, pos).m_83281_();
   }

   private static boolean isHazard(BlockState state) {
      return state.m_60713_(Blocks.f_50128_)
         || state.m_60713_(Blocks.f_50450_)
         || state.m_60713_(Blocks.f_50683_)
         || state.m_60713_(Blocks.f_50684_)
         || state.m_60713_(Blocks.f_50083_)
         || state.m_60713_(Blocks.f_50084_)
         || state.m_60713_(Blocks.f_50685_)
         || state.m_60713_(Blocks.f_152499_);
   }

   private record PlayerPosition(double x, double z) {
   }

   public record PoolStatus(int target, int prepared, int attempts, double borderSize, double margin) {
   }

   public record TestResult(int requested, int valid, int attempts, double borderSize, double margin, List<BlockPos> samples) {
   }
}
