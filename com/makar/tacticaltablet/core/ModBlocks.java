package com.makar.tacticaltablet.core;

import com.makar.tacticaltablet.airdrop.AirdropCrateBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModBlocks {
   public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, "tacticaltablet");
   public static final RegistryObject<Block> AIRDROP_CRATE = BLOCKS.register(
      "airdrop_crate", () -> new AirdropCrateBlock(Properties.m_284310_().m_284180_(MapColor.f_283784_).m_60978_(3.5F).m_60918_(SoundType.f_56736_).m_60955_())
   );

   private ModBlocks() {
   }
}
