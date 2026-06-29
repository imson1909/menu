package com.makar.tacticaltablet.game.contract;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.level.Level;

public class ContractTrackerItem extends Item {
   public ContractTrackerItem(Properties properties) {
      super(properties);
   }

   public InteractionResultHolder<ItemStack> m_7203_(Level level, Player player, InteractionHand hand) {
      if (!level.f_46443_ && player instanceof ServerPlayer serverPlayer) {
         ContractManager.onTrackerUsed(serverPlayer);
      }

      return InteractionResultHolder.m_19092_(player.m_21120_(hand), level.f_46443_);
   }
}
