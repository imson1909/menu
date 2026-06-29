package com.makar.tacticaltablet.corpse.client;

import com.makar.tacticaltablet.core.ModEntities;
import net.minecraft.world.entity.EntityType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent.RegisterRenderers;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;

@EventBusSubscriber(modid = "tacticaltablet", bus = Bus.MOD, value = Dist.CLIENT)
public final class CorpseClientEvents {
   private CorpseClientEvents() {
   }

   @SubscribeEvent
   public static void onRegisterRenderers(RegisterRenderers event) {
      event.registerEntityRenderer((EntityType)ModEntities.PLAYER_CORPSE.get(), CorpseRenderer::new);
   }
}
