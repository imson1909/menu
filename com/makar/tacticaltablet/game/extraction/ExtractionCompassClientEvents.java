package com.makar.tacticaltablet.game.extraction;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@EventBusSubscriber(modid = "tacticaltablet", bus = Bus.MOD, value = Dist.CLIENT)
public final class ExtractionCompassClientEvents {
   private static final String TAG_EVENT_ITEM = "tactical_event_item";
   private static final String TAG_EVENT_TYPE = "event_type";
   private static final String TAG_TARGET_X = "extraction_target_x";
   private static final String TAG_TARGET_Z = "extraction_target_z";
   private static final String TAG_TARGET_DIMENSION = "extraction_target_dimension";
   private static final String EVENT_TYPE = "extraction_point";

   private ExtractionCompassClientEvents() {
   }

   @SubscribeEvent
   public static void onClientSetup(FMLClientSetupEvent event) {
      event.enqueueWork(
         () -> ItemProperties.register(Items.f_220211_, new ResourceLocation("tacticaltablet", "extraction_angle"), ExtractionCompassClientEvents::angle)
      );
   }

   private static float angle(ItemStack stack, ClientLevel level, LivingEntity entity, int seed) {
      if (!isExtractionCompass(stack)) {
         return -1.0F;
      }

      if (level != null && entity != null) {
         CompoundTag tag = stack.m_41783_();
         if (tag != null && tag.m_128441_("extraction_target_x") && tag.m_128441_("extraction_target_z")) {
            String targetDimension = tag.m_128461_("extraction_target_dimension");
            if (!targetDimension.isBlank() && targetDimension.equals(level.m_46472_().m_135782_().toString())) {
               double targetX = tag.m_128451_("extraction_target_x") + 0.5;
               double targetZ = tag.m_128451_("extraction_target_z") + 0.5;
               double dx = targetX - entity.m_20185_();
               double dz = targetZ - entity.m_20189_();
               if (dx * dx + dz * dz < 1.0E-4) {
                  return 0.0F;
               }

               double targetAngle = Math.atan2(dz, dx) / (Math.PI * 2);
               double entityYaw = Mth.m_14109_(entity.m_146908_() / 360.0, 1.0);
               return (float)Mth.m_14109_(0.5 - (entityYaw - 0.25 - targetAngle), 1.0);
            } else {
               return fallbackSpin(level);
            }
         } else {
            return fallbackSpin(level);
         }
      } else {
         return fallbackSpin(level);
      }
   }

   private static boolean isExtractionCompass(ItemStack stack) {
      if (stack != null && !stack.m_41619_() && stack.m_150930_(Items.f_220211_)) {
         CompoundTag tag = stack.m_41783_();
         return tag != null && tag.m_128471_("tactical_event_item") && "extraction_point".equals(tag.m_128461_("event_type"));
      } else {
         return false;
      }
   }

   private static float fallbackSpin(ClientLevel level) {
      return level == null ? 0.0F : (float)(level.m_46467_() % 32L) / 32.0F;
   }
}
