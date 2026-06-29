package com.makar.tacticaltablet.tablet.client;

import com.makar.tacticaltablet.game.MatchMode;
import com.makar.tacticaltablet.game.MatchPhase;
import com.makar.tacticaltablet.tablet.net.PacketHandler;
import com.makar.tacticaltablet.tablet.net.VoteModePacket;
import com.mojang.blaze3d.systems.RenderSystem;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;

public class VotingScreen extends Screen {
   public static final int PANEL_W = 220;
   public static final int PANEL_H = 118;
   public static final String PANEL_TEXTURE_PATH = "assets/tacticaltablet/textures/gui/vote_panel.png";
   private static final ResourceLocation PANEL = new ResourceLocation("tacticaltablet", "textures/gui/vote_panel.png");
   private static final ResourceLocation CLICK = new ResourceLocation("tacticaltablet", "click");
   private static final ResourceLocation HOVER = new ResourceLocation("tacticaltablet", "hover");
   private static final float GUI_SOUND_VOLUME = 0.0625F;
   private static final int BUTTON_H = 26;

   public VotingScreen() {
      super(Component.m_237113_("Голосование"));
   }

   protected void m_7856_() {
      this.m_169413_();
      List<MatchMode> modes = availableModes();
      if (!modes.isEmpty()) {
         int buttonW = buttonWidth(modes.size());
         int gap = modes.size() >= 4 ? 5 : 8;
         int totalW = buttonW * modes.size() + gap * (modes.size() - 1);
         int x = (this.f_96543_ - totalW) / 2;
         int y = (this.f_96544_ - 118) / 2 + 58;

         for (int i = 0; i < modes.size(); i++) {
            MatchMode mode = modes.get(i);
            this.m_142416_(new VotingScreen.VoteButton(x + i * (buttonW + gap), y, buttonW, mode));
         }
      }
   }

   public void m_86600_() {
      MatchPhase phase = TabletClientState.getMatchPhase();
      if (phase == MatchPhase.TEAM_SELECT) {
         Minecraft.m_91087_().m_91152_(new TeamSelectScreen());
      } else {
         if (phase != MatchPhase.VOTING) {
            Minecraft.m_91087_().m_91152_(null);
         }
      }
   }

   public void m_88315_(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
      this.m_280273_(g);
      g.m_280509_(0, 0, this.f_96543_, this.f_96544_, -1728053248);
      int x = (this.f_96543_ - 220) / 2;
      int y = (this.f_96544_ - 118) / 2;
      g.m_280509_(x + 4, y + 4, x + 220 + 4, y + 118 + 4, 1711276032);
      RenderSystem.setShader(GameRenderer::m_172817_);
      RenderSystem.setShaderTexture(0, PANEL);
      g.m_280163_(PANEL, x, y, 0.0F, 0.0F, 220, 118, 220, 118);
      Minecraft minecraft = Minecraft.m_91087_();
      g.m_280137_(minecraft.f_91062_, "ГОЛОСОВАНИЕ", x + 110, y + 14, -10027162);
      g.m_280137_(minecraft.f_91062_, "Осталось: " + TabletClientState.getVoteTimeLeft() + " сек.", x + 110, y + 30, -1);
      g.m_280137_(minecraft.f_91062_, "Голос засчитывается после нажатия", x + 110, y + 93, -5592406);
      super.m_88315_(g, mouseX, mouseY, partialTick);
   }

   public boolean m_6913_() {
      return false;
   }

   public boolean m_7043_() {
      return false;
   }

   private static List<MatchMode> availableModes() {
      List<MatchMode> modes = new ArrayList<>();

      for (MatchMode mode : MatchMode.values()) {
         if (TabletClientState.isVoteModeAvailable(mode)) {
            modes.add(mode);
         }
      }

      return modes;
   }

   private static int buttonWidth(int count) {
      if (count <= 2) {
         return 78;
      } else {
         return count == 3 ? 58 : 48;
      }
   }

   private static String shortLabel(MatchMode mode) {
      return switch (mode) {
         case SOLO -> "СОЛО";
         case DUO -> "ДУО";
         case TRIO -> "ТРИО";
         case SQUADS -> "ОТРЯДЫ";
      };
   }

   private static void playSound(ResourceLocation sound) {
      Minecraft.m_91087_().m_91106_().m_120367_(SimpleSoundInstance.m_119755_(SoundEvent.m_262824_(sound), 1.0F, 0.0625F));
   }

   private static class VoteButton extends Button {
      private final MatchMode mode;
      private boolean wasHovered;

      private VoteButton(int x, int y, int w, MatchMode mode) {
         super(Button.m_253074_(Component.m_237113_(VotingScreen.shortLabel(mode)), button -> {}).m_252987_(x, y, w, 26));
         this.mode = mode;
      }

      public void m_5691_() {
         VotingScreen.playSound(VotingScreen.CLICK);
         PacketHandler.sendToServer(new VoteModePacket(this.mode));
      }

      public void m_7435_(SoundManager soundManager) {
      }

      public void m_87963_(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
         boolean hover = this.m_5953_(mouseX, mouseY);
         boolean selected = TabletClientState.getSelectedVote() == this.mode;
         if (hover && !this.wasHovered) {
            VotingScreen.playSound(VotingScreen.HOVER);
         }

         this.wasHovered = hover;
         int fill = selected ? -869368526 : (hover ? -870041564 : -871360496);
         int border = selected ? -1 : (hover ? -1638426 : -10027162);
         int titleColor = selected ? -1 : (hover ? -1 : -10027162);
         g.m_280509_(this.m_252754_(), this.m_252907_(), this.m_252754_() + this.f_93618_, this.m_252907_() + this.f_93619_, fill);
         g.m_280509_(this.m_252754_(), this.m_252907_(), this.m_252754_() + this.f_93618_, this.m_252907_() + 1, border);
         g.m_280509_(this.m_252754_(), this.m_252907_() + this.f_93619_ - 1, this.m_252754_() + this.f_93618_, this.m_252907_() + this.f_93619_, border);
         g.m_280509_(this.m_252754_(), this.m_252907_(), this.m_252754_() + 1, this.m_252907_() + this.f_93619_, border);
         g.m_280509_(this.m_252754_() + this.f_93618_ - 1, this.m_252907_(), this.m_252754_() + this.f_93618_, this.m_252907_() + this.f_93619_, border);
         Minecraft minecraft = Minecraft.m_91087_();
         g.m_280137_(minecraft.f_91062_, VotingScreen.shortLabel(this.mode), this.m_252754_() + this.f_93618_ / 2, this.m_252907_() + 4, titleColor);
         g.m_280137_(
            minecraft.f_91062_,
            String.valueOf(TabletClientState.getVoteCount(this.mode)),
            this.m_252754_() + this.f_93618_ / 2,
            this.m_252907_() + 15,
            selected ? -1 : -5592406
         );
      }
   }
}
