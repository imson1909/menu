package com.makar.tacticaltablet.tablet;

import com.makar.tacticaltablet.game.lobby.LobbyManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.level.Level;

public class TacticalTabletItem extends Item {
   public TacticalTabletItem(Properties props) {
      super(props);
   }

   public InteractionResultHolder<ItemStack> m_7203_(Level level, Player player, InteractionHand hand) {
      if (!level.f_46443_ && player instanceof ServerPlayer sp) {
         LobbyManager.sync(sp);
      }

      return InteractionResultHolder.m_19092_(player.m_21120_(hand), level.f_46443_);
   }
}
