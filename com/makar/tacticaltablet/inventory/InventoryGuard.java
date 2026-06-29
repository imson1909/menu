package com.makar.tacticaltablet.inventory;

import com.makar.tacticaltablet.airdrop.AirdropCompassHelper;
import com.makar.tacticaltablet.airdrop.AirdropManager;
import com.makar.tacticaltablet.anticheat.AntiCheatManager;
import com.makar.tacticaltablet.anticheat.Severity;
import com.makar.tacticaltablet.anticheat.ViolationType;
import com.makar.tacticaltablet.core.ModItems;
import com.makar.tacticaltablet.game.GameStateManager;
import com.makar.tacticaltablet.game.extraction.ExtractionCompassHelper;
import com.makar.tacticaltablet.game.extraction.ExtractionPointManager;
import com.makar.tacticaltablet.game.lives.LivesManager;
import com.makar.tacticaltablet.tablet.PlayerTabletState;
import java.util.Set;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;

public class InventoryGuard {
   private static int tickCounter = 0;

   public static void tick(MinecraftServer server) {
      if (server != null) {
         tickCounter++;
         if (tickCounter >= 40) {
            tickCounter = 0;

            for (ServerPlayer player : server.m_6846_().m_11314_()) {
               check(player);
            }
         }
      }
   }

   private static void check(ServerPlayer player) {
      Set<String> tags = player.m_19880_();
      boolean inLobby = GameStateManager.isInLobby(player) || tags.contains("in_lobby");
      boolean playing = tags.contains("war.playing");
      boolean eliminated = LivesManager.isEliminated(player);
      boolean gameRunning = GameStateManager.isRunning(player.f_8924_);
      boolean tabletLobbyStage = GameStateManager.isTabletAvailableInLobby(player.f_8924_);
      boolean kitUsed = PlayerTabletState.isKitUsed(player);
      boolean rtpUsed = PlayerTabletState.isRtpUsed(player);
      boolean relevant = inLobby || playing || eliminated;
      if (relevant) {
         if (eliminated) {
            if (!isInventoryEmpty(player)) {
               int removed = countItems(player);
               InventoryManager.clearInventory(player);
               recordInventory(player, removed, "eliminated inventory cleanup");
            }
         } else if (inLobby && !gameRunning && tabletLobbyStage) {
            keepOnlyTabletCompassAndSync(player);
         } else if (inLobby && !gameRunning) {
            if (!isInventoryEmpty(player)) {
               int removed = countItems(player);
               InventoryManager.clearInventory(player);
               recordInventory(player, removed, "waiting lobby inventory cleanup");
            }
         } else if (inLobby && gameRunning && !kitUsed) {
            keepOnlyTabletCompassAndSync(player);
         } else if (inLobby && gameRunning && !rtpUsed) {
            InventoryManager.giveTabletIfMissing(player);
         } else if (playing && rtpUsed && !kitUsed) {
            keepOnlyTabletCompassAndSync(player);
         } else {
            if (playing && kitUsed && InventoryManager.hasTablet(player)) {
               int removed = countTablets(player);
               InventoryManager.clearTablets(player);
               recordInventory(player, removed, Severity.HIGH, "tablet after kit used");
            }
         }
      }
   }

   private static void keepOnlyTabletCompassAndSync(ServerPlayer player) {
      InventoryGuard.InventoryCleanup cleanup = keepOnlyTabletAndAirdropCompass(player);
      boolean changed = cleanup.removed() > 0;
      if (!InventoryManager.hasTablet(player)) {
         InventoryManager.giveTabletIfMissing(player);
      }

      AirdropManager.giveCompassToJoiningPlayer(player);
      ExtractionPointManager.giveCompassToActiveParticipant(player);
      if (changed) {
         InventoryManager.syncInventory(player);
      }

      if (changed) {
         boolean duplicateTablets = cleanup.extraTablets() > 0;
         recordInventory(
            player,
            cleanup.removed(),
            duplicateTablets ? Severity.HIGH : severityForRemoved(cleanup.removed()),
            duplicateTablets ? "removed non-tablet items and duplicate tablets" : "removed non-tablet items"
         );
      }
   }

   private static InventoryGuard.InventoryCleanup keepOnlyTabletAndAirdropCompass(ServerPlayer player) {
      int removed = 0;
      int extraTablets = 0;
      boolean tabletAlreadyKept = false;
      boolean compassAlreadyKept = false;
      boolean extractionCompassAlreadyKept = false;
      boolean trackerAlreadyKept = false;

      for (int i = 0; i < player.m_150109_().m_6643_(); i++) {
         ItemStack stack = player.m_150109_().m_8020_(i);
         if (!stack.m_41619_()) {
            if (stack.m_41720_() == ModItems.TACTICAL_TABLET.get()) {
               if (!tabletAlreadyKept) {
                  tabletAlreadyKept = true;
               } else {
                  player.m_150109_().m_6836_(i, ItemStack.f_41583_);
                  removed++;
                  extraTablets++;
               }
            } else if (AirdropCompassHelper.isAirdropCompass(stack)) {
               if (!compassAlreadyKept) {
                  compassAlreadyKept = true;
               } else {
                  player.m_150109_().m_6836_(i, ItemStack.f_41583_);
                  removed++;
               }
            } else if (!ExtractionCompassHelper.isExtractionCompass(stack) || !ExtractionPointManager.isActive()) {
               if (stack.m_41720_() == ModItems.CONTRACT_TRACKER.get()) {
                  if (!trackerAlreadyKept) {
                     trackerAlreadyKept = true;
                  } else {
                     player.m_150109_().m_6836_(i, ItemStack.f_41583_);
                     removed++;
                  }
               } else {
                  player.m_150109_().m_6836_(i, ItemStack.f_41583_);
                  removed++;
               }
            } else if (!extractionCompassAlreadyKept) {
               extractionCompassAlreadyKept = true;
            } else {
               player.m_150109_().m_6836_(i, ItemStack.f_41583_);
               removed++;
            }
         }
      }

      if (removed > 0) {
         player.m_150109_().m_6596_();
      }

      return new InventoryGuard.InventoryCleanup(removed, extraTablets);
   }

   private static boolean isInventoryEmpty(ServerPlayer player) {
      for (int i = 0; i < player.m_150109_().m_6643_(); i++) {
         if (!player.m_150109_().m_8020_(i).m_41619_()) {
            return false;
         }
      }

      return true;
   }

   private static int countItems(ServerPlayer player) {
      int count = 0;

      for (int i = 0; i < player.m_150109_().m_6643_(); i++) {
         if (!player.m_150109_().m_8020_(i).m_41619_()) {
            count++;
         }
      }

      return count;
   }

   private static int countTablets(ServerPlayer player) {
      int count = 0;

      for (InteractionHand hand : InteractionHand.values()) {
         if (player.m_21120_(hand).m_41720_() == ModItems.TACTICAL_TABLET.get()) {
            count++;
         }
      }

      for (int i = 0; i < player.m_150109_().m_6643_(); i++) {
         if (player.m_150109_().m_8020_(i).m_41720_() == ModItems.TACTICAL_TABLET.get()) {
            count++;
         }
      }

      return count;
   }

   private static Severity severityForRemoved(int removed) {
      return removed > 1 ? Severity.MEDIUM : Severity.LOW;
   }

   private static void recordInventory(ServerPlayer player, int removed, String reason) {
      recordInventory(player, removed, severityForRemoved(removed), reason);
   }

   private static void recordInventory(ServerPlayer player, int removed, Severity severity, String reason) {
      if (removed > 0) {
         AntiCheatManager.record(player, ViolationType.ILLEGAL_INVENTORY, severity, "removed " + removed + " items; reason=" + reason);
      }
   }

   private record InventoryCleanup(int removed, int extraTablets) {
   }
}
