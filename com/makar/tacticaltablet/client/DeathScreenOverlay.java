package com.makar.tacticaltablet.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;

public final class DeathScreenOverlay extends Screen {
   private static final int TEXT_COLOR = -52429;
   private static final ResourceLocation SAD_THROMBONE = new ResourceLocation("tacticaltablet", "sad_thrombone");
   private final String titleText;
   private final String subtitleText;
   private int ticksLeft;

   private DeathScreenOverlay(String title, String subtitle, int durationTicks) {
      super(Component.m_237119_());
      this.titleText = title == null ? "" : title;
      this.subtitleText = subtitle == null ? "" : subtitle;
      this.ticksLeft = Math.max(1, durationTicks);
   }

   public static void show(String newTitle, String newSubtitle, int durationTicks) {
      show(newTitle, newSubtitle, durationTicks, false);
   }

   public static void show(String newTitle, String newSubtitle, int durationTicks, boolean playSadTrombone) {
      Minecraft minecraft = Minecraft.m_91087_();
      if (playSadTrombone) {
         minecraft.m_91106_().m_120367_(SimpleSoundInstance.m_119755_(SoundEvent.m_262824_(SAD_THROMBONE), 1.0F, 1.0F));
      }

      minecraft.m_91152_(new DeathScreenOverlay(newTitle, newSubtitle, durationTicks));
   }

   public static boolean isActive() {
      return Minecraft.m_91087_().f_91080_ instanceof DeathScreenOverlay;
   }

   public void m_86600_() {
      if (--this.ticksLeft <= 0 && this.f_96541_ != null && this.f_96541_.f_91080_ == this) {
         this.f_96541_.m_91152_(null);
      }
   }

   public void m_88315_(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
      graphics.m_280509_(0, 0, this.f_96543_, this.f_96544_, -16777216);
      graphics.m_280168_().m_85836_();
      graphics.m_280168_().m_252880_(this.f_96543_ / 2.0F, this.f_96544_ / 2.0F - 18.0F, 0.0F);
      graphics.m_280168_().m_85841_(2.0F, 2.0F, 1.0F);
      graphics.m_280137_(this.f_96547_, this.titleText, 0, 0, -52429);
      graphics.m_280168_().m_85849_();
      graphics.m_280137_(this.f_96547_, this.subtitleText, this.f_96543_ / 2, this.f_96544_ / 2 + 12, -52429);
   }

   public boolean m_6913_() {
      return false;
   }

   public boolean m_7043_() {
      return false;
   }

   public boolean m_7933_(int keyCode, int scanCode, int modifiers) {
      return true;
   }

   public boolean m_6375_(double mouseX, double mouseY, int button) {
      return true;
   }
}
