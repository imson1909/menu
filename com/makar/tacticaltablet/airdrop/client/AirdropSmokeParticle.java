package com.makar.tacticaltablet.airdrop.client;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;
import org.jetbrains.annotations.Nullable;

public final class AirdropSmokeParticle extends TextureSheetParticle {
   private static final float RED = 0.82F;
   private static final float GREEN = 0.035F;
   private static final float BLUE = 0.025F;
   private final SpriteSet sprites;
   private final float initialSize;
   private final float spinSpeed;

   private AirdropSmokeParticle(ClientLevel level, double x, double y, double z, double velocityX, double velocityY, double velocityZ, SpriteSet sprites) {
      super(level, x, y, z, velocityX, velocityY, velocityZ);
      this.sprites = sprites;
      this.f_107219_ = false;
      this.f_172258_ = 0.96F;
      this.f_107225_ = 150 + this.f_107223_.m_188503_(51);
      this.initialSize = 0.475F + this.f_107223_.m_188501_() * 0.225F;
      this.f_107663_ = this.initialSize;
      this.f_107230_ = 0.0F;
      this.f_107231_ = this.f_107223_.m_188501_() * (float) (Math.PI * 2);
      this.f_107204_ = this.f_107231_;
      this.spinSpeed = (this.f_107223_.m_188501_() - 0.5F) * 0.012F;
      this.m_107253_(0.82F, 0.035F, 0.025F);
      this.m_108339_(sprites);
   }

   public void m_5989_() {
      this.f_107209_ = this.f_107212_;
      this.f_107210_ = this.f_107213_;
      this.f_107211_ = this.f_107214_;
      this.f_107204_ = this.f_107231_;
      if (this.f_107224_++ >= this.f_107225_) {
         this.m_107274_();
      } else {
         this.m_6257_(this.f_107215_, this.f_107216_, this.f_107217_);
         this.f_107215_ *= 0.96;
         this.f_107216_ *= 0.997;
         this.f_107217_ *= 0.96;
         this.f_107231_ = this.f_107231_ + this.spinSpeed;
         float progress = (float)this.f_107224_ / this.f_107225_;
         float fadeIn = Math.min(1.0F, this.f_107224_ / 10.0F);
         float fadeOut = Math.min(1.0F, (this.f_107225_ - this.f_107224_) / 28.0F);
         this.f_107230_ = 0.82F * fadeIn * fadeOut;
         this.f_107663_ = this.initialSize * (0.85F + progress * 2.35F);
         this.m_108339_(this.sprites);
      }
   }

   public ParticleRenderType m_7556_() {
      return ParticleRenderType.f_107431_;
   }

   public static final class Provider implements ParticleProvider<SimpleParticleType> {
      private final SpriteSet sprites;

      public Provider(SpriteSet sprites) {
         this.sprites = sprites;
      }

      @Nullable
      public Particle createParticle(
         SimpleParticleType type, ClientLevel level, double x, double y, double z, double velocityX, double velocityY, double velocityZ
      ) {
         return new AirdropSmokeParticle(level, x, y, z, velocityX, velocityY, velocityZ, this.sprites);
      }
   }
}
