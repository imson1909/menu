package com.makar.tacticaltablet.core;

import com.makar.tacticaltablet.game.contract.ContractTrackerItem;
import com.makar.tacticaltablet.tablet.TacticalTabletItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModItems {
   public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, "tacticaltablet");
   public static final RegistryObject<Item> TACTICAL_TABLET = ITEMS.register("tactical_tablet", () -> new TacticalTabletItem(new Properties().m_41487_(1)));
   public static final RegistryObject<Item> CONTRACT_TRACKER = ITEMS.register("contract_tracker", () -> new ContractTrackerItem(new Properties().m_41487_(1)));
   public static final RegistryObject<Item> AIRDROP_CRATE = ITEMS.register(
      "airdrop_crate", () -> new BlockItem((Block)ModBlocks.AIRDROP_CRATE.get(), new Properties())
   );
   public static final RegistryObject<Item> AIRDROP_CRATE_FLYING = ITEMS.register("airdrop_crate_flying", () -> new Item(new Properties().m_41487_(1)));
}
