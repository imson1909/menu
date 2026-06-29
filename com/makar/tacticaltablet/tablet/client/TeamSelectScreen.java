package com.makar.tacticaltablet.tablet.client;

import com.makar.tacticaltablet.game.MatchPhase;
import com.makar.tacticaltablet.game.team.TeamId;
import com.makar.tacticaltablet.tablet.net.JoinTeamPacket;
import com.makar.tacticaltablet.tablet.net.PacketHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;

public class TeamSelectScreen extends Screen {
   private static final ResourceLocation CLICK = new ResourceLocation("tacticaltablet", "click");
   private static final ResourceLocation HOVER = new ResourceLocation("tacticaltablet", "hover");
   private static final float GUI_SOUND_VOLUME = 0.0625F;
   private static final int PANEL_W = 320;
   private static final int PANEL_H = 196;
   private static final int TEAM_CARD_W = 138;
   private static final int TEAM_CARD_H = 62;
   private static final int TEAM_CARD_GAP_X = 8;
   private static final int TEAM_CARD_GAP_Y = 8;

   public TeamSelectScreen() {
      super(Component.m_237113_("Выбор команды"));
   }

   protected void m_7856_() {
      this.m_169413_();
      int x0 = (this.f_96543_ - 320) / 2;
      int y0 = (this.f_96544_ - 196) / 2;
      int startX = x0 + 18;
      int startY = y0 + 48;
      TeamId[] teams = TeamId.values();

      for (int i = 0; i < teams.length; i++) {
         int x = startX + i % 2 * 146;
         int y = startY + i / 2 * 70;
         this.m_142416_(new TeamSelectScreen.TeamButton(x, y, teams[i]));
      }
   }

   public void m_86600_() {
      MatchPhase phase = TabletClientState.getMatchPhase();
      if (phase == MatchPhase.VOTING) {
         Minecraft.m_91087_().m_91152_(new VotingScreen());
      } else {
         if (phase != MatchPhase.TEAM_SELECT) {
            Minecraft.m_91087_().m_91152_(null);
         }
      }
   }

   public void m_88315_(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
      this.m_280273_(g);
      g.m_280509_(0, 0, this.f_96543_, this.f_96544_, -1728053248);
      int x = (this.f_96543_ - 320) / 2;
      int y = (this.f_96544_ - 196) / 2;
      drawPanel(g, x, y);
      Minecraft minecraft = Minecraft.m_91087_();
      g.m_280137_(minecraft.f_91062_, "ВЫБОР КОМАНДЫ", x + 160, y + 14, -10027162);
      g.m_280137_(minecraft.f_91062_, "Осталось: " + TabletClientState.getTeamSelectTimeLeft() + " сек.", x + 160, y + 30, -1);
      g.m_280137_(minecraft.f_91062_, "Выбери слот. Пустые места заполнит автобаланс.", x + 160, y + 196 - 16, -5592406);
      super.m_88315_(g, mouseX, mouseY, partialTick);
   }

   public boolean m_6913_() {
      return false;
   }

   public boolean m_7043_() {
      return false;
   }

   private static void drawPanel(GuiGraphics g, int x, int y) {
      g.m_280509_(x + 4, y + 4, x + 320 + 4, y + 196 + 4, 1711276032);
      g.m_280509_(x, y, x + 320, y + 196, -351793654);
      g.m_280509_(x + 1, y + 1, x + 320 - 1, y + 3, -10682512);
      g.m_280509_(x + 1, y + 196 - 3, x + 320 - 1, y + 196 - 1, -10682512);
      g.m_280509_(x + 1, y + 1, x + 3, y + 196 - 1, -10682512);
      g.m_280509_(x + 320 - 3, y + 1, x + 320 - 1, y + 196 - 1, -10682512);
      g.m_280509_(x + 6, y + 6, x + 320 - 6, y + 196 - 6, 856825876);
   }

   private static void playSound(ResourceLocation sound) {
      Minecraft.m_91087_().m_91106_().m_120367_(SimpleSoundInstance.m_119755_(SoundEvent.m_262824_(sound), 1.0F, 0.0625F));
   }

   private static String fitName(Minecraft minecraft, String name, int maxWidth) {
      if (minecraft.f_91062_.m_92895_(name) <= maxWidth) {
         return name;
      }

      String suffix = "...";
      int suffixWidth = minecraft.f_91062_.m_92895_(suffix);
      String result = name;

      while (!result.isEmpty() && minecraft.f_91062_.m_92895_(result) + suffixWidth > maxWidth) {
         result = result.substring(0, result.length() - 1);
      }

      return result.isEmpty() ? suffix : result + suffix;
   }

   private static class TeamButton extends Button {
      private final TeamId team;
      private boolean wasHovered;

      private TeamButton(int x, int y, TeamId team) {
         super(Button.m_253074_(Component.m_237113_(team.displayName()), button -> {}).m_252987_(x, y, 138, 62));
         this.team = team;
      }

      public void m_5691_() {
         TeamSelectScreen.playSound(TeamSelectScreen.CLICK);
         PacketHandler.sendToServer(new JoinTeamPacket(this.team));
      }

      public void m_7435_(SoundManager soundManager) {
      }

      public void m_87963_(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
         boolean hover = this.m_5953_(mouseX, mouseY);
         boolean selected = TabletClientState.getSelectedTeam() == this.team.ordinal();
         if (hover && !this.wasHovered) {
            TeamSelectScreen.playSound(TeamSelectScreen.HOVER);
         }

         this.wasHovered = hover;
         int border = selected ? -1 : this.team.textColor();
         int bg = selected ? -870699494 : (hover ? -870964458 : -871360496);
         g.m_280509_(this.m_252754_(), this.m_252907_(), this.m_252754_() + this.f_93618_, this.m_252907_() + this.f_93619_, bg);
         g.m_280509_(this.m_252754_(), this.m_252907_(), this.m_252754_() + this.f_93618_, this.m_252907_() + 1, border);
         g.m_280509_(this.m_252754_(), this.m_252907_() + this.f_93619_ - 1, this.m_252754_() + this.f_93618_, this.m_252907_() + this.f_93619_, border);
         g.m_280509_(this.m_252754_(), this.m_252907_(), this.m_252754_() + 1, this.m_252907_() + this.f_93619_, border);
         g.m_280509_(this.m_252754_() + this.f_93618_ - 1, this.m_252907_(), this.m_252754_() + this.f_93618_, this.m_252907_() + this.f_93619_, border);
         Minecraft minecraft = Minecraft.m_91087_();
         g.m_280137_(minecraft.f_91062_, this.team.displayName(), this.m_252754_() + this.f_93618_ / 2, this.m_252907_() + 5, this.team.textColor());
         int slots = Math.max(1, TabletClientState.getTeamSlotSize());

         for (int i = 0; i < slots; i++) {
            String name = TabletClientState.getTeamSlotName(this.team.ordinal(), i);
            boolean empty = name == null || name.isBlank();
            String label = empty ? "[ пусто ]" : TeamSelectScreen.fitName(minecraft, name, this.f_93618_ - 20);
            int color = empty ? -8947849 : -1644826;
            g.m_280056_(minecraft.f_91062_, label, this.m_252754_() + 10, this.m_252907_() + 18 + i * 8, color, false);
         }
      }
   }
}
