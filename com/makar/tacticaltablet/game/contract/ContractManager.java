package com.makar.tacticaltablet.game.contract;

import com.makar.tacticaltablet.core.ModItems;
import com.makar.tacticaltablet.game.GameStateManager;
import com.makar.tacticaltablet.game.MatchPhase;
import com.makar.tacticaltablet.game.lives.LivesManager;
import com.makar.tacticaltablet.game.team.TeamMatchManager;
import com.makar.tacticaltablet.inventory.InventoryManager;
import com.makar.tacticaltablet.progression.PlayerProgressManager;
import com.makar.tacticaltablet.tablet.PlayerTabletState;
import com.makar.tacticaltablet.tablet.net.ContractSelectionStatePacket;
import com.makar.tacticaltablet.tablet.net.ContractTrackerStatePacket;
import com.makar.tacticaltablet.tablet.net.PacketHandler;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.level.levelgen.Heightmap.Types;

public final class ContractManager {
   public static final int SELECTION_SECONDS = 60;
   public static final int MIN_PLAYERS = 3;
   public static final int SIGNAL_SECONDS = 15;
   public static final int TARGET_AREA_RADIUS = 25;
   private static final int MAX_SELECTION_TARGETS = 16;
   private static final int ZONE_GRID_SIZE = 50;
   private static final int TARGET_AREA_OFFSET = 20;
   private static final int TRACKER_COOLDOWN_SECONDS = 3;
   private static final long TRACKER_COOLDOWN_MS = 3000L;
   private static final Random RANDOM = new Random();
   private static final Map<UUID, ContractManager.Contract> contractsByOwner = new HashMap<>();
   private static final Map<UUID, Set<UUID>> targetToOwners = new HashMap<>();
   private static final Map<UUID, Long> pickCooldownUntil = new HashMap<>();
   private static final Set<UUID> debugArmorStands = new HashSet<>();
   private static final Set<UUID> trackerViewers = ConcurrentHashMap.newKeySet();
   private static int selectionSecondsLeft = 0;
   private static int signalSecondsLeft = 15;
   private static boolean selectionActive = false;
   private static boolean soloDebugEnabled = false;
   private static int tickCounter = 0;

   private ContractManager() {
   }

   public static void onMatchStart(MinecraftServer server) {
      clearContracts();
      pickCooldownUntil.clear();
      trackerViewers.clear();
      signalSecondsLeft = 15;
      tickCounter = 0;
      boolean enabled = server != null && (GameStateManager.onlinePlayers(server) >= 3 || soloDebugEnabled);
      selectionActive = enabled;
      selectionSecondsLeft = enabled ? 60 : 0;
   }

   public static void tick(MinecraftServer server) {
      if (server != null && GameStateManager.isRunning(server)) {
         if (GameStateManager.getMatchPhase() == MatchPhase.RUNNING) {
            if (++tickCounter >= 20) {
               tickCounter = 0;
               if (selectionActive) {
                  if (selectionSecondsLeft > 0) {
                     selectionSecondsLeft--;
                  }

                  if (selectionSecondsLeft <= 0) {
                     selectionActive = false;
                     removeUnclaimedSelectionTrackers(server);
                  }

                  syncSelectionAll(server);
               }

               if (!contractsByOwner.isEmpty()) {
                  if (signalSecondsLeft > 0) {
                     signalSecondsLeft--;
                  }

                  if (signalSecondsLeft <= 0) {
                     updateTargetAreas(server);
                     signalSecondsLeft = 15;
                     syncTrackers(server);
                  }
               }
            }
         }
      }
   }

   public static void reset(MinecraftServer server) {
      if (server != null) {
         removeAllTrackers(server);
         removeDebugArmorStands(server);
      }

      clearContracts();
      debugArmorStands.clear();
      pickCooldownUntil.clear();
      trackerViewers.clear();
      selectionActive = false;
      selectionSecondsLeft = 0;
      signalSecondsLeft = 15;
      tickCounter = 0;
   }

   public static void setSoloDebugEnabled(boolean enabled) {
      soloDebugEnabled = enabled;
   }

   public static boolean isSoloDebugEnabled() {
      return soloDebugEnabled;
   }

   public static void forceStartSelection(MinecraftServer server) {
      if (server != null && GameStateManager.isRunning(server)) {
         selectionActive = true;
         selectionSecondsLeft = 60;
         giveSelectionTrackers(server);
         syncSelectionAll(server);
      }
   }

   public static boolean selectTarget(ServerPlayer owner, UUID targetUuid) {
      if (owner != null && targetUuid != null) {
         MinecraftServer server = owner.f_8924_;
         if (!selectionActive || selectionSecondsLeft <= 0) {
            owner.m_213846_(Component.m_237113_("[WAR] Выбор контракта уже закрыт."));
            syncSelection(owner);
            return false;
         } else if (!LivesManager.canContinueMatch(owner)) {
            owner.m_213846_(Component.m_237113_("[WAR] Нельзя выбрать контракт после выбывания."));
            syncSelection(owner);
            return false;
         } else if (contractsByOwner.containsKey(owner.m_20148_())) {
            owner.m_213846_(Component.m_237113_("[WAR] У тебя уже есть активный контракт."));
            syncSelection(owner);
            return false;
         } else {
            ServerPlayer target = server.m_6846_().m_11259_(targetUuid);
            if (!isValidTarget(owner, target)) {
               owner.m_213846_(Component.m_237113_("[WAR] Эта цель недоступна."));
               syncSelection(owner);
               return false;
            } else {
               ContractDifficulty difficulty = difficultyFor(target);
               if (PlayerProgressManager.getCoins(owner) < difficulty.price()) {
                  owner.m_213846_(Component.m_237113_("[WAR] Не хватает монет для контракта. Нужно " + difficulty.price() + "."));
                  syncSelection(owner);
                  return false;
               } else {
                  PlayerProgressManager.addCoins(owner, -difficulty.price());
                  PlayerProgressManager.savePlayer(owner);
                  ContractManager.Contract contract = new ContractManager.Contract(
                     owner.m_20148_(),
                     target.m_20148_(),
                     target.m_7755_().getString(),
                     selectedClassName(target),
                     difficulty,
                     PlayerProgressManager.getKills(target),
                     PlayerProgressManager.getWins(target),
                     PlayerProgressManager.getCareerProgressPercent(target)
                  );
                  putContract(contract);
                  updateTargetArea(server, contract);
                  giveTracker(owner);
                  owner.m_213846_(Component.m_237113_("[WAR] Контракт принят: цель " + contract.targetName() + ". Награда: " + difficulty.reward() + " монет."));
                  target.m_213846_(Component.m_237113_("[WAR] Игрок \"" + owner.m_7755_().getString() + "\" начал охоту за вами в этой игре."));
                  refreshContractAccess(server);
                  sendTrackerState(owner, true);
                  return true;
               }
            }
         }
      } else {
         return false;
      }
   }

   public static void onTrackerUsed(ServerPlayer player) {
      if (player != null) {
         long now = System.currentTimeMillis();
         long cooldownUntil = pickCooldownUntil.getOrDefault(player.m_20148_(), 0L);
         if (cooldownUntil > now) {
            long secondsLeft = Math.max(1L, (cooldownUntil - now + 999L) / 1000L);
            player.m_213846_(Component.m_237113_("[WAR] РўСЂРµРєРµСЂ РїРµСЂРµР·Р°СЂСЏР¶Р°РµС‚СЃСЏ: " + secondsLeft + " СЃ."));
            syncSelection(player);
         } else {
            pickCooldownUntil.put(player.m_20148_(), now + 3000L);
            if (!contractsByOwner.containsKey(player.m_20148_())) {
               if (canSelectContract(player)) {
                  syncSelection(player);
                  sendTrackerState(player, true);
               } else if (!visibleContracts(player).isEmpty()) {
                  sendTrackerState(player, true);
               } else {
                  removeTracker(player);
                  player.m_213846_(Component.m_237113_("[WAR] У тебя нет активного контракта."));
               }
            } else {
               sendTrackerState(player, true);
            }
         }
      }
   }

   public static boolean createDebugSelfContract(ServerPlayer player) {
      if (player == null) {
         return false;
      } else if (!player.m_20310_(2)) {
         player.m_213846_(Component.m_237113_("Доступно только администраторам"));
         return false;
      } else {
         ContractDifficulty difficulty = difficultyFor(player);
         ContractManager.Contract contract = new ContractManager.Contract(
            player.m_20148_(),
            player.m_20148_(),
            player.m_7755_().getString(),
            selectedClassName(player),
            difficulty,
            PlayerProgressManager.getKills(player),
            PlayerProgressManager.getWins(player),
            PlayerProgressManager.getCareerProgressPercent(player)
         );
         putContract(contract);
         updateTargetArea(player.f_8924_, contract);
         giveTracker(player);
         sendTrackerState(player, true);
         syncSelection(player);
         return true;
      }
   }

   public static boolean createDebugArmorStandContract(ServerPlayer player) {
      if (player == null) {
         return false;
      }

      if (!player.m_20310_(2)) {
         player.m_213846_(Component.m_237113_("Доступно только администраторам"));
         return false;
      }

      ServerLevel level = GameStateManager.getOverworld(player.f_8924_);
      if (level == null) {
         return false;
      }

      ArmorStand stand = (ArmorStand)EntityType.f_20529_.m_20615_(level);
      if (stand == null) {
         return false;
      }

      WorldBorder border = level.m_6857_();
      int radius = Math.max(20, (int)Math.round(border.m_61959_() / 2.0) - 10);
      int x = (int)Math.round(border.m_6347_()) + RANDOM.nextInt(radius * 2 + 1) - radius;
      int z = (int)Math.round(border.m_6345_()) + RANDOM.nextInt(radius * 2 + 1) - radius;
      int y = level.m_6924_(Types.MOTION_BLOCKING_NO_LEAVES, x, z);
      stand.m_7678_(x + 0.5, y, z + 0.5, RANDOM.nextFloat() * 360.0F, 0.0F);
      stand.m_6593_(Component.m_237113_("Тестовая цель контракта"));
      stand.m_20340_(true);
      stand.m_20242_(false);
      stand.m_20331_(false);
      level.m_7967_(stand);
      debugArmorStands.add(stand.m_20148_());
      ContractDifficulty difficulty = ContractDifficulty.MEDIUM;
      ContractManager.Contract contract = new ContractManager.Contract(
         player.m_20148_(), stand.m_20148_(), "Тестовая цель", "Armor Stand", difficulty, 0, 0, 50
      );
      putContract(contract);
      contract.setTargetArea(x, z);
      giveTracker(player);
      sendTrackerState(player, true);
      syncSelection(player);
      player.m_213846_(Component.m_237113_("[WAR] Тестовая цель создана: X " + x + " Z " + z + "."));
      return true;
   }

   public static void onPlayerKilled(ServerPlayer victim, ServerPlayer killer) {
      if (victim != null) {
         MinecraftServer server = victim.f_8924_;
         Set<UUID> toRemove = new HashSet<>();

         for (ContractManager.Contract contract : new ArrayList<>(contractsByOwner.values())) {
            if (contract.targetUuid().equals(victim.m_20148_())) {
               if (killer != null && isContractHunter(contract, killer.m_20148_())) {
                  rewardTeam(server, contract.ownerUuid(), contract.difficulty().reward(), "Контракт выполнен");
               } else if (isSuicide(victim, killer)) {
                  ServerPlayer payer = server.m_6846_().m_11259_(contract.ownerUuid());
                  if (payer != null) {
                     PlayerProgressManager.addCoins(payer, contract.difficulty().price());
                     PlayerProgressManager.savePlayer(payer);
                     payer.m_213846_(Component.m_237113_("[WAR] Цель контракта погибла сама. Возврат: " + contract.difficulty().price() + " монет."));
                  }
               } else {
                  notifyTeam(server, contract.ownerUuid(), "Цель выбыла. Контракт закрыт.");
               }

               toRemove.add(contract.ownerUuid());
            } else if (isContractHunter(contract, victim.m_20148_()) && !hasAliveContractHunter(server, contract)) {
               notifyTeam(server, contract.ownerUuid(), "Контракт провален: команда выбыла из матча.");
               rewardSurvivingTarget(server, contract, "Твоя команда пережила контракт");
               toRemove.add(contract.ownerUuid());
            }
         }

         for (UUID ownerUuid : toRemove) {
            removeContract(ownerUuid);
         }

         if (!toRemove.isEmpty()) {
            refreshContractAccess(server);
         }
      }
   }

   public static void syncSelection(ServerPlayer player) {
      if (player != null) {
         PacketHandler.sendToPlayer(player, selectionState(player));
      }
   }

   public static void onPlayerDisconnect(ServerPlayer player) {
      if (player != null) {
         MinecraftServer server = player.f_8924_;
         UUID uuid = player.m_20148_();
         trackerViewers.remove(uuid);
         removeTracker(player);
         Set<UUID> toRemove = new HashSet<>();

         for (ContractManager.Contract contract : new ArrayList<>(contractsByOwner.values())) {
            if (contract.targetUuid().equals(uuid)) {
               notifyTeam(server, contract.ownerUuid(), "Цель вышла с сервера. Контракт закрыт.");
               toRemove.add(contract.ownerUuid());
            } else if (isContractHunter(contract, uuid) && !hasAliveContractHunterExcluding(server, contract, uuid)) {
               rewardSurvivingTarget(server, contract, "Твоя команда пережила контракт");
               toRemove.add(contract.ownerUuid());
            }
         }

         for (UUID ownerUuid : toRemove) {
            removeContract(ownerUuid);
         }

         refreshContractAccess(server);
      }
   }

   public static void finishMatch(MinecraftServer server) {
      if (server != null) {
         if (contractsByOwner.isEmpty()) {
            trackerViewers.clear();
         } else {
            for (ContractManager.Contract contract : new ArrayList<>(contractsByOwner.values())) {
               notifyTeam(server, contract.ownerUuid(), "Контракт закрыт: матч завершён.");
               rewardSurvivingTarget(server, contract, "Твоя команда пережила контракт до конца матча");
            }

            clearContracts();
            selectionActive = false;
            selectionSecondsLeft = 0;
            trackerViewers.clear();
            refreshContractAccess(server);
         }
      }
   }

   public static void resetPickCooldowns(MinecraftServer server) {
      pickCooldownUntil.clear();
      syncSelectionAll(server);
   }

   public static void resetPickCooldown(ServerPlayer player) {
      if (player != null) {
         pickCooldownUntil.remove(player.m_20148_());
         syncSelection(player);
      }
   }

   public static void syncSelectionAll(MinecraftServer server) {
      if (server != null) {
         for (ServerPlayer player : server.m_6846_().m_11314_()) {
            syncSelection(player);
         }
      }
   }

   public static ContractSelectionStatePacket selectionState(ServerPlayer viewer) {
      List<ContractSelectionStatePacket.TargetEntry> entries = new ArrayList<>();
      boolean canSelect = canSelectContract(viewer);
      if (viewer != null && canSelect) {
         List<ServerPlayer> targets = new ArrayList<>(viewer.f_8924_.m_6846_().m_11314_());
         targets.sort(Comparator.comparing(player -> player.m_7755_().getString(), String.CASE_INSENSITIVE_ORDER));

         for (ServerPlayer target : targets) {
            if (entries.size() >= 16) {
               break;
            }

            if (isValidTarget(viewer, target)) {
               ContractDifficulty difficulty = difficultyFor(target);
               entries.add(
                  new ContractSelectionStatePacket.TargetEntry(
                     target.m_20148_(),
                     target.m_7755_().getString(),
                     selectedClassName(target),
                     PlayerProgressManager.getKills(target),
                     PlayerProgressManager.getWins(target),
                     PlayerProgressManager.getCareerProgressPercent(target),
                     difficulty.ordinal(),
                     difficulty.price(),
                     difficulty.reward()
                  )
               );
            }
         }
      }

      return new ContractSelectionStatePacket(
         selectionActive,
         selectionSecondsLeft,
         cooldownLeftMs(viewer),
         contractsByOwner.containsKey(viewer == null ? null : viewer.m_20148_()),
         GameStateManager.getCurrentMode() != null,
         entries
      );
   }

   public static void sendTrackerState(ServerPlayer player, boolean open) {
      if (player != null) {
         PacketHandler.sendToPlayer(player, trackerState(player, open));
      }
   }

   public static void addTrackerViewer(ServerPlayer player) {
      if (player != null) {
         trackerViewers.add(player.m_20148_());
         sendTrackerState(player, false);
      }
   }

   public static void removeTrackerViewer(ServerPlayer player) {
      if (player != null) {
         trackerViewers.remove(player.m_20148_());
      }
   }

   public static void giveSelectionTrackerIfAvailable(ServerPlayer player) {
      if (player != null && canSelectContract(player)) {
         giveTracker(player);
         syncSelection(player);
      }
   }

   private static void giveSelectionTrackers(MinecraftServer server) {
      for (ServerPlayer player : server.m_6846_().m_11314_()) {
         giveSelectionTrackerIfAvailable(player);
      }
   }

   private static ContractTrackerStatePacket trackerState(ServerPlayer player, boolean open) {
      List<ContractManager.Contract> contracts = visibleContracts(player);
      ServerLevel overworld = GameStateManager.getOverworld(player.f_8924_);
      WorldBorder border = overworld == null ? null : overworld.m_6857_();
      int zoneCenterX = border == null ? 0 : (int)Math.round(border.m_6347_());
      int zoneCenterZ = border == null ? 0 : (int)Math.round(border.m_6345_());
      int zoneRadius = border == null ? 180 : Math.max(1, (int)Math.round(border.m_61959_() / 2.0));
      if (!canSelectContract(player) && !contracts.isEmpty()) {
         List<ContractTrackerStatePacket.TargetEntry> targets = new ArrayList<>();

         for (ContractManager.Contract contract : contracts) {
            targets.add(
               new ContractTrackerStatePacket.TargetEntry(
                  contract.targetName(),
                  contract.targetClass(),
                  contract.targetKills(),
                  contract.targetWins(),
                  contract.targetCareerPercent(),
                  contract.difficulty().ordinal(),
                  contract.difficulty().price(),
                  contract.difficulty().reward(),
                  contract.targetAreaX(),
                  contract.targetAreaZ(),
                  25
               )
            );
         }

         return new ContractTrackerStatePacket(
            true,
            open,
            zoneCenterX,
            zoneCenterZ,
            zoneRadius,
            (int)Math.round(player.m_20185_()),
            (int)Math.round(player.m_20189_()),
            signalSecondsLeft,
            targets
         );
      } else {
         return ContractTrackerStatePacket.empty(open, zoneCenterX, zoneCenterZ, zoneRadius);
      }
   }

   private static boolean canSelectContract(ServerPlayer player) {
      return player != null
         && selectionActive
         && selectionSecondsLeft > 0
         && GameStateManager.isRunning(player.f_8924_)
         && LivesManager.canContinueMatch(player)
         && !contractsByOwner.containsKey(player.m_20148_());
   }

   private static long cooldownLeftMs(ServerPlayer player) {
      return player == null ? 0L : Math.max(0L, pickCooldownUntil.getOrDefault(player.m_20148_(), 0L) - System.currentTimeMillis());
   }

   private static boolean isValidTarget(ServerPlayer owner, ServerPlayer target) {
      return owner != null
         && target != null
         && !owner.m_20148_().equals(target.m_20148_())
         && !TeamMatchManager.areTeammates(owner, target)
         && !isTargetClaimedByTeam(owner, target.m_20148_())
         && LivesManager.isAliveParticipant(target);
   }

   private static boolean isTargetClaimedByTeam(ServerPlayer owner, UUID targetUuid) {
      if (owner != null && targetUuid != null) {
         for (ContractManager.Contract contract : contractsByOwner.values()) {
            if (contract.targetUuid().equals(targetUuid) && sameContractTeam(owner.m_20148_(), contract.ownerUuid())) {
               return true;
            }
         }

         return false;
      } else {
         return false;
      }
   }

   private static List<ContractManager.Contract> visibleContracts(ServerPlayer player) {
      if (player == null) {
         return List.of();
      }

      List<ContractManager.Contract> result = new ArrayList<>();

      for (ContractManager.Contract contract : contractsByOwner.values()) {
         if (sameContractTeam(player.m_20148_(), contract.ownerUuid())) {
            result.add(contract);
         }
      }

      result.sort(Comparator.comparing(ContractManager.Contract::targetName, String.CASE_INSENSITIVE_ORDER));
      return result;
   }

   private static boolean sameContractTeam(UUID first, UUID second) {
      if (first == null || second == null) {
         return false;
      } else {
         return first.equals(second)
            ? true
            : GameStateManager.getCurrentMode() != null && GameStateManager.getCurrentMode().isTeamMode() && TeamMatchManager.areTeammates(first, second);
      }
   }

   private static List<ServerPlayer> contractTeam(MinecraftServer server, UUID playerUuid) {
      if (server != null && playerUuid != null) {
         if (GameStateManager.getCurrentMode() != null && GameStateManager.getCurrentMode().isTeamMode()) {
            List<ServerPlayer> team = TeamMatchManager.getOnlineTeamMembers(server, playerUuid);
            if (!team.isEmpty()) {
               return team;
            }
         }

         ServerPlayer player = server.m_6846_().m_11259_(playerUuid);
         return player == null ? List.of() : List.of(player);
      } else {
         return List.of();
      }
   }

   private static ContractDifficulty difficultyFor(ServerPlayer target) {
      return ContractDifficulty.forCareerPercent(PlayerProgressManager.getCareerProgressPercent(target));
   }

   private static void updateTargetAreas(MinecraftServer server) {
      for (ContractManager.Contract contract : contractsByOwner.values()) {
         updateTargetArea(server, contract);
      }
   }

   private static boolean isSuicide(ServerPlayer victim, ServerPlayer killer) {
      return killer == null || killer.m_20148_().equals(victim.m_20148_());
   }

   private static boolean isContractHunter(ContractManager.Contract contract, UUID playerUuid) {
      return contract != null && playerUuid != null && sameContractTeam(contract.ownerUuid(), playerUuid);
   }

   private static boolean hasAliveContractHunter(MinecraftServer server, ContractManager.Contract contract) {
      for (ServerPlayer player : contractTeam(server, contract.ownerUuid())) {
         if (LivesManager.isAliveParticipant(player)) {
            return true;
         }
      }

      return false;
   }

   private static boolean hasAliveContractHunterExcluding(MinecraftServer server, ContractManager.Contract contract, UUID excludedUuid) {
      for (ServerPlayer player : contractTeam(server, contract.ownerUuid())) {
         if (!player.m_20148_().equals(excludedUuid) && LivesManager.isAliveParticipant(player)) {
            return true;
         }
      }

      return false;
   }

   private static void notifyTeam(MinecraftServer server, UUID playerUuid, String message) {
      for (ServerPlayer member : contractTeam(server, playerUuid)) {
         member.m_213846_(Component.m_237113_("[WAR] " + message));
      }
   }

   private static void rewardTeam(MinecraftServer server, UUID playerUuid, int totalReward, String reason) {
      List<ServerPlayer> recipients = new ArrayList<>(contractTeam(server, playerUuid));
      if (!recipients.isEmpty() && totalReward > 0) {
         recipients.sort(Comparator.comparing(Entity::m_20149_));
         int baseShare = totalReward / recipients.size();
         int remainder = totalReward % recipients.size();

         for (int i = 0; i < recipients.size(); i++) {
            ServerPlayer recipient = recipients.get(i);
            int share = baseShare + (i < remainder ? 1 : 0);
            if (share > 0) {
               PlayerProgressManager.addCoins(recipient, share);
               PlayerProgressManager.savePlayer(recipient);
               recipient.m_213846_(Component.m_237113_("[WAR] " + reason + ". Твоя доля: " + share + " монет."));
            }
         }
      }
   }

   private static void putContract(ContractManager.Contract contract) {
      if (contract != null) {
         removeContract(contract.ownerUuid());
         contractsByOwner.put(contract.ownerUuid(), contract);
         targetToOwners.computeIfAbsent(contract.targetUuid(), ignored -> new HashSet<>()).add(contract.ownerUuid());
      }
   }

   private static ContractManager.Contract removeContract(UUID ownerUuid) {
      if (ownerUuid == null) {
         return null;
      }

      ContractManager.Contract removed = contractsByOwner.remove(ownerUuid);
      if (removed != null) {
         Set<UUID> owners = targetToOwners.get(removed.targetUuid());
         if (owners != null) {
            owners.remove(ownerUuid);
            if (owners.isEmpty()) {
               targetToOwners.remove(removed.targetUuid());
            }
         }
      }

      return removed;
   }

   private static void clearContracts() {
      contractsByOwner.clear();
      targetToOwners.clear();
   }

   private static void rewardSurvivingTarget(MinecraftServer server, ContractManager.Contract contract, String reason) {
      ServerPlayer target = server.m_6846_().m_11259_(contract.targetUuid());
      if (target != null && LivesManager.isAliveParticipant(target)) {
         int reward = Math.max(1, contract.difficulty().reward() / 2);
         rewardTeam(server, contract.targetUuid(), reward, reason);
      }
   }

   private static void updateTargetArea(MinecraftServer server, ContractManager.Contract contract) {
      ServerPlayer target = server.m_6846_().m_11259_(contract.targetUuid());
      if (target != null) {
         int roundedX = Math.round(target.m_146903_() / 50.0F) * 50;
         int roundedZ = Math.round(target.m_146907_() / 50.0F) * 50;
         contract.setTargetArea(roundedX + RANDOM.nextInt(41) - 20, roundedZ + RANDOM.nextInt(41) - 20);
      }
   }

   private static void syncTrackers(MinecraftServer server) {
      trackerViewers.removeIf(uuid -> server.m_6846_().m_11259_(uuid) == null);

      for (ServerPlayer player : server.m_6846_().m_11314_()) {
         if (!visibleContracts(player).isEmpty() || trackerViewers.contains(player.m_20148_())) {
            ensureTracker(player);
            sendTrackerState(player, false);
         }
      }
   }

   private static void removeUnclaimedSelectionTrackers(MinecraftServer server) {
      for (ServerPlayer player : server.m_6846_().m_11314_()) {
         if (visibleContracts(player).isEmpty()) {
            removeTracker(player);
         }
      }
   }

   public static void ensureTracker(ServerPlayer player) {
      if (player != null && !visibleContracts(player).isEmpty()) {
         if (!hasTracker(player)) {
            giveTracker(player);
         }
      }
   }

   private static void refreshContractAccess(MinecraftServer server) {
      if (server != null) {
         for (ServerPlayer player : server.m_6846_().m_11314_()) {
            boolean needsTracker = canSelectContract(player) || !visibleContracts(player).isEmpty();
            if (needsTracker) {
               giveTracker(player);
            } else {
               removeTracker(player);
            }

            syncSelection(player);
            if (trackerViewers.contains(player.m_20148_())) {
               sendTrackerState(player, false);
            }
         }
      }
   }

   private static void giveTracker(ServerPlayer player) {
      if (player != null && !hasTracker(player)) {
         ItemStack stack = new ItemStack((ItemLike)ModItems.CONTRACT_TRACKER.get());
         if (!player.m_150109_().m_36054_(stack)) {
            player.m_36176_(stack, false);
         }

         InventoryManager.syncInventory(player);
      }
   }

   private static boolean hasTracker(ServerPlayer player) {
      return !findTracker(player).m_41619_();
   }

   private static ItemStack findTracker(ServerPlayer player) {
      if (player == null) {
         return ItemStack.f_41583_;
      }

      for (int i = 0; i < player.m_150109_().m_6643_(); i++) {
         if (player.m_150109_().m_8020_(i).m_41720_() == ModItems.CONTRACT_TRACKER.get()) {
            return player.m_150109_().m_8020_(i);
         }
      }

      return ItemStack.f_41583_;
   }

   public static void removeTracker(ServerPlayer player) {
      if (player != null) {
         for (InteractionHand hand : InteractionHand.values()) {
            if (player.m_21120_(hand).m_41720_() == ModItems.CONTRACT_TRACKER.get()) {
               player.m_21008_(hand, ItemStack.f_41583_);
            }
         }

         for (int i = 0; i < player.m_150109_().m_6643_(); i++) {
            if (player.m_150109_().m_8020_(i).m_41720_() == ModItems.CONTRACT_TRACKER.get()) {
               player.m_150109_().m_6836_(i, ItemStack.f_41583_);
            }
         }

         InventoryManager.syncInventory(player);
      }
   }

   private static void removeAllTrackers(MinecraftServer server) {
      for (ServerPlayer player : server.m_6846_().m_11314_()) {
         removeTracker(player);
      }
   }

   private static void removeDebugArmorStands(MinecraftServer server) {
      ServerLevel level = GameStateManager.getOverworld(server);
      if (level != null && !debugArmorStands.isEmpty()) {
         for (UUID uuid : new HashSet<>(debugArmorStands)) {
            Entity entity = level.m_8791_(uuid);
            if (entity != null) {
               entity.m_146870_();
            }
         }

         debugArmorStands.clear();
      }
   }

   private static String selectedClassName(ServerPlayer player) {
      String selected = PlayerTabletState.getSelectedClass(player);
      return selected != null && !selected.isBlank() ? displayClassName(selected) : "Не выбран";
   }

   private static String displayClassName(String kit) {
      if ("boomguy".equals(kit)) {
         return "Подрывник";
      } else if ("dream".equals(kit)) {
         return "Дрим";
      } else if ("rpgtrooper".equals(kit)) {
         return "РПГ-боец";
      } else if ("droneoperator".equals(kit)) {
         return "Оператор дрона";
      } else if ("machinegunner".equals(kit)) {
         return "Пулемётчик";
      } else if ("mortarman".equals(kit)) {
         return "Миномётчик";
      } else if ("stormtrooper".equals(kit)) {
         return "Штурмовик";
      } else if ("sniper".equals(kit)) {
         return "Снайпер";
      } else if ("scout".equals(kit)) {
         return "Разведчик";
      } else if ("tagilla".equals(kit)) {
         return "Тагилла";
      } else if ("blackops".equals(kit)) {
         return "Спецназ";
      } else if ("cowboy".equals(kit)) {
         return "Ковбой";
      } else if ("solider".equals(kit)) {
         return "Солдат";
      } else if ("rebel".equals(kit)) {
         return "Повстанец";
      } else if ("saboteur".equals(kit)) {
         return "Диверсант";
      } else if ("killer".equals(kit)) {
         return "Киллер";
      } else if ("miniboss".equals(kit)) {
         return "Мини-Босс";
      } else if ("shahed".equals(kit)) {
         return "Шахед оп.";
      } else {
         return "krot".equals(kit) ? "Крот" : kit;
      }
   }

   private static final class Contract {
      private final UUID ownerUuid;
      private final UUID targetUuid;
      private final String targetName;
      private final String targetClass;
      private final ContractDifficulty difficulty;
      private final int targetKills;
      private final int targetWins;
      private final int targetCareerPercent;
      private int targetAreaX;
      private int targetAreaZ;

      private Contract(
         UUID ownerUuid,
         UUID targetUuid,
         String targetName,
         String targetClass,
         ContractDifficulty difficulty,
         int targetKills,
         int targetWins,
         int targetCareerPercent
      ) {
         this.ownerUuid = Objects.requireNonNull(ownerUuid, "ownerUuid");
         this.targetUuid = Objects.requireNonNull(targetUuid, "targetUuid");
         this.targetName = Objects.requireNonNull(targetName, "targetName");
         this.targetClass = Objects.requireNonNull(targetClass, "targetClass");
         this.difficulty = Objects.requireNonNull(difficulty, "difficulty");
         this.targetKills = targetKills;
         this.targetWins = targetWins;
         this.targetCareerPercent = targetCareerPercent;
      }

      private UUID ownerUuid() {
         return this.ownerUuid;
      }

      private UUID targetUuid() {
         return this.targetUuid;
      }

      private String targetName() {
         return this.targetName;
      }

      private String targetClass() {
         return this.targetClass;
      }

      private ContractDifficulty difficulty() {
         return this.difficulty;
      }

      private int targetKills() {
         return this.targetKills;
      }

      private int targetWins() {
         return this.targetWins;
      }

      private int targetCareerPercent() {
         return this.targetCareerPercent;
      }

      private int targetAreaX() {
         return this.targetAreaX;
      }

      private int targetAreaZ() {
         return this.targetAreaZ;
      }

      private void setTargetArea(int x, int z) {
         this.targetAreaX = x;
         this.targetAreaZ = z;
      }
   }
}
