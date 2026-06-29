package com.makar.tacticaltablet.tablet.client;

import com.makar.tacticaltablet.tablet.net.ContractSelectionStatePacket;
import com.makar.tacticaltablet.tablet.net.ContractTrackerStatePacket;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;

public final class ContractClientPacketHandler {
   private static final int MAX_TARGETS = 16;
   private static final int MAX_NAME_LENGTH = 32;
   private static final int MAX_CLASS_LENGTH = 32;

   private ContractClientPacketHandler() {
   }

   public static void handleSelection(ContractSelectionStatePacket packet) {
      if (packet != null) {
         ContractClientState.updateSelection(
            packet.selectionActive(),
            packet.selectionSecondsLeft(),
            packet.cooldownLeftMs(),
            packet.hasActiveContract(),
            packet.soloMode(),
            sanitizeSelectionTargets(packet.targets())
         );
      }
   }

   public static void handleTracker(ContractTrackerStatePacket packet) {
      if (packet != null && packet.zoneRadius() > 0) {
         List<ContractTrackerStatePacket.TargetEntry> targets = sanitizeTrackerTargets(packet.targets());
         ContractClientState.updateTracker(
            packet.active() && !targets.isEmpty(),
            packet.zoneCenterX(),
            packet.zoneCenterZ(),
            packet.zoneRadius(),
            packet.playerX(),
            packet.playerZ(),
            packet.signalSecondsLeft(),
            targets
         );
         Minecraft minecraft = Minecraft.m_91087_();
         if (packet.openScreen() && minecraft.f_91073_ != null && minecraft.f_91074_ != null) {
            minecraft.m_91152_(new ContractTrackerScreen());
         }
      }
   }

   private static List<ContractSelectionStatePacket.TargetEntry> sanitizeSelectionTargets(List<ContractSelectionStatePacket.TargetEntry> targets) {
      if (targets != null && !targets.isEmpty()) {
         List<ContractSelectionStatePacket.TargetEntry> sanitized = new ArrayList<>();

         for (ContractSelectionStatePacket.TargetEntry target : targets) {
            if (target != null && target.uuid() != null) {
               if (sanitized.size() >= 16) {
                  break;
               }

               sanitized.add(
                  new ContractSelectionStatePacket.TargetEntry(
                     target.uuid(),
                     sanitizeText(target.name(), 32),
                     sanitizeText(target.selectedClass(), 32),
                     Math.max(0, target.kills()),
                     Math.max(0, target.wins()),
                     Math.max(0, Math.min(100, target.careerPercent())),
                     Math.max(0, target.difficulty()),
                     Math.max(0, target.price()),
                     Math.max(0, target.reward())
                  )
               );
            }
         }

         return sanitized;
      } else {
         return List.of();
      }
   }

   private static List<ContractTrackerStatePacket.TargetEntry> sanitizeTrackerTargets(List<ContractTrackerStatePacket.TargetEntry> targets) {
      if (targets != null && !targets.isEmpty()) {
         List<ContractTrackerStatePacket.TargetEntry> sanitized = new ArrayList<>();

         for (ContractTrackerStatePacket.TargetEntry target : targets) {
            if (target != null && target.areaRadius() >= 0) {
               if (sanitized.size() >= 16) {
                  break;
               }

               sanitized.add(
                  new ContractTrackerStatePacket.TargetEntry(
                     sanitizeText(target.name(), 32),
                     sanitizeText(target.selectedClass(), 32),
                     target.kills(),
                     target.wins(),
                     target.careerPercent(),
                     target.difficulty(),
                     target.price(),
                     target.reward(),
                     target.areaX(),
                     target.areaZ(),
                     target.areaRadius()
                  )
               );
            }
         }

         return sanitized;
      } else {
         return List.of();
      }
   }

   private static String sanitizeText(String value, int maxLength) {
      if (value != null && !value.isBlank()) {
         return value.length() <= maxLength ? value : value.substring(0, maxLength);
      } else {
         return "";
      }
   }
}
