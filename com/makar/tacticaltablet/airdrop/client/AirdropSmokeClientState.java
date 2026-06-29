package com.makar.tacticaltablet.airdrop.client;

import com.makar.tacticaltablet.airdrop.net.AirdropSmokeStatePacket;
import com.makar.tacticaltablet.core.ModParticles;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent.LoggingOut;
import net.minecraftforge.event.TickEvent.ClientTickEvent;
import net.minecraftforge.event.TickEvent.Phase;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

@EventBusSubscriber(modid = "tacticaltablet", value = Dist.CLIENT)
public final class AirdropSmokeClientState {
   private static final double EMIT_DISTANCE_SQ = 9216.0;
   private static final int EMIT_INTERVAL_TICKS = 2;
   private static final int PARTICLES_PER_EMISSION = 2;
   private static boolean active;
   private static ResourceLocation dimension;
   private static BlockPos pos = BlockPos.f_121853_;
   private static int ticker;

   private AirdropSmokeClientState() {
   }

   public static void handle(AirdropSmokeStatePacket packet) {
      active = packet.active();
      dimension = packet.dimension();
      pos = packet.pos().m_7949_();
      ticker = 0;
   }

   @SubscribeEvent
   public static void onClientTick(ClientTickEvent event) {
      if (event.phase == Phase.END && active) {
         Minecraft minecraft = Minecraft.m_91087_();
         ClientLevel level = minecraft.f_91073_;
         if (level != null && minecraft.f_91074_ != null && dimension != null) {
            if (level.m_46472_().m_135782_().equals(dimension)) {
               if (!(minecraft.f_91074_.m_20275_(pos.m_123341_() + 0.5, pos.m_123342_() + 1.0, pos.m_123343_() + 0.5) > 9216.0)) {
                  ticker++;
                  if (ticker >= 2) {
                     ticker = 0;

                     for (int i = 0; i < 2; i++) {
                        double x = pos.m_123341_() + 0.5 + (level.f_46441_.m_188500_() - 0.5) * 0.55;
                        double y = pos.m_123342_() + 1.0 + level.f_46441_.m_188500_() * 0.35;
                        double z = pos.m_123343_() + 0.5 + (level.f_46441_.m_188500_() - 0.5) * 0.55;
                        double velocityX = (level.f_46441_.m_188500_() - 0.5) * 0.012;
                        double velocityY = 0.105 + level.f_46441_.m_188500_() * 0.035;
                        double velocityZ = (level.f_46441_.m_188500_() - 0.5) * 0.012;
                        level.m_7106_((ParticleOptions)ModParticles.AIRDROP_SMOKE.get(), x, y, z, velocityX, velocityY, velocityZ);
                     }
                  }
               }
            }
         }
      }
   }

   @SubscribeEvent
   public static void onLogout(LoggingOut event) {
      active = false;
      dimension = null;
      ticker = 0;
   }
}
