package com.makar.tacticaltablet.airdrop;

import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Component.Serializer;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

public final class AirdropCompassHelper {
   private static final String TAG_AIRDROP_COMPASS = "TacticalTabletAirdropCompass";
   private static final String TAG_AIRDROP_ID = "AirdropId";

   private AirdropCompassHelper() {
   }

   public static void giveOrUpdate(ServerPlayer player, AirdropData data) {
      if (player != null && data != null && data.compassTargetPos != null) {
         ItemStack existing = findAirdropCompass(player.m_150109_());
         if (!existing.m_41619_()) {
            configureCompass(existing, data.id, data.compassTargetPos, data.dimension);
         } else {
            ItemStack compass = new ItemStack(Items.f_42522_);
            configureCompass(compass, data.id, data.compassTargetPos, data.dimension);
            if (!player.m_150109_().m_36054_(compass)) {
               player.m_36176_(compass, false);
            }
         }
      }
   }

   public static void removeAllAirdropCompasses(ServerPlayer player) {
      if (player != null) {
         Inventory inventory = player.m_150109_();
         removeFromList(inventory.f_35974_);
         removeFromList(inventory.f_35976_);
      }
   }

   public static boolean isAirdropCompass(ItemStack stack) {
      if (stack != null && !stack.m_41619_() && stack.m_150930_(Items.f_42522_)) {
         CompoundTag tag = stack.m_41783_();
         return tag != null && tag.m_128471_("TacticalTabletAirdropCompass");
      } else {
         return false;
      }
   }

   private static ItemStack findAirdropCompass(Inventory inventory) {
      for (ItemStack stack : inventory.f_35974_) {
         if (isAirdropCompass(stack)) {
            return stack;
         }
      }

      for (ItemStack stack : inventory.f_35976_) {
         if (isAirdropCompass(stack)) {
            return stack;
         }
      }

      return ItemStack.f_41583_;
   }

   private static void removeFromList(NonNullList<ItemStack> items) {
      for (int index = 0; index < items.size(); index++) {
         ItemStack stack = (ItemStack)items.get(index);
         if (isAirdropCompass(stack)) {
            items.set(index, ItemStack.f_41583_);
         }
      }
   }

   private static void configureCompass(ItemStack stack, UUID airdropId, BlockPos target, ResourceKey<Level> dimension) {
      stack.m_41714_(Component.m_237113_("§cКомпас сброса"));
      CompoundTag tag = stack.m_41784_();
      tag.m_128379_("TacticalTabletAirdropCompass", true);
      tag.m_128359_("AirdropId", airdropId.toString());
      tag.m_128379_("LodestoneTracked", false);
      tag.m_128359_("LodestoneDimension", dimension.m_135782_().toString());
      CompoundTag pos = new CompoundTag();
      pos.m_128405_("X", target.m_123341_());
      pos.m_128405_("Y", target.m_123342_());
      pos.m_128405_("Z", target.m_123343_());
      tag.m_128365_("LodestonePos", pos);
      CompoundTag display = tag.m_128469_("display");
      ListTag lore = new ListTag();
      lore.add(StringTag.m_129297_(Serializer.m_130703_(Component.m_237113_("§7Указывает в примерную зону сброса"))));
      lore.add(StringTag.m_129297_(Serializer.m_130703_(Component.m_237113_("§7Точка может быть неточной"))));
      display.m_128365_("Lore", lore);
      tag.m_128365_("display", display);
   }
}
