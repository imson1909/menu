package com.makar.tacticaltablet.corpse;

import com.makar.tacticaltablet.core.ModEntities;
import net.minecraft.world.entity.EntityType;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;

@EventBusSubscriber(modid = "tacticaltablet", bus = Bus.MOD)
public final class CorpseEntityEvents {
   private CorpseEntityEvents() {
   }

   @SubscribeEvent
   public static void onEntityAttributes(EntityAttributeCreationEvent event) {
      event.put((EntityType)ModEntities.PLAYER_CORPSE.get(), CorpseEntity.createAttributes().m_22265_());
   }
}
