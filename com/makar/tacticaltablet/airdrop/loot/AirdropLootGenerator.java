package com.makar.tacticaltablet.airdrop.loot;

import com.makar.tacticaltablet.core.TacticalTabletMod;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

public final class AirdropLootGenerator {
   private AirdropLootGenerator() {
   }

   public static void fillChest(ServerLevel level, BlockPos chestPos) {
      if (level != null && chestPos != null) {
         if (!(level.m_7702_(chestPos) instanceof Container container)) {
            TacticalTabletMod.LOGGER.warn("AirDrop loot skipped: block at {} is not a container.", chestPos);
         } else {
            container.m_6211_();
            AirdropLootLoader.AirdropLootConfig config = AirdropLootLoader.getConfig();
            if (!config.sets().isEmpty() && config.totalSetWeight() > 0) {
               AirdropLootSet set = chooseSet(level, config);
               if (set != null && set.items != null && !set.items.isEmpty()) {
                  int placedEntries = 0;

                  for (AirdropLootEntry entry : set.items) {
                     if (isContainerFull(container)) {
                        TacticalTabletMod.LOGGER
                           .warn(
                              "AirDrop loot set '{}' has more entries than free chest slots. Placed {} of {} entries.",
                              new Object[]{set.name, placedEntries, set.items.size()}
                           );
                        break;
                     }

                     ItemStack stack = createStack(level, entry);
                     if (!stack.m_41619_()) {
                        int slot = findPreferredFreeSlot(container, entry.slot);
                        if (slot < 0) {
                           break;
                        }

                        container.m_6836_(slot, stack);
                        placedEntries++;
                     }
                  }

                  container.m_6596_();
                  TacticalTabletMod.LOGGER
                     .info("Filled AirDrop chest with loot set '{}' ({} of {} entries).", new Object[]{set.name, placedEntries, set.items.size()});
               } else {
                  TacticalTabletMod.LOGGER.warn("AirDrop loot skipped: selected loot set is empty.");
               }
            } else {
               TacticalTabletMod.LOGGER.warn("AirDrop loot skipped: no valid loot sets.");
            }
         }
      }
   }

   public static int reloadLoot() {
      return AirdropLootLoader.reload();
   }

   private static AirdropLootSet chooseSet(ServerLevel level, AirdropLootLoader.AirdropLootConfig config) {
      int roll = level.f_46441_.m_188503_(config.totalSetWeight()) + 1;
      int cursor = 0;

      for (AirdropLootSet set : config.sets()) {
         cursor += set.weight;
         if (roll <= cursor) {
            return set;
         }
      }

      return config.sets().get(config.sets().size() - 1);
   }

   private static ItemStack createStack(ServerLevel level, AirdropLootEntry entry) {
      ResourceLocation id = new ResourceLocation(entry.item);
      Item item = (Item)ForgeRegistries.ITEMS.getValue(id);
      if (item == null) {
         return ItemStack.f_41583_;
      }

      ItemStack stack = new ItemStack(item, entry.count);
      if (entry.nbt != null && !entry.nbt.isBlank()) {
         try {
            CompoundTag tag = TagParser.m_129359_(entry.nbt);
            stack.m_41751_(tag);
         } catch (Exception exception) {
            TacticalTabletMod.LOGGER.warn("Failed to parse AirDrop loot NBT for {}: {}", new Object[]{entry.item, entry.nbt, exception});
            return ItemStack.f_41583_;
         }
      }

      return stack;
   }

   private static int findPreferredFreeSlot(Container container, Integer preferredSlot) {
      if (preferredSlot != null && preferredSlot >= 0 && preferredSlot < container.m_6643_() && container.m_8020_(preferredSlot).m_41619_()) {
         return preferredSlot;
      }

      for (int slot = 0; slot < container.m_6643_(); slot++) {
         if (container.m_8020_(slot).m_41619_()) {
            return slot;
         }
      }

      return -1;
   }

   private static boolean isContainerFull(Container container) {
      for (int slot = 0; slot < container.m_6643_(); slot++) {
         if (container.m_8020_(slot).m_41619_()) {
            return false;
         }
      }

      return true;
   }
}
