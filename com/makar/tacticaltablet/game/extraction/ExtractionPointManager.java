package com.makar.tacticaltablet.game.extraction;

import com.makar.tacticaltablet.core.TacticalTabletMod;
import com.makar.tacticaltablet.game.GameStateManager;
import com.makar.tacticaltablet.game.MatchPhase;
import com.makar.tacticaltablet.game.lives.LivesManager;
import com.makar.tacticaltablet.game.team.TeamId;
import com.makar.tacticaltablet.game.team.TeamMatchManager;
import com.makar.tacticaltablet.progression.ClassXPManager;
import com.makar.tacticaltablet.progression.PlayerProgressManager;
import com.makar.tacticaltablet.progression.XpNotifier;
import com.makar.tacticaltablet.tablet.PlayerTabletState;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.BossEvent.BossBarColor;
import net.minecraft.world.BossEvent.BossBarOverlay;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.level.levelgen.Heightmap.Types;
import net.minecraft.world.phys.Vec3;

public final class ExtractionPointManager {
   private static final Random RANDOM = new Random();
   private static ExtractionPointData data = ExtractionPointData.idle();
   private static ExtractionPointConfig config = new ExtractionPointConfig();
   private static long matchStartTick = 0L;
   private static int particleTicker = 0;
   private static double decayRemainderTicks = 0.0;
   private static Boolean forcedContested = null;
   private static UUID forcedOwnerPlayerId = null;
   private static ExtractionPointVisualHelper.VisualMode debugVisualMode = null;

   private ExtractionPointManager() {
   }

   public static void onMatchStart(MinecraftServer server) {
      reset(server);
      config = ExtractionPointConfig.load(server);
      matchStartTick = now(server);
      if (config.enabled && LivesManager.getAlivePlayerCount(server) >= config.minAlivePlayers) {
         int delaySeconds = config.startDelayMinSeconds;
         int range = config.startDelayMaxSeconds - config.startDelayMinSeconds;
         if (range > 0) {
            delaySeconds += RANDOM.nextInt(range + 1);
         }

         data = new ExtractionPointData();
         data.eventId = UUID.randomUUID();
         data.state = ExtractionPointState.SCHEDULED;
         data.scheduledStartTick = now(server) + delaySeconds * 20L;
         data.expireAtMatchTick = matchStartTick + config.expireAtMatchTimeSeconds * 20L;
         data.radius = config.captureRadius;
         data.halfHeight = effectiveCaptureHalfHeight();
         data.requiredCaptureTicks = config.requiredCaptureSeconds * 20;
         data.nextMilestoneRewardAtTicks = config.milestoneRewardIntervalSeconds * 20;
         TacticalTabletMod.LOGGER.info("ExtractionPoint scheduled: delay={}s, eventId={}", delaySeconds, data.eventId);
      }
   }

   public static void tick(MinecraftServer server) {
      if (server != null && data != null && data.state != ExtractionPointState.IDLE) {
         if (GameStateManager.isRunning(server) && GameStateManager.getMatchPhase() == MatchPhase.RUNNING) {
            long tick = now(server);
            if (data.state == ExtractionPointState.SCHEDULED) {
               if (tick >= data.scheduledStartTick) {
                  activateRandom(server);
               }
            } else if (data.state == ExtractionPointState.ACTIVE) {
               tickActive(server);
            } else {
               if (data.state == ExtractionPointState.ENDING_WINNER || data.state == ExtractionPointState.ENDING_EXPIRED) {
                  tickEnding(server);
                  if (tick >= data.endingUntilTick) {
                     cleanup(server);
                  }
               }
            }
         }
      }
   }

   public static void reset(MinecraftServer server) {
      cleanup(server);
      config = ExtractionPointConfig.load(server);
      data = ExtractionPointData.idle();
      forcedContested = null;
      forcedOwnerPlayerId = null;
      debugVisualMode = null;
      particleTicker = 0;
      decayRemainderTicks = 0.0;
   }

   public static void cleanup(MinecraftServer server) {
      if (data != null && data.bossbar != null) {
         data.bossbar.m_7706_();
         data.bossbar.m_8321_(false);
         data.bossbar = null;
      }

      if (server != null) {
         for (ServerPlayer player : server.m_6846_().m_11314_()) {
            performCleanup(player);
         }
      }

      if (data != null) {
         data.state = ExtractionPointState.IDLE;
         data.playersInside.clear();
         data.teamsInside.clear();
      }
   }

   public static void onPlayerDeathOrLogout(ServerPlayer player) {
      if (player != null) {
         performCleanup(player);
         if (data != null) {
            UUID uuid = player.m_20148_();
            data.playersInside.remove(uuid);
            if (uuid.equals(data.currentOwnerPlayerId)) {
               data.currentOwnerPlayerId = null;
               data.continuousOwnerCaptureTicks = 0;
            }

            if (uuid.equals(forcedOwnerPlayerId)) {
               forcedOwnerPlayerId = null;
            }
         }
      }
   }

   public static void onPlayerRespawn(ServerPlayer player) {
      if (player != null) {
         performCleanup(player);
         giveCompassToActiveParticipant(player);
      }
   }

   public static void giveCompassToActiveParticipant(ServerPlayer player) {
      if (player != null && data != null && data.state == ExtractionPointState.ACTIVE && config.navigatorEnabled) {
         if (isEligibleForExtraction(player)) {
            ExtractionCompassHelper.giveOrUpdate(player, data, Level.f_46428_);
         }
      }
   }

   public static boolean isActive() {
      return data != null && data.state == ExtractionPointState.ACTIVE;
   }

   public static ExtractionPointData getData() {
      return data;
   }

   public static ExtractionPointConfig getConfig(MinecraftServer server) {
      if (server != null) {
         config = ExtractionPointConfig.load(server);
      }

      return config;
   }

   public static long getMatchTimeSeconds(MinecraftServer server) {
      return server != null && matchStartTick > 0L ? Math.max(0L, (now(server) - matchStartTick) / 20L) : 0L;
   }

   public static boolean startRandom(CommandSourceStack source) {
      MinecraftServer server = source.m_81377_();
      config = ExtractionPointConfig.load(server);
      matchStartTick = matchStartTick <= 0L ? now(server) : matchStartTick;
      data = new ExtractionPointData();
      data.eventId = UUID.randomUUID();
      data.radius = config.captureRadius;
      data.halfHeight = effectiveCaptureHalfHeight();
      data.requiredCaptureTicks = config.requiredCaptureSeconds * 20;
      data.nextMilestoneRewardAtTicks = config.milestoneRewardIntervalSeconds * 20;
      data.expireAtMatchTick = matchStartTick + config.expireAtMatchTimeSeconds * 20L;
      return activateRandom(server);
   }

   public static boolean startAt(CommandSourceStack source, BlockPos pos) {
      MinecraftServer server = source.m_81377_();
      ServerLevel level = GameStateManager.getOverworld(server);
      if (server != null && level != null && pos != null) {
         config = ExtractionPointConfig.load(server);
         matchStartTick = matchStartTick <= 0L ? now(server) : matchStartTick;
         data = createActiveData(server, pos);
         startActive(server, level);
         return true;
      } else {
         return false;
      }
   }

   public static void stopExpired(MinecraftServer server) {
      finishExpired(server);
   }

   public static void setProgressSeconds(int seconds) {
      if (data != null) {
         data.globalCaptureProgressTicks = clamp(seconds * 20, 0, Math.max(1, data.requiredCaptureTicks));
      }
   }

   public static void addProgressSeconds(int seconds) {
      if (data != null) {
         data.globalCaptureProgressTicks = clamp(data.globalCaptureProgressTicks + seconds * 20, 0, Math.max(1, data.requiredCaptureTicks));
      }
   }

   public static void resetProgress() {
      if (data != null) {
         data.globalCaptureProgressTicks = 0;
         data.continuousOwnerCaptureTicks = 0;
         data.nextMilestoneRewardAtTicks = config.milestoneRewardIntervalSeconds * 20;
      }
   }

   public static void decayProgressSeconds(int seconds) {
      if (data != null) {
         data.globalCaptureProgressTicks = Math.max(0, data.globalCaptureProgressTicks - seconds * 20);
      }
   }

   public static void setForcedContested(Boolean value) {
      forcedContested = value;
   }

   public static void forceOwner(ServerPlayer player) {
      forcedOwnerPlayerId = player == null ? null : player.m_20148_();
      if (player != null && data != null) {
         data.currentOwnerPlayerId = player.m_20148_();
         data.currentOwnerTeamId = null;
      }
   }

   public static void clearOwner() {
      forcedOwnerPlayerId = null;
      if (data != null) {
         data.currentOwnerPlayerId = null;
         data.currentOwnerTeamId = null;
         data.continuousOwnerCaptureTicks = 0;
      }
   }

   public static void setDebugVisualMode(ExtractionPointVisualHelper.VisualMode mode) {
      debugVisualMode = mode;
   }

   public static void rewardMilestone(ServerPlayer player) {
      if (player != null) {
         reward(player, config.milestoneClassXp, config.milestoneCoins, false, false);
      }
   }

   public static void rewardFinal(ServerPlayer player) {
      if (player != null) {
         reward(player, config.finalClassXp, config.finalCoins, true, false);
      }
   }

   public static ExtractionPointManager.FindPositionResult findPosition(MinecraftServer server, int attempts) {
      ServerLevel level = GameStateManager.getOverworld(server);
      ExtractionPointManager.FindPositionResult result = new ExtractionPointManager.FindPositionResult();
      if (level == null) {
         return result;
      }

      WorldBorder border = level.m_6857_();
      int maxAttempts = Math.max(1, attempts);

      for (int index = 0; index < maxAttempts; index++) {
         BlockPos candidate = randomCandidate(level, border);
         ExtractionPointManager.Rejection rejection = validateCandidate(level, candidate, border);
         result.count(rejection);
         if (rejection == ExtractionPointManager.Rejection.ACCEPTED) {
            result.acceptedPos = candidate;
            break;
         }
      }

      return result;
   }

   public static double distanceToNearestBorderSide(ServerLevel level, BlockPos center) {
      if (level != null && center != null) {
         WorldBorder border = level.m_6857_();
         double halfSize = border.m_61959_() / 2.0;
         double dx = Math.abs(center.m_123341_() + 0.5 - border.m_6347_());
         double dz = Math.abs(center.m_123343_() + 0.5 - border.m_6345_());
         return halfSize - Math.max(dx, dz);
      } else {
         return 0.0;
      }
   }

   public static boolean willExpireByBorder(ServerLevel level) {
      return data != null && data.center != null ? distanceToNearestBorderSide(level, data.center) <= data.radius + config.borderSafetyMargin : false;
   }

   private static boolean activateRandom(MinecraftServer server) {
      ServerLevel level = GameStateManager.getOverworld(server);
      if (level == null) {
         return false;
      } else {
         ExtractionPointManager.FindPositionResult result = findPosition(server, config.maxLocationAttempts);
         if (result.acceptedPos == null) {
            finishExpired(server);
            return false;
         } else {
            data = createActiveData(server, result.acceptedPos);
            startActive(server, level);
            return true;
         }
      }
   }

   private static ExtractionPointData createActiveData(MinecraftServer server, BlockPos center) {
      ExtractionPointData active = new ExtractionPointData();
      active.eventId = UUID.randomUUID();
      active.center = center;
      active.radius = config.captureRadius;
      active.halfHeight = effectiveCaptureHalfHeight();
      active.state = ExtractionPointState.ACTIVE;
      active.activeStartTick = now(server);
      active.expireAtMatchTick = matchStartTick + config.expireAtMatchTimeSeconds * 20L;
      active.requiredCaptureTicks = config.requiredCaptureSeconds * 20;
      active.nextMilestoneRewardAtTicks = config.milestoneRewardIntervalSeconds * 20;
      return active;
   }

   private static void startActive(MinecraftServer server, ServerLevel level) {
      ensureBossbar(server);
      syncBossbarPlayers(server);
      if (config.navigatorEnabled) {
         for (ServerPlayer player : server.m_6846_().m_11314_()) {
            if (isEligibleForExtraction(player)) {
               ExtractionCompassHelper.giveOrUpdate(player, data, level.m_46472_());
            }
         }
      }

      broadcast(server, "[" + config.displayName + "] Сигнал активен. Найдите и удерживайте зону.");
      TacticalTabletMod.LOGGER.info("ExtractionPoint active: center={}, eventId={}", data.center, data.eventId);
   }

   private static void tickActive(MinecraftServer server) {
      ServerLevel level = GameStateManager.getOverworld(server);
      if (level != null && data.center != null) {
         if (now(server) < data.expireAtMatchTick && !willExpireByBorder(level)) {
            updatePlayersInside(server);
            updateCapture(server);
            updateBossbar(server);
            tickParticles(level);
            if (data.globalCaptureProgressTicks >= data.requiredCaptureTicks) {
               finishWinner(server, level);
            }
         } else {
            finishExpired(server);
         }
      }
   }

   private static void tickEnding(MinecraftServer server) {
      syncBossbarPlayers(server);
      ServerLevel level = GameStateManager.getOverworld(server);
      if (level != null) {
         tickParticles(level);
      }
   }

   private static void updatePlayersInside(MinecraftServer server) {
      data.playersInside.clear();
      data.teamsInside.clear();

      for (ServerPlayer player : server.m_6846_().m_11314_()) {
         if (isEligibleForExtraction(player) && isInside(player)) {
            data.playersInside.add(player.m_20148_());
            TeamId team = TeamMatchManager.getTeam(player);
            if (team != null) {
               data.teamsInside.add(team.name());
            }
         }
      }
   }

   private static void updateCapture(MinecraftServer server) {
      if (Boolean.TRUE.equals(forcedContested)) {
         markContested();
      } else {
         ServerPlayer forcedOwner = forcedOwnerPlayerId == null ? null : server.m_6846_().m_11259_(forcedOwnerPlayerId);
         if (forcedOwner != null && isEligibleForExtraction(forcedOwner)) {
            captureByPlayer(server, forcedOwner);
         } else {
            boolean teamMode = GameStateManager.getCurrentMode().isTeamMode();
            if (data.playersInside.isEmpty()) {
               markEmpty();
            } else if (teamMode) {
               if (data.teamsInside.size() != 1) {
                  markContested();
               } else {
                  captureByTeam(server, data.teamsInside.iterator().next());
               }
            } else if (data.playersInside.size() != 1) {
               markContested();
            } else {
               ServerPlayer owner = server.m_6846_().m_11259_(data.playersInside.iterator().next());
               if (owner == null) {
                  markEmpty();
               } else {
                  captureByPlayer(server, owner);
               }
            }
         }
      }
   }

   private static void captureByPlayer(MinecraftServer server, ServerPlayer owner) {
      boolean sameOwner = owner.m_20148_().equals(data.currentOwnerPlayerId) && data.currentOwnerTeamId == null;
      if (!sameOwner) {
         data.currentOwnerPlayerId = owner.m_20148_();
         data.currentOwnerTeamId = null;
         data.continuousOwnerCaptureTicks = 0;
         data.nextMilestoneRewardAtTicks = config.milestoneRewardIntervalSeconds * 20;
      }

      data.contested = false;
      data.globalCaptureProgressTicks = Math.min(data.requiredCaptureTicks, data.globalCaptureProgressTicks + 1);
      data.continuousOwnerCaptureTicks++;
      maybeRewardMilestone(List.of(owner));
   }

   private static void captureByTeam(MinecraftServer server, String teamName) {
      boolean sameOwner = teamName.equals(data.currentOwnerTeamId);
      if (!sameOwner) {
         data.currentOwnerPlayerId = null;
         data.currentOwnerTeamId = teamName;
         data.continuousOwnerCaptureTicks = 0;
         data.nextMilestoneRewardAtTicks = config.milestoneRewardIntervalSeconds * 20;
      }

      data.contested = false;
      data.globalCaptureProgressTicks = Math.min(data.requiredCaptureTicks, data.globalCaptureProgressTicks + 1);
      data.continuousOwnerCaptureTicks++;
      maybeRewardMilestone(playersInsideForTeam(server, teamName));
   }

   private static void markContested() {
      data.contested = true;
      data.continuousOwnerCaptureTicks = 0;
      data.nextMilestoneRewardAtTicks = config.milestoneRewardIntervalSeconds * 20;
   }

   private static void markEmpty() {
      data.contested = false;
      data.currentOwnerPlayerId = null;
      data.currentOwnerTeamId = null;
      data.continuousOwnerCaptureTicks = 0;
      data.nextMilestoneRewardAtTicks = config.milestoneRewardIntervalSeconds * 20;
      decayRemainderTicks = decayRemainderTicks + config.progressDecayPerSecond;
      int decayTicks = (int)decayRemainderTicks;
      decayRemainderTicks -= decayTicks;
      data.globalCaptureProgressTicks = Math.max(0, data.globalCaptureProgressTicks - decayTicks);
   }

   private static void maybeRewardMilestone(List<ServerPlayer> players) {
      if (data.continuousOwnerCaptureTicks >= data.nextMilestoneRewardAtTicks) {
         for (ServerPlayer player : players) {
            reward(player, config.milestoneClassXp, config.milestoneCoins, false, false);
         }

         data.nextMilestoneRewardAtTicks = data.nextMilestoneRewardAtTicks + config.milestoneRewardIntervalSeconds * 20;
      }
   }

   private static void finishWinner(MinecraftServer server, ServerLevel level) {
      for (ServerPlayer player : finalRewardPlayers(server)) {
         reward(player, config.finalClassXp, config.finalCoins, true, data.currentOwnerTeamId != null);
      }

      data.state = ExtractionPointState.ENDING_WINNER;
      data.endingUntilTick = now(server) + config.winnerBossbarSeconds * 20L;
      updateBossbarWinner(server);
      ExtractionPointVisualHelper.playCaptured(level, data.center);

      for (ServerPlayer player : server.m_6846_().m_11314_()) {
         performCleanup(player);
      }
   }

   private static void finishExpired(MinecraftServer server) {
      if (server != null && data != null && data.state != ExtractionPointState.IDLE) {
         data.state = ExtractionPointState.ENDING_EXPIRED;
         data.endingUntilTick = now(server) + config.endingFadeSeconds * 20L;
         ensureBossbar(server);
         data.bossbar.m_6456_(Component.m_237113_("Сигнал бизнес-точки потерян"));
         data.bossbar.m_6451_(BossBarColor.WHITE);
         data.bossbar.m_142711_(Math.max(0.0F, progress()));
         syncBossbarPlayers(server);

         for (ServerPlayer player : server.m_6846_().m_11314_()) {
            performCleanup(player);
         }
      }
   }

   private static List<ServerPlayer> finalRewardPlayers(MinecraftServer server) {
      if (data.currentOwnerTeamId != null) {
         return playersInsideForTeam(server, data.currentOwnerTeamId);
      }

      ServerPlayer player = data.currentOwnerPlayerId == null ? null : server.m_6846_().m_11259_(data.currentOwnerPlayerId);
      return player != null && isEligibleForExtraction(player) ? List.of(player) : List.of();
   }

   private static List<ServerPlayer> playersInsideForTeam(MinecraftServer server, String teamName) {
      List<ServerPlayer> players = new ArrayList<>();
      if (teamName == null) {
         return players;
      }

      for (UUID uuid : data.playersInside) {
         ServerPlayer player = server.m_6846_().m_11259_(uuid);
         if (player != null && isEligibleForExtraction(player)) {
            TeamId team = TeamMatchManager.getTeam(player);
            if (team != null && team.name().equals(teamName)) {
               players.add(player);
            }
         }
      }

      return players;
   }

   private static void reward(ServerPlayer player, int classXp, int coins, boolean finisher, boolean teamReward) {
      if (player != null) {
         if (coins > 0) {
            PlayerProgressManager.addCoins(player, coins);
         }

         String clazz = PlayerTabletState.getSelectedClass(player);
         if (clazz != null && !clazz.isBlank() && classXp > 0) {
            classXp = ClassXPManager.addXP(player, clazz, classXp);
            XpNotifier.send(player, classXp, config.displayName);
         } else {
            ClassXPManager.sync(player);
         }

         PlayerProgressManager.savePlayer(player);
         if (finisher) {
            String text = teamReward
               ? "[" + config.displayName + "] Ваша команда захватила зону: +" + classXp + " XP класса, +" + coins + " монет."
               : "[" + config.displayName + "] Вы захватили зону: +" + classXp + " XP класса, +" + coins + " монет.";
            player.m_213846_(Component.m_237113_(text).m_130940_(ChatFormatting.GOLD));
         } else {
            player.f_8906_
               .m_9829_(
                  new ClientboundSetActionBarTextPacket(
                     Component.m_237113_("+" + classXp + " XP класса, +" + coins + " монеты за удержание").m_130940_(ChatFormatting.GOLD)
                  )
               );
         }
      }
   }

   private static void ensureBossbar(MinecraftServer server) {
      if (data.bossbar == null) {
         data.bossbar = new ServerBossEvent(Component.m_237113_(config.displayName + " активна"), BossBarColor.YELLOW, BossBarOverlay.PROGRESS);
      }

      data.bossbar.m_8321_(true);
      syncBossbarPlayers(server);
   }

   private static void syncBossbarPlayers(MinecraftServer server) {
      if (server != null && data != null && data.bossbar != null) {
         for (ServerPlayer player : server.m_6846_().m_11314_()) {
            if (isEligibleForExtraction(player)) {
               data.bossbar.m_6543_(player);
            } else {
               data.bossbar.m_6539_(player);
            }
         }
      }
   }

   private static void updateBossbar(MinecraftServer server) {
      ensureBossbar(server);
      data.bossbar.m_142711_(progress());
      if (data.contested) {
         data.bossbar.m_6456_(Component.m_237113_("Зона оспаривается"));
         data.bossbar.m_6451_(BossBarColor.RED);
      } else if (data.playersInside.isEmpty()) {
         data.bossbar.m_6456_(Component.m_237113_("бизнес-точка свободна — прогресс снижается"));
         data.bossbar.m_6451_(BossBarColor.YELLOW);
      } else {
         int progressSeconds = data.globalCaptureProgressTicks / 20;
         int requiredSeconds = Math.max(1, data.requiredCaptureTicks / 20);
         if (data.currentOwnerTeamId != null) {
            data.bossbar.m_6456_(Component.m_237113_("Захват: Команда " + data.currentOwnerTeamId + " — " + progressSeconds + "/" + requiredSeconds + " сек"));
         } else {
            ServerPlayer owner = data.currentOwnerPlayerId == null ? null : server.m_6846_().m_11259_(data.currentOwnerPlayerId);
            String name = owner == null ? "-" : owner.m_7755_().getString();
            data.bossbar.m_6456_(Component.m_237113_("Захват: " + name + " — " + progressSeconds + "/" + requiredSeconds + " сек"));
         }

         data.bossbar.m_6451_(BossBarColor.GREEN);
      }
   }

   private static void updateBossbarWinner(MinecraftServer server) {
      ensureBossbar(server);
      data.bossbar.m_6451_(BossBarColor.GREEN);
      data.bossbar.m_142711_(1.0F);
      if (data.currentOwnerTeamId != null) {
         data.bossbar.m_6456_(Component.m_237113_("бизнес-точка захвачена: Команда " + data.currentOwnerTeamId));
      } else {
         ServerPlayer owner = data.currentOwnerPlayerId == null ? null : server.m_6846_().m_11259_(data.currentOwnerPlayerId);
         data.bossbar.m_6456_(Component.m_237113_("бизнес-точка захвачена: " + (owner == null ? "-" : owner.m_7755_().getString())));
      }
   }

   private static void tickParticles(ServerLevel level) {
      if (++particleTicker >= config.particleUpdateIntervalTicks) {
         particleTicker = 0;
         ExtractionPointVisualHelper.VisualMode mode = debugVisualMode;
         if (mode == null) {
            if (data.state == ExtractionPointState.ENDING_EXPIRED) {
               mode = ExtractionPointVisualHelper.VisualMode.ENDING;
            } else if (data.state == ExtractionPointState.ENDING_WINNER) {
               mode = ExtractionPointVisualHelper.VisualMode.CAPTURED;
            } else if (data.contested) {
               mode = ExtractionPointVisualHelper.VisualMode.CONTESTED;
            } else if (!data.playersInside.isEmpty()) {
               mode = ExtractionPointVisualHelper.VisualMode.CAPTURING;
            } else {
               mode = ExtractionPointVisualHelper.VisualMode.NORMAL;
            }
         }

         ExtractionPointVisualHelper.spawnRing(level, data, config, mode);
      }
   }

   private static BlockPos randomCandidate(ServerLevel level, WorldBorder border) {
      double angle = RANDOM.nextDouble() * Math.PI * 2.0;
      double distance = RANDOM.nextDouble() * config.centerOffsetRadius;
      int x = (int)Math.round(border.m_6347_() + Math.cos(angle) * distance);
      int z = (int)Math.round(border.m_6345_() + Math.sin(angle) * distance);
      int y = level.m_6924_(Types.MOTION_BLOCKING_NO_LEAVES, x, z);
      return new BlockPos(x, y, z);
   }

   private static ExtractionPointManager.Rejection validateCandidate(ServerLevel level, BlockPos pos, WorldBorder border) {
      if (pos.m_123342_() < config.minEventY) {
         return ExtractionPointManager.Rejection.REJECTED_Y_TOO_LOW;
      }

      if (pos.m_123342_() > config.maxEventY) {
         return ExtractionPointManager.Rejection.REJECTED_Y_TOO_HIGH;
      }

      if (distanceToNearestBorderSide(level, pos) <= config.captureRadius + config.borderSafetyMargin) {
         return ExtractionPointManager.Rejection.REJECTED_BORDER;
      }

      BlockState at = level.m_8055_(pos);
      BlockState below = level.m_8055_(pos.m_7495_());
      if (config.blockedLiquids) {
         if (at.m_60819_().m_205070_(FluidTags.f_13131_) || below.m_60819_().m_205070_(FluidTags.f_13131_)) {
            return ExtractionPointManager.Rejection.REJECTED_WATER;
         }

         if (at.m_60819_().m_205070_(FluidTags.f_13132_) || below.m_60819_().m_205070_(FluidTags.f_13132_)) {
            return ExtractionPointManager.Rejection.REJECTED_LAVA;
         }
      }

      return ExtractionPointManager.Rejection.ACCEPTED;
   }

   private static boolean isEligibleForExtraction(ServerPlayer player) {
      return player != null
         && player.m_6084_()
         && player.m_9236_().m_46472_().equals(Level.f_46428_)
         && player.m_19880_().contains("war.playing")
         && LivesManager.isAliveParticipant(player)
         && player.f_8941_.m_9290_() != GameType.SPECTATOR;
   }

   private static void performCleanup(ServerPlayer player) {
      ExtractionCompassHelper.removeAllExtractionCompasses(player);
   }

   private static boolean isInside(ServerPlayer player) {
      Vec3 pos = player.m_20182_();
      double dx = pos.f_82479_ - (data.center.m_123341_() + 0.5);
      double dz = pos.f_82481_ - (data.center.m_123343_() + 0.5);
      double horizontalSq = dx * dx + dz * dz;
      return horizontalSq <= data.radius * data.radius && Math.abs(pos.f_82480_ - data.center.m_123342_()) <= data.halfHeight;
   }

   private static double effectiveCaptureHalfHeight() {
      return Math.max(24.0, config.captureHalfHeight);
   }

   private static float progress() {
      return data != null && data.requiredCaptureTicks > 0
         ? Math.max(0.0F, Math.min(1.0F, (float)data.globalCaptureProgressTicks / data.requiredCaptureTicks))
         : 0.0F;
   }

   private static long now(MinecraftServer server) {
      return server == null ? 0L : server.m_129921_();
   }

   private static void broadcast(MinecraftServer server, String message) {
      if (server != null) {
         for (ServerPlayer player : server.m_6846_().m_11314_()) {
            player.m_213846_(Component.m_237113_(message));
         }
      }
   }

   private static int clamp(int value, int min, int max) {
      return Math.max(min, Math.min(max, value));
   }

   public static class FindPositionResult {
      public int rejectedWater;
      public int rejectedLava;
      public int rejectedYTooLow;
      public int rejectedYTooHigh;
      public int rejectedBorder;
      public int accepted;
      public BlockPos acceptedPos;

      private void count(ExtractionPointManager.Rejection rejection) {
         switch (rejection) {
            case REJECTED_WATER:
               this.rejectedWater++;
               break;
            case REJECTED_LAVA:
               this.rejectedLava++;
               break;
            case REJECTED_Y_TOO_LOW:
               this.rejectedYTooLow++;
               break;
            case REJECTED_Y_TOO_HIGH:
               this.rejectedYTooHigh++;
               break;
            case REJECTED_BORDER:
               this.rejectedBorder++;
               break;
            case ACCEPTED:
               this.accepted++;
         }
      }
   }

   private enum Rejection {
      REJECTED_WATER,
      REJECTED_LAVA,
      REJECTED_Y_TOO_LOW,
      REJECTED_Y_TOO_HIGH,
      REJECTED_BORDER,
      ACCEPTED;
   }
}
