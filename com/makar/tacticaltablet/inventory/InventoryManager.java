package com.makar.tacticaltablet.inventory;

import com.makar.tacticaltablet.core.ModItems;
import com.makar.tacticaltablet.tablet.TabletAppearanceManager;
import net.minecraft.network.protocol.game.ClientboundContainerSetContentPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;

public class InventoryManager {
   public static void clearInventory(ServerPlayer player) {
      if (player != null) {
         boolean changed = !player.m_150109_().m_7983_()
            || !player.m_21120_(InteractionHand.MAIN_HAND).m_41619_()
            || !player.m_21120_(InteractionHand.OFF_HAND).m_41619_();
         if (changed) {
            player.m_150109_().m_6211_();
            player.m_21008_(InteractionHand.MAIN_HAND, ItemStack.f_41583_);
            player.m_21008_(InteractionHand.OFF_HAND, ItemStack.f_41583_);
            player.m_150109_().m_6596_();
            syncInventory(player);
         }
      }
   }

   public static void giveFreshTablet(ServerPlayer player) {
      if (player != null) {
         boolean changed = removeTablets(player);
         player.m_150109_().m_36054_(createTablet(player));
         changed = true;
         if (changed) {
            player.m_150109_().m_6596_();
            syncInventory(player);
         }
      }
   }

   public static void giveTabletIfMissing(ServerPlayer player) {
      if (player != null) {
         if (hasTablet(player)) {
            updateTabletModels(player);
         } else {
            player.m_150109_().m_36054_(createTablet(player));
            player.m_150109_().m_6596_();
            syncInventory(player);
         }
      }
   }

   public static void clearTablets(ServerPlayer player) {
      if (player != null) {
         if (removeTablets(player)) {
            player.m_150109_().m_6596_();
            syncInventory(player);
         }
      }
   }

   public static boolean hasTablet(ServerPlayer player) {
      if (player == null) {
         return false;
      }

      for (InteractionHand hand : InteractionHand.values()) {
         if (player.m_21120_(hand).m_41720_() == ModItems.TACTICAL_TABLET.get()) {
            return true;
         }
      }

      for (int i = 0; i < player.m_150109_().m_6643_(); i++) {
         if (player.m_150109_().m_8020_(i).m_41720_() == ModItems.TACTICAL_TABLET.get()) {
            return true;
         }
      }

      return false;
   }

   public static void updateTabletModels(ServerPlayer player) {
      if (player != null) {
         boolean changed = false;

         for (InteractionHand hand : InteractionHand.values()) {
            ItemStack stack = player.m_21120_(hand);
            if (stack.m_41720_() == ModItems.TACTICAL_TABLET.get()) {
               String before = stack.m_41783_() == null ? "" : stack.m_41783_().toString();
               TabletAppearanceManager.apply(player, stack);
               String after = stack.m_41783_() == null ? "" : stack.m_41783_().toString();
               changed |= !before.equals(after);
            }
         }

         for (int i = 0; i < player.m_150109_().m_6643_(); i++) {
            ItemStack stack = player.m_150109_().m_8020_(i);
            if (stack.m_41720_() == ModItems.TACTICAL_TABLET.get()) {
               String before = stack.m_41783_() == null ? "" : stack.m_41783_().toString();
               TabletAppearanceManager.apply(player, stack);
               String after = stack.m_41783_() == null ? "" : stack.m_41783_().toString();
               changed |= !before.equals(after);
            }
         }

         if (changed) {
            player.m_150109_().m_6596_();
            syncInventory(player);
         }
      }
   }

   public static void syncInventory(ServerPlayer player) {
      if (player != null) {
         player.f_36095_.m_38946_();
         player.f_36096_.m_38946_();
         player.f_8906_
            .m_9829_(
               new ClientboundContainerSetContentPacket(
                  player.f_36095_.f_38840_, player.f_36095_.m_182425_(), player.f_36095_.m_38927_(), player.f_36095_.m_142621_()
               )
            );
      }
   }

   private static ItemStack createTablet(ServerPlayer player) {
      return TabletAppearanceManager.apply(player, new ItemStack((ItemLike)ModItems.TACTICAL_TABLET.get()));
   }

   private static boolean removeTablets(ServerPlayer player) {
      boolean changed = false;

      for (InteractionHand hand : InteractionHand.values()) {
         if (player.m_21120_(hand).m_41720_() == ModItems.TACTICAL_TABLET.get()) {
            player.m_21008_(hand, ItemStack.f_41583_);
            changed = true;
         }
      }

      for (int i = 0; i < player.m_150109_().m_6643_(); i++) {
         if (player.m_150109_().m_8020_(i).m_41720_() == ModItems.TACTICAL_TABLET.get()) {
            player.m_150109_().m_6836_(i, ItemStack.f_41583_);
            changed = true;
         }
      }

      return changed;
   }
}
