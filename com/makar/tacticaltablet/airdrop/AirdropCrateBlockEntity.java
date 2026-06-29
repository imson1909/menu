package com.makar.tacticaltablet.airdrop;

import com.makar.tacticaltablet.core.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public final class AirdropCrateBlockEntity extends RandomizableContainerBlockEntity {
   private static final int SLOT_COUNT = 27;
   private NonNullList<ItemStack> items = NonNullList.m_122780_(27, ItemStack.f_41583_);

   public AirdropCrateBlockEntity(BlockPos pos, BlockState state) {
      super((BlockEntityType)ModBlockEntities.AIRDROP_CRATE.get(), pos, state);
   }

   protected Component m_6820_() {
      return Component.m_237115_("container.tacticaltablet.airdrop_crate");
   }

   protected AbstractContainerMenu m_6555_(int containerId, Inventory inventory) {
      return ChestMenu.m_39237_(containerId, inventory, this);
   }

   public int m_6643_() {
      return 27;
   }

   protected NonNullList<ItemStack> m_7086_() {
      return this.items;
   }

   protected void m_6520_(NonNullList<ItemStack> items) {
      this.items = items;
   }
}
