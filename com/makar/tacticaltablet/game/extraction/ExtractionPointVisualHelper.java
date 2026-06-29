package com.makar.tacticaltablet.game.extraction;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import org.joml.Vector3f;

public final class ExtractionPointVisualHelper {
   private static final Map<String, DustParticleOptions> PARTICLES = new ConcurrentHashMap<>();

   private ExtractionPointVisualHelper() {
   }

   public static void spawnRing(ServerLevel level, ExtractionPointData data, ExtractionPointConfig config, ExtractionPointVisualHelper.VisualMode mode) {
      if (level != null && data != null && data.center != null && config != null && config.particleEnabled) {
         DustParticleOptions particle = particle(mode, config.particleScale);
         int basePoints = config.ringPoints * 2;
         int points = mode == ExtractionPointVisualHelper.VisualMode.CAPTURED ? basePoints * 2 : basePoints;
         double radius = data.radius;
         double visibleDistanceSq = config.particleVisibleDistance * config.particleVisibleDistance;
         double bottomY = data.center.m_123342_() - data.halfHeight;
         double topY = data.center.m_123342_() + data.halfHeight;

         for (double y = bottomY; y <= topY + 0.1; y += 5.0) {
            for (int index = 0; index < points; index++) {
               double angle = (Math.PI * 2) * index / points;
               double x = data.center.m_123341_() + 0.5 + Math.cos(angle) * radius;
               double z = data.center.m_123343_() + 0.5 + Math.sin(angle) * radius;
               double particleY = y + 0.15;

               for (ServerPlayer player : level.m_6907_()) {
                  if (!(player.m_20275_(x, particleY, z) > visibleDistanceSq)) {
                     level.m_8624_(player, particle, true, x, particleY, z, 1, 0.02, 0.01, 0.02, 0.0);
                  }
               }
            }
         }
      }
   }

   public static void playCaptured(ServerLevel level, BlockPos center) {
      if (level != null && center != null) {
         level.m_5594_(null, center, SoundEvents.f_12496_, SoundSource.PLAYERS, 1.0F, 1.0F);
         level.m_5594_(null, center, SoundEvents.f_12275_, SoundSource.PLAYERS, 0.8F, 1.2F);
      }
   }

   private static Vector3f color(ExtractionPointVisualHelper.VisualMode mode) {
      return switch (mode) {
         case CAPTURING -> new Vector3f(0.1F, 0.9F, 0.25F);
         case CONTESTED -> new Vector3f(1.0F, 0.25F, 0.05F);
         case CAPTURED -> new Vector3f(0.1F, 1.0F, 0.25F);
         case ENDING -> new Vector3f(0.35F, 0.35F, 0.35F);
         case NORMAL -> new Vector3f(1.0F, 0.85F, 0.35F);
      };
   }

   private static DustParticleOptions particle(ExtractionPointVisualHelper.VisualMode mode, float scale) {
      String key = mode.name() + ":" + scale;
      return PARTICLES.computeIfAbsent(key, ignored -> new DustParticleOptions(color(mode), scale));
   }

   public enum VisualMode {
      NORMAL,
      CAPTURING,
      CONTESTED,
      CAPTURED,
      ENDING;
   }
}
