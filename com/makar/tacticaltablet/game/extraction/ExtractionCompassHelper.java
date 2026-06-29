package com.makar.tacticaltablet.game.extraction;

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

public final class ExtractionCompassHelper {
   private static final String TAG_EVENT_ITEM = "tactical_event_item";
   private static final String TAG_EVENT_TYPE = "event_type";
   private static final String TAG_EVENT_ID = "event_id";
   private static final String TAG_TARGET_X = "extraction_target_x";
   private static final String TAG_TARGET_Y = "extraction_target_y";
   private static final String TAG_TARGET_Z = "extraction_target_z";
   private static final String TAG_TARGET_DIMENSION = "extraction_target_dimension";
   private static final String EVENT_TYPE = "extraction_point";
   private static final int EXTRACTION_COMPASS_MODEL_DATA = 93001;

   private ExtractionCompassHelper() {
   }

   public static void giveOrUpdate(ServerPlayer player, ExtractionPointData data, ResourceKey<Level> dimension) {
      if (player != null && data != null && data.center != null && data.eventId != null) {
         ItemStack existing = findExtractionCompass(player.m_150109_());
         if (!existing.m_41619_()) {
            configureCompass(existing, data.eventId, data.center, dimension);
            removeDuplicates(player, existing);
         } else {
            ItemStack compass = new ItemStack(Items.f_220211_);
            configureCompass(compass, data.eventId, data.center, dimension);
            if (!player.m_150109_().m_36054_(compass)) {
               player.m_36176_(compass, false);
            }
         }
      }
   }

   public static void removeAllExtractionCompasses(ServerPlayer player) {
      if (player != null) {
         Inventory inventory = player.m_150109_();
         removeFromList(inventory.f_35974_);
         removeFromList(inventory.f_35976_);
      }
   }

   public static boolean isExtractionCompass(ItemStack stack) {
      if (stack != null && !stack.m_41619_() && stack.m_150930_(Items.f_220211_)) {
         CompoundTag tag = stack.m_41783_();
         return tag != null && tag.m_128471_("tactical_event_item") && "extraction_point".equals(tag.m_128461_("event_type"));
      } else {
         return false;
      }
   }

   private static ItemStack findExtractionCompass(Inventory inventory) {
      for (ItemStack stack : inventory.f_35974_) {
         if (isExtractionCompass(stack)) {
            return stack;
         }
      }

      for (ItemStack stack : inventory.f_35976_) {
         if (isExtractionCompass(stack)) {
            return stack;
         }
      }

      return ItemStack.f_41583_;
   }

   private static void removeDuplicates(ServerPlayer player, ItemStack kept) {
      boolean foundKept = false;

      for (int i = 0; i < player.m_150109_().m_6643_(); i++) {
         ItemStack stack = player.m_150109_().m_8020_(i);
         if (isExtractionCompass(stack)) {
            if (!foundKept && stack == kept) {
               foundKept = true;
            } else {
               player.m_150109_().m_6836_(i, ItemStack.f_41583_);
            }
         }
      }

      player.m_150109_().m_6596_();
   }

   private static void removeFromList(NonNullList<ItemStack> items) {
      for (int index = 0; index < items.size(); index++) {
         if (isExtractionCompass((ItemStack)items.get(index))) {
            items.set(index, ItemStack.f_41583_);
         }
      }
   }

   private static void configureCompass(ItemStack stack, UUID eventId, BlockPos target, ResourceKey<Level> dimension) {
      stack.m_41714_(Component.m_237113_("бизнес-темка"));
      CompoundTag tag = stack.m_41784_();
      tag.m_128379_("tactical_event_item", true);
      tag.m_128359_("event_type", "extraction_point");
      tag.m_128359_("event_id", eventId.toString());
      tag.m_128405_("extraction_target_x", target.m_123341_());
      tag.m_128405_("extraction_target_y", target.m_123342_());
      tag.m_128405_("extraction_target_z", target.m_123343_());
      tag.m_128359_("extraction_target_dimension", dimension.m_135782_().toString());
      tag.m_128405_("CustomModelData", 93001);
      tag.m_128379_("LodestoneTracked", false);
      tag.m_128359_("LodestoneDimension", dimension.m_135782_().toString());
      CompoundTag pos = new CompoundTag();
      pos.m_128405_("X", target.m_123341_());
      pos.m_128405_("Y", target.m_123342_());
      pos.m_128405_("Z", target.m_123343_());
      tag.m_128365_("LodestonePos", pos);
      CompoundTag display = tag.m_128469_("display");
      ListTag lore = new ListTag();
      lore.add(StringTag.m_129297_(Serializer.m_130703_(Component.m_237113_("§7Указывает на активную бизнес-точку"))));
      lore.add(StringTag.m_129297_(Serializer.m_130703_(Component.m_237113_("§7Исчезнет после завершения события"))));
      display.m_128365_("Lore", lore);
      tag.m_128365_("display", display);
   }
}
