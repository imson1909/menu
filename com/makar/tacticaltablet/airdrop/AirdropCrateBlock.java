package com.makar.tacticaltablet.airdrop;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.BlockHitResult;

public final class AirdropCrateBlock extends BaseEntityBlock {
   public static final BooleanProperty OPENED = BooleanProperty.m_61465_("opened");

   public AirdropCrateBlock(Properties properties) {
      super(properties);
      this.m_49959_((BlockState)((BlockState)this.f_49792_.m_61090_()).m_61124_(OPENED, false));
   }

   protected void m_7926_(Builder<Block, BlockState> builder) {
      builder.m_61104_(new Property[]{OPENED});
   }

   public RenderShape m_7514_(BlockState state) {
      return RenderShape.MODEL;
   }

   public BlockEntity m_142194_(BlockPos pos, BlockState state) {
      return new AirdropCrateBlockEntity(pos, state);
   }

   public InteractionResult m_6227_(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
      if (level.f_46443_) {
         return InteractionResult.SUCCESS;
      }

      if (level.m_7702_(pos) instanceof AirdropCrateBlockEntity crate) {
         if (!(Boolean)state.m_61143_(OPENED)) {
            level.m_7731_(pos, (BlockState)state.m_61124_(OPENED, true), 3);
            if (player instanceof ServerPlayer serverPlayer) {
               AirdropManager.onChestInteract(serverPlayer, pos);
            }
         }

         player.m_5893_(crate);
         return InteractionResult.CONSUME;
      } else {
         return InteractionResult.PASS;
      }
   }

   public boolean m_7278_(BlockState state) {
      return true;
   }

   public int m_6782_(BlockState state, Level level, BlockPos pos) {
      return AbstractContainerMenu.m_38918_(level.m_7702_(pos));
   }

   public boolean m_7420_(BlockState state, BlockGetter level, BlockPos pos) {
      return true;
   }
}
