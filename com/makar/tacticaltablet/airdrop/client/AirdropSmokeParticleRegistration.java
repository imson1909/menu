package com.makar.tacticaltablet.airdrop.client;

import com.makar.tacticaltablet.core.ModParticles;
import net.minecraft.core.particles.ParticleType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterParticleProvidersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;

@EventBusSubscriber(modid = "tacticaltablet", bus = Bus.MOD, value = Dist.CLIENT)
public final class AirdropSmokeParticleRegistration {
   private AirdropSmokeParticleRegistration() {
   }

   @SubscribeEvent
   public static void registerParticles(RegisterParticleProvidersEvent event) {
      event.registerSpriteSet((ParticleType)ModParticles.AIRDROP_SMOKE.get(), AirdropSmokeParticle.Provider::new);
   }
}
