package com.makar.tacticaltablet.map;

import com.makar.tacticaltablet.game.GameStateManager;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.phys.AABB;

public class WorldCleanupManager {
   private static final double MAX_CLEANUP_RADIUS = 2048.0;

   public static void clearDroppedItems(MinecraftServer server) {
      if (server != null) {
         clearDroppedItemsInLevel(server.m_129880_(Level.f_46428_));
         clearDroppedItemsInLevel(GameStateManager.getLobbyLevel(server));
      }
   }

   private static void clearDroppedItemsInLevel(ServerLevel level) {
      if (level != null) {
         WorldBorder border = level.m_6857_();
         double radius = Math.min(2048.0, Math.max(16.0, border.m_61959_() / 2.0));
         AABB area = new AABB(
            border.m_6347_() - radius, level.m_141937_(), border.m_6345_() - radius, border.m_6347_() + radius, level.m_151558_(), border.m_6345_() + radius
         );

         for (ItemEntity item : level.m_142425_(EntityType.f_20461_, area, Entity::m_6084_)) {
            item.m_146870_();
         }
      }
   }
}
