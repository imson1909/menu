package com.makar.tacticaltablet.airdrop;

import com.makar.tacticaltablet.airdrop.loot.AirdropLootGenerator;
import com.makar.tacticaltablet.airdrop.net.AirdropSmokeStatePacket;
import com.makar.tacticaltablet.core.ModBlocks;
import com.makar.tacticaltablet.core.ModItems;
import com.makar.tacticaltablet.core.ModSounds;
import com.makar.tacticaltablet.core.TacticalTabletMod;
import com.makar.tacticaltablet.game.GameStateManager;
import com.makar.tacticaltablet.game.lives.LivesManager;
import com.makar.tacticaltablet.integration.discord.DiscordLeaderboardService;
import com.makar.tacticaltablet.tablet.net.PacketHandler;
import java.util.Locale;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.level.levelgen.Heightmap.Types;
import net.minecraft.world.phys.Vec3;

public final class AirdropManager {
   private static final int FIRST_AUTO_SPAWN_DELAY_TICKS = 2400;
   private static final int AUTO_SPAWN_INTERVAL_TICKS = 6000;
   private static final int ANNOUNCE_DELAY_TICKS = 1200;
   private static final int OPENED_DURATION_TICKS = 2400;
   private static final int LANDED_EXPIRE_TICKS = 6000;
   private static final int FIND_ATTEMPTS = 50;
   private static final double FALL_START_HEIGHT = 100.0;
   private static final double FALL_SPEED_PER_TICK = 0.15;
   private static final double MIN_RADIUS_FACTOR = 0.18;
   private static final double MAX_RADIUS_FACTOR = 0.75;
   private static final boolean REMOVE_CHEST_ON_EXPIRE = false;
   private static AirdropData activeAirdrop;
   private static int autoSpawnTicker = 0;
   private static int nextAutoSpawnDelayTicks = 2400;

   private AirdropManager() {
   }

   public static boolean hasActiveAirdrop() {
      return activeAirdrop != null && activeAirdrop.state != AirdropState.NONE && activeAirdrop.state != AirdropState.EXPIRED;
   }

   public static AirdropData getActiveAirdrop() {
      return activeAirdrop;
   }

   public static void start(ServerLevel level, boolean instant) {
      if (level != null) {
         if (hasActiveAirdrop()) {
            TacticalTabletMod.LOGGER.warn("Tactical Tablet AirDrop start skipped: another AirDrop is already active.");
         } else if (level.m_46472_().equals(GameStateManager.LOBBY_DIMENSION)) {
            TacticalTabletMod.LOGGER.warn("Tactical Tablet AirDrop start skipped: cannot start in lobby dimension.");
         } else {
            BlockPos realDropPos = findSafeDropPos(level);
            if (realDropPos == null) {
               TacticalTabletMod.LOGGER.warn("Tactical Tablet AirDrop start cancelled: no safe drop point found.");
            } else {
               BlockPos compassTarget = createCompassTarget(level, realDropPos);
               activeAirdrop = new AirdropData(
                  UUID.randomUUID(),
                  instant ? AirdropState.FALLING : AirdropState.ANNOUNCED,
                  level.m_46472_(),
                  realDropPos,
                  compassTarget,
                  instant ? 0 : 1200,
                  realDropPos.m_123342_() + 100.0
               );
               announceStart(level);
               giveOrUpdateCompasses(level);
               if (GameStateManager.isRunning(level.m_7654_())) {
                  DiscordLeaderboardService.recordMatchAirdropStarted();
               }

               if (instant) {
                  broadcast(level, "§c[СБРОС] §fГруз уже в пути.");
                  spawnFallingCrate(level);
               }

               TacticalTabletMod.LOGGER
                  .info(
                     "Tactical Tablet AirDrop started: id={}, state={}, real={}, compass={}",
                     new Object[]{activeAirdrop.id, activeAirdrop.state, formatPos(activeAirdrop.realDropPos), formatPos(activeAirdrop.compassTargetPos)}
                  );
            }
         }
      }
   }

   public static void cancel(ServerLevel level) {
      if (activeAirdrop != null) {
         ServerLevel activeLevel = resolveActiveLevel(level);
         finish(activeLevel == null ? level : activeLevel);
      }
   }

   public static void serverTick(ServerLevel level) {
      if (level != null) {
         tickAutoSpawner(level);
         if (activeAirdrop != null) {
            if (level.m_46472_().equals(activeAirdrop.dimension)) {
               switch (activeAirdrop.state) {
                  case ANNOUNCED:
                     tickAnnounced(level);
                     break;
                  case FALLING:
                     tickFalling(level);
                     break;
                  case LANDED:
                     tickLanded(level);
                     break;
                  case OPENED:
                     tickOpened(level);
               }
            }
         }
      }
   }

   public static void onChestInteract(ServerPlayer player, BlockPos pos) {
      if (player != null && pos != null && activeAirdrop != null) {
         if (activeAirdrop.state == AirdropState.LANDED) {
            if (activeAirdrop.chestPos != null && activeAirdrop.chestPos.equals(pos)) {
               activeAirdrop.state = AirdropState.OPENED;
               activeAirdrop.opened = true;
               activeAirdrop.greenSmoke = true;
               activeAirdrop.openedBy = player.m_20148_();
               activeAirdrop.ticksSinceOpened = 0;
               TacticalTabletMod.LOGGER
                  .info(
                     "Tactical Tablet AirDrop opened: id={}, player={}, chest={}",
                     new Object[]{activeAirdrop.id, player.m_6302_(), formatPos(activeAirdrop.chestPos)}
                  );
            }
         }
      }
   }

   public static boolean isAirdropChest(BlockPos pos) {
      return activeAirdrop != null
         && activeAirdrop.chestPos != null
         && activeAirdrop.chestPos.equals(pos)
         && (activeAirdrop.state == AirdropState.LANDED || activeAirdrop.state == AirdropState.OPENED);
   }

   public static boolean isOrphanedVisualEntity(Entity entity) {
      if (entity instanceof ArmorStand stand) {
         return !stand.m_6844_(EquipmentSlot.HEAD).m_150930_((Item)ModItems.AIRDROP_CRATE_FLYING.get())
            ? false
            : activeAirdrop == null
               || activeAirdrop.state != AirdropState.FALLING
               || activeAirdrop.visualEntityId == null
               || !activeAirdrop.visualEntityId.equals(stand.m_20148_());
      } else {
         return false;
      }
   }

   public static void finish(ServerLevel level) {
      if (activeAirdrop != null) {
         activeAirdrop.state = AirdropState.EXPIRED;
         ServerLevel activeLevel = resolveActiveLevel(level);
         if (activeLevel != null) {
            sendSmokeState(activeLevel, false);
            removeVisualEntity(activeLevel);
            removeAirdropCompasses(activeLevel);
            broadcast(activeLevel, "§7[СБРОС] Событие завершено.");
         }

         TacticalTabletMod.LOGGER.info("Tactical Tablet AirDrop finished.");
         activeAirdrop = null;
      }
   }

   public static void giveCompassToJoiningPlayer(ServerPlayer player) {
      if (player != null && activeAirdrop != null) {
         if (activeAirdrop.chestPos != null && (activeAirdrop.state == AirdropState.LANDED || activeAirdrop.state == AirdropState.OPENED)) {
            PacketHandler.sendToPlayer(player, createSmokePacket(true));
         }

         if (isEligibleForCompass(player)) {
            if (activeAirdrop.state == AirdropState.ANNOUNCED
               || activeAirdrop.state == AirdropState.FALLING
               || activeAirdrop.state == AirdropState.LANDED
               || activeAirdrop.state == AirdropState.OPENED) {
               AirdropCompassHelper.giveOrUpdate(player, activeAirdrop);
            }
         }
      }
   }

   public static void resetAutoScheduler() {
      autoSpawnTicker = 0;
      nextAutoSpawnDelayTicks = 2400;
   }

   public static void resetRuntime(ServerLevel level) {
      ServerLevel activeLevel = resolveActiveLevel(level);
      if (activeLevel != null) {
         sendSmokeState(activeLevel, false);
         removeVisualEntity(activeLevel);
         removeAirdropCompasses(activeLevel);
      }

      activeAirdrop = null;
      autoSpawnTicker = 0;
      nextAutoSpawnDelayTicks = 2400;
   }

   private static void tickAutoSpawner(ServerLevel level) {
      if (!GameStateManager.isRunning(level.m_7654_())) {
         autoSpawnTicker = 0;
      } else if (!hasActiveAirdrop()) {
         autoSpawnTicker++;
         if (autoSpawnTicker >= nextAutoSpawnDelayTicks) {
            autoSpawnTicker = 0;
            nextAutoSpawnDelayTicks = 6000;
            start(level, false);
         }
      }
   }

   private static void tickAnnounced(ServerLevel level) {
      activeAirdrop.ticksUntilDrop--;
      if (activeAirdrop.ticksUntilDrop == 600) {
         broadcast(level, "§c[СБРОС] §fДо сброса: 30 сек.");
      }

      if (activeAirdrop.ticksUntilDrop == 200) {
         sendActionBar(level, "§c[СБРОС] §fГруз приближается.");
      }

      if (activeAirdrop.ticksUntilDrop <= 0) {
         activeAirdrop.state = AirdropState.FALLING;
         spawnFallingCrate(level);
      }
   }

   private static void tickFalling(ServerLevel level) {
      Entity visual = getVisualEntity(level);
      if (visual == null) {
         spawnFallingCrate(level);
         visual = getVisualEntity(level);
      }

      activeAirdrop.currentCrateY -= 0.15;
      double x = activeAirdrop.realDropPos.m_123341_() + 0.5;
      double y = activeAirdrop.currentCrateY;
      double z = activeAirdrop.realDropPos.m_123343_() + 0.5;
      if (visual != null) {
         visual.m_6021_(x, y, z);
      }

      if (activeAirdrop.currentCrateY <= activeAirdrop.realDropPos.m_123342_() + 1.0) {
         landAirdrop(level);
      }
   }

   private static void tickLanded(ServerLevel level) {
      activeAirdrop.ticksSinceLanded++;
      if (activeAirdrop.ticksSinceLanded >= 6000) {
         finish(level);
      }
   }

   private static void tickOpened(ServerLevel level) {
      activeAirdrop.ticksSinceOpened++;
      if (activeAirdrop.ticksSinceOpened >= 2400) {
         finish(level);
      }
   }

   private static BlockPos findSafeDropPos(ServerLevel level) {
      Vec3 center = getSafeZoneCenter(level);
      double radius = getSafeZoneRadius(level);
      if (radius <= 8.0) {
         return null;
      }

      double minDistance = Math.min(radius * 0.18, Math.max(0.0, radius - 4.0));
      double maxDistance = Math.max(minDistance + 1.0, radius * 0.75);

      for (int attempt = 0; attempt < 50; attempt++) {
         double angle = level.f_46441_.m_188500_() * Math.PI * 2.0;
         double distance = minDistance + level.f_46441_.m_188500_() * (maxDistance - minDistance);
         int x = (int)Math.round(center.f_82479_ + Math.cos(angle) * distance);
         int z = (int)Math.round(center.f_82481_ + Math.sin(angle) * distance);
         BlockPos surface = level.m_5452_(Types.MOTION_BLOCKING_NO_LEAVES, new BlockPos(x, 0, z));
         if (isSafeDropPosition(level, surface, attempt < 40)) {
            return surface;
         }
      }

      return null;
   }

   private static BlockPos createCompassTarget(ServerLevel level, BlockPos realPos) {
      Vec3 center = getSafeZoneCenter(level);
      double radius = getSafeZoneRadius(level);

      for (int attempt = 0; attempt < 30; attempt++) {
         double angle = level.f_46441_.m_188500_() * Math.PI * 2.0;
         double offset = 25.0 + level.f_46441_.m_188500_() * 25.0;
         int x = (int)Math.round(realPos.m_123341_() + Math.cos(angle) * offset);
         int z = (int)Math.round(realPos.m_123343_() + Math.sin(angle) * offset);
         double distanceToCenter = Math.hypot(x - center.f_82479_, z - center.f_82481_);
         if (!(distanceToCenter > radius * 0.9)) {
            BlockPos target = level.m_5452_(Types.MOTION_BLOCKING_NO_LEAVES, new BlockPos(x, 0, z));
            if (level.m_6857_().m_61937_(target)) {
               return target;
            }
         }
      }

      return realPos;
   }

   private static Vec3 getSafeZoneCenter(ServerLevel level) {
      WorldBorder border = level.m_6857_();
      return new Vec3(border.m_6347_(), 0.0, border.m_6345_());
   }

   private static double getSafeZoneRadius(ServerLevel level) {
      return level.m_6857_().m_61959_() / 2.0;
   }

   private static boolean isSafeDropPosition(ServerLevel level, BlockPos pos, boolean avoidLeaves) {
      if (pos == null) {
         return false;
      } else if (level.m_46472_().equals(GameStateManager.LOBBY_DIMENSION)) {
         return false;
      } else if (!level.m_6857_().m_61937_(pos)) {
         return false;
      } else if (!level.m_46859_(pos)) {
         return false;
      } else if (!level.m_46859_(pos.m_7494_())) {
         return false;
      } else {
         BlockPos belowPos = pos.m_7495_();
         BlockState below = level.m_8055_(belowPos);
         BlockState at = level.m_8055_(pos);
         if (!below.m_60783_(level, belowPos, Direction.UP)) {
            return false;
         } else if (avoidLeaves && below.m_204336_(BlockTags.f_13035_)) {
            return false;
         } else {
            return below.m_60819_().m_205070_(FluidTags.f_13131_) || below.m_60819_().m_205070_(FluidTags.f_13132_)
               ? false
               : !at.m_60819_().m_205070_(FluidTags.f_13131_) && !at.m_60819_().m_205070_(FluidTags.f_13132_);
         }
      }
   }

   private static void announceStart(ServerLevel level) {
      sendTitle(level, "§cСБРОС", "§fГруз будет сброшен через 60 секунд");
      broadcast(level, "§c[СБРОС] §fКомпас указывает в примерную зону сброса.");
   }

   private static void giveOrUpdateCompasses(ServerLevel level) {
      for (ServerPlayer player : level.m_7654_().m_6846_().m_11314_()) {
         if (isEligibleForCompass(player)) {
            AirdropCompassHelper.giveOrUpdate(player, activeAirdrop);
         }
      }
   }

   private static boolean isEligibleForCompass(ServerPlayer player) {
      if (player == null) {
         return false;
      }

      if (LivesManager.isEliminated(player)) {
         return false;
      }

      boolean inBattle = player.m_19880_().contains("war.playing");
      boolean waitingForRtp = player.m_19880_().contains("in_lobby");
      return (inBattle || waitingForRtp) && LivesManager.canContinueMatch(player);
   }

   private static void removeAirdropCompasses(ServerLevel level) {
      for (ServerPlayer player : level.m_7654_().m_6846_().m_11314_()) {
         AirdropCompassHelper.removeAllAirdropCompasses(player);
      }
   }

   private static void spawnFallingCrate(ServerLevel level) {
      BlockPos visualPos = BlockPos.m_274561_(
         activeAirdrop.realDropPos.m_123341_() + 0.5, activeAirdrop.currentCrateY, activeAirdrop.realDropPos.m_123343_() + 0.5
      );
      if (level.m_143340_(visualPos)) {
         removeVisualEntity(level);
         AirdropVisualArmorStand stand = new AirdropVisualArmorStand(
            level, activeAirdrop.realDropPos.m_123341_() + 0.5, activeAirdrop.currentCrateY, activeAirdrop.realDropPos.m_123343_() + 0.5
         );
         stand.m_6842_(true);
         stand.m_20242_(true);
         stand.m_20331_(true);
         stand.m_20225_(true);
         stand.m_8061_(EquipmentSlot.HEAD, new ItemStack((ItemLike)ModItems.AIRDROP_CRATE_FLYING.get()));
         activeAirdrop.visualEntityId = stand.m_20148_();
         if (!level.m_7967_(stand)) {
            activeAirdrop.visualEntityId = null;
         }
      }
   }

   private static void landAirdrop(ServerLevel level) {
      removeVisualEntity(level);
      BlockPos chestPos = findChestPlacement(level, activeAirdrop.realDropPos);
      if (chestPos == null) {
         TacticalTabletMod.LOGGER.warn("Tactical Tablet AirDrop could not place chest at {}", formatPos(activeAirdrop.realDropPos));
         finish(level);
      } else {
         level.m_7731_(chestPos, ((Block)ModBlocks.AIRDROP_CRATE.get()).m_49966_(), 3);
         activeAirdrop.chestPos = chestPos;
         activeAirdrop.state = AirdropState.LANDED;
         activeAirdrop.ticksSinceLanded = 0;
         AirdropLootGenerator.fillChest(level, chestPos);
         sendSmokeState(level, true);
         level.m_5594_(null, chestPos, (SoundEvent)ModSounds.PARACHUTE_CLOSE.get(), SoundSource.BLOCKS, 1.0F, 1.0F);
         broadcast(level, "§c[СБРОС] §fГруз приземлился. Следуйте по дыму.");
         TacticalTabletMod.LOGGER.info("Tactical Tablet AirDrop landed at {}", formatPos(chestPos));
      }
   }

   private static void sendSmokeState(ServerLevel level, boolean active) {
      if (level != null && activeAirdrop != null && activeAirdrop.dimension != null) {
         AirdropSmokeStatePacket packet = createSmokePacket(active);

         for (ServerPlayer player : level.m_7654_().m_6846_().m_11314_()) {
            PacketHandler.sendToPlayer(player, packet);
         }
      }
   }

   private static AirdropSmokeStatePacket createSmokePacket(boolean active) {
      BlockPos smokePos = activeAirdrop.chestPos != null ? activeAirdrop.chestPos : activeAirdrop.realDropPos;
      return new AirdropSmokeStatePacket(active, activeAirdrop.dimension.m_135782_(), smokePos);
   }

   private static BlockPos findChestPlacement(ServerLevel level, BlockPos origin) {
      BlockPos[] candidates = new BlockPos[]{origin, origin.m_7494_(), origin.m_122012_(), origin.m_122019_(), origin.m_122029_(), origin.m_122024_()};

      for (BlockPos candidate : candidates) {
         if (level.m_46859_(candidate)
            && level.m_46859_(candidate.m_7494_())
            && level.m_8055_(candidate.m_7495_()).m_60783_(level, candidate.m_7495_(), Direction.UP)) {
            return candidate;
         }
      }

      return null;
   }

   private static Entity getVisualEntity(ServerLevel level) {
      return activeAirdrop != null && activeAirdrop.visualEntityId != null ? level.m_8791_(activeAirdrop.visualEntityId) : null;
   }

   private static void removeVisualEntity(ServerLevel level) {
      Entity visual = getVisualEntity(level);
      if (visual != null) {
         visual.m_146870_();
      }

      if (activeAirdrop != null) {
         activeAirdrop.visualEntityId = null;
      }
   }

   private static ServerLevel resolveActiveLevel(ServerLevel fallback) {
      if (activeAirdrop == null) {
         return fallback;
      } else if (fallback != null && fallback.m_7654_() != null) {
         ServerLevel activeLevel = fallback.m_7654_().m_129880_(activeAirdrop.dimension);
         return activeLevel == null ? fallback : activeLevel;
      } else {
         return fallback;
      }
   }

   private static void broadcast(ServerLevel level, String message) {
      Component component = Component.m_237113_(message);

      for (ServerPlayer player : level.m_7654_().m_6846_().m_11314_()) {
         player.m_213846_(component);
      }
   }

   private static void sendTitle(ServerLevel level, String title, String subtitle) {
      Component titleComponent = Component.m_237113_(title);
      Component subtitleComponent = Component.m_237113_(subtitle);

      for (ServerPlayer player : level.m_7654_().m_6846_().m_11314_()) {
         player.f_8906_.m_9829_(new ClientboundSetTitlesAnimationPacket(10, 70, 20));
         player.f_8906_.m_9829_(new ClientboundSetTitleTextPacket(titleComponent));
         player.f_8906_.m_9829_(new ClientboundSetSubtitleTextPacket(subtitleComponent));
      }
   }

   private static void sendActionBar(ServerLevel level, String message) {
      Component component = Component.m_237113_(message);

      for (ServerPlayer player : level.m_7654_().m_6846_().m_11314_()) {
         player.f_8906_.m_9829_(new ClientboundSetActionBarTextPacket(component));
      }
   }

   private static String formatPos(BlockPos pos) {
      return pos == null ? "-" : String.format(Locale.ROOT, "%d %d %d", pos.m_123341_(), pos.m_123342_(), pos.m_123343_());
   }
}
