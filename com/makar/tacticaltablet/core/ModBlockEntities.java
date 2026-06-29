package com.makar.tacticaltablet.core;

import com.makar.tacticaltablet.airdrop.AirdropCrateBlockEntity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.BlockEntityType.Builder;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModBlockEntities {
   public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, "tacticaltablet");
   public static final RegistryObject<BlockEntityType<AirdropCrateBlockEntity>> AIRDROP_CRATE = BLOCK_ENTITIES.register(
      "airdrop_crate", () -> Builder.m_155273_(AirdropCrateBlockEntity::new, new Block[]{(Block)ModBlocks.AIRDROP_CRATE.get()}).m_58966_(null)
   );

   private ModBlockEntities() {
   }
}
