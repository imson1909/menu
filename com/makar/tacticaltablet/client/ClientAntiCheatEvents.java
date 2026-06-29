package com.makar.tacticaltablet.client;

import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent.ClientTickEvent;
import net.minecraftforge.event.TickEvent.Phase;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

@EventBusSubscriber(modid = "tacticaltablet", value = Dist.CLIENT)
public class ClientAntiCheatEvents {
   @SubscribeEvent
   public static void onClientTick(ClientTickEvent event) {
      if (event.phase == Phase.END) {
         Minecraft mc = Minecraft.m_91087_();
         if (mc.f_91074_ != null) {
            if (mc.f_91073_ != null) {
               if (mc.m_91290_().m_114377_()) {
                  mc.m_91290_().m_114473_(false);
               }

               if (mc.f_91066_.f_92063_ || mc.f_91066_.f_92064_ || mc.f_91066_.f_92065_) {
                  mc.f_91066_.f_92063_ = false;
                  mc.f_91066_.f_92064_ = false;
                  mc.f_91066_.f_92065_ = false;
               }

               if (mc.f_91066_.m_92176_() != CameraType.FIRST_PERSON) {
                  mc.f_91066_.m_92157_(CameraType.FIRST_PERSON);
               }
            }
         }
      }
   }
}
