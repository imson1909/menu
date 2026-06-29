package com.makar.tacticaltablet.tablet;

import com.makar.tacticaltablet.progression.PlayerProgressManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

public final class TabletAppearanceManager {
   public static final int EPIC_MODEL_DATA = 1;
   public static final int LEGEND_MODEL_DATA = 2;

   private TabletAppearanceManager() {
   }

   public static int getAppearanceTier(ServerPlayer player) {
      return getModelData(player);
   }

   public static ItemStack apply(ServerPlayer player, ItemStack stack) {
      if (stack != null && !stack.m_41619_()) {
         int modelData = getModelData(player);
         if (modelData <= 0) {
            if (stack.m_41782_()) {
               stack.m_41783_().m_128473_("CustomModelData");
            }

            return stack;
         } else {
            stack.m_41784_().m_128405_("CustomModelData", modelData);
            return stack;
         }
      } else {
         return stack;
      }
   }

   private static int getModelData(ServerPlayer player) {
      if (player == null) {
         return 0;
      }

      boolean allEpic = true;
      boolean allLegend = true;

      for (String clazz : PlayerProgressManager.getStandardClasses()) {
         int level = PlayerProgressManager.getLevel(player, clazz);
         if (level < 1) {
            allEpic = false;
         }

         if (level < 2) {
            allLegend = false;
         }
      }

      if (allLegend) {
         return 2;
      } else {
         return allEpic ? 1 : 0;
      }
   }
}
