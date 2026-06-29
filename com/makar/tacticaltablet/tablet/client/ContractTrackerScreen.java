package com.makar.tacticaltablet.tablet.client;

import com.makar.tacticaltablet.tablet.net.ContractSelectTargetPacket;
import com.makar.tacticaltablet.tablet.net.ContractSelectionStatePacket;
import com.makar.tacticaltablet.tablet.net.ContractTrackerStatePacket;
import com.makar.tacticaltablet.tablet.net.PacketHandler;
import com.makar.tacticaltablet.tablet.net.TrackerWatchPacket;
import com.mojang.blaze3d.systems.RenderSystem;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public class ContractTrackerScreen extends Screen {
   private static final ResourceLocation PANEL = new ResourceLocation("tacticaltablet", "textures/gui/contract_gui.png");
   private static final int UI_WIDTH = 220;
   private static final int UI_HEIGHT = 300;
   private static final int MAP_SIZE = 148;
   private int lastTargetCount = -1;
   private boolean lastSelectionMode;
   private boolean watching;

   public ContractTrackerScreen() {
      super(Component.m_237113_("Контрактный трекер"));
   }

   protected void m_7856_() {
      this.startWatching();
      this.rebuildSelectionButtons();
   }

   public void m_7861_() {
      this.stopWatching();
      super.m_7861_();
   }

   public boolean m_7043_() {
      return false;
   }

   public void m_86600_() {
      boolean selectionMode = this.isSelectionMode();
      int targetCount = ContractClientState.getTargets().size();
      if (selectionMode != this.lastSelectionMode || targetCount != this.lastTargetCount) {
         this.rebuildSelectionButtons();
      }
   }

   public void m_88315_(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
      this.m_280273_(g);
      int x = (this.f_96543_ - 220) / 2;
      int y = (this.f_96544_ - 300) / 2;
      RenderSystem.setShader(GameRenderer::m_172817_);
      RenderSystem.setShaderTexture(0, PANEL);
      g.m_280163_(PANEL, x, y, 0.0F, 0.0F, 220, 300, 220, 300);
      if (!ContractClientState.isTrackerActive()) {
         this.drawSelection(g, x, y);
         super.m_88315_(g, mouseX, mouseY, partialTick);
      } else {
         this.drawInfo(g, x, y);
         this.drawContractMap(g, x + 36, y + 110, 148);
         super.m_88315_(g, mouseX, mouseY, partialTick);
      }
   }

   private boolean isSelectionMode() {
      return !ContractClientState.isTrackerActive()
         && ContractClientState.isSoloMode()
         && ContractClientState.isSelectionActive()
         && !ContractClientState.hasActiveContract();
   }

   private void startWatching() {
      if (!this.watching) {
         this.watching = true;
         if (this.canSendWatchPacket()) {
            PacketHandler.sendToServer(new TrackerWatchPacket(true));
         }
      }
   }

   private void stopWatching() {
      if (this.watching) {
         this.watching = false;
         if (this.canSendWatchPacket()) {
            PacketHandler.sendToServer(new TrackerWatchPacket(false));
         }
      }
   }

   private boolean canSendWatchPacket() {
      return Minecraft.m_91087_().m_91403_() != null;
   }

   private void rebuildSelectionButtons() {
      this.m_169413_();
      this.lastSelectionMode = this.isSelectionMode();
      this.lastTargetCount = ContractClientState.getTargets().size();
      if (this.lastSelectionMode) {
         int x = (this.f_96543_ - 220) / 2;
         int y = (this.f_96544_ - 300) / 2;
         List<ContractSelectionStatePacket.TargetEntry> targets = ContractClientState.getTargets();
         int count = Math.min(8, targets.size());

         for (int i = 0; i < count; i++) {
            ContractSelectionStatePacket.TargetEntry target = targets.get(i);
            this.m_142416_(new ContractTrackerScreen.TrackerTargetButton(x + 28, y + 78 + i * 19, 164, 16, target));
         }
      }
   }

   private void drawSelection(GuiGraphics g, int x, int y) {
      Font font = Minecraft.m_91087_().f_91062_;
      g.m_280137_(font, "КОНТРАКТЫ", x + 110, y + 18, -6306966);
      if (!ContractClientState.isSoloMode()) {
         g.m_280137_(font, "Контракты доступны только в соло.", x + 110, y + 132, -9882);
      } else if (ContractClientState.hasActiveContract()) {
         g.m_280137_(font, "Контракт активен.", x + 110, y + 126, -10027162);
         g.m_280137_(font, "Открой трекер еще раз.", x + 110, y + 142, -5592406);
      } else if (!ContractClientState.isSelectionActive()) {
         g.m_280137_(font, "Выбор контракта закрыт.", x + 110, y + 132, -5592406);
      } else {
         g.m_280137_(font, "Осталось: " + ContractClientState.getSelectionSecondsLeft() + "с", x + 110, y + 44, -5592406);
         long cooldown = ContractClientState.getCooldownLeftMs();
         if (cooldown > 0L) {
            g.m_280137_(font, "Трекер: " + this.formatTime(cooldown), x + 110, y + 56, -9882);
         }

         if (ContractClientState.getTargets().isEmpty()) {
            g.m_280137_(font, "Нет доступных целей.", x + 110, y + 132, -5592406);
         } else {
            g.m_280137_(font, "Выбери одну цель", x + 110, cooldown > 0L ? y + 68 : y + 60, -3355444);
         }
      }
   }

   private void drawInfo(GuiGraphics g, int x, int y) {
      Font font = Minecraft.m_91087_().f_91062_;
      int left = x + 34;
      int top = y + 35;
      List<ContractTrackerStatePacket.TargetEntry> targets = ContractClientState.getTrackerTargets();
      g.m_280137_(font, "КОНТРАКТЫ КОМАНДЫ", x + 110, y + 18, -6306966);
      int visible = Math.min(4, targets.size());

      for (int i = 0; i < visible; i++) {
         ContractTrackerStatePacket.TargetEntry target = targets.get(i);
         int color = ContractClientState.difficultyColor(target.difficulty());
         String line = i + 1 + ". " + target.name() + " | " + target.selectedClass() + " | " + target.reward();
         g.m_280056_(font, font.m_92834_(line, 152), left, top + i * 13, color, false);
      }

      if (targets.size() > visible) {
         g.m_280056_(font, "+" + (targets.size() - visible) + " целей", left, top + visible * 13, -5592406, false);
      }

      g.m_280056_(font, "Сигнал: " + ContractClientState.getSignalSecondsLeft() + "с", left, top + 60, -6306966, false);
   }

   private void drawContractMap(GuiGraphics g, int mapX, int mapY, int size) {
      g.m_280509_(mapX, mapY, mapX + size, mapY + size, -1442510843);
      int gridColor = 1712596495;

      for (int i = 1; i < 4; i++) {
         int p = mapX + i * size / 4;
         g.m_280509_(p, mapY + 1, p + 1, mapY + size - 1, gridColor);
         int q = mapY + i * size / 4;
         g.m_280509_(mapX + 1, q, mapX + size - 1, q + 1, gridColor);
      }

      this.drawRectOutline(g, mapX, mapY, size, size, -5592406);
      this.drawRectOutline(g, mapX + 2, mapY + 2, size - 4, size - 4, -3355444);
      Font font = Minecraft.m_91087_().f_91062_;
      g.m_280137_(font, "N", mapX + size / 2, mapY - 10, -5592406);
      g.m_280137_(font, "S", mapX + size / 2, mapY + size + 3, -5592406);
      g.m_280056_(font, "W", mapX - 10, mapY + size / 2 - 4, -5592406, false);
      g.m_280056_(font, "E", mapX + size + 4, mapY + size / 2 - 4, -5592406, false);
      int targetIndex = 0;

      for (ContractTrackerStatePacket.TargetEntry target : ContractClientState.getTrackerTargets()) {
         int targetX = this.toMapX(mapX, size, target.areaX());
         int targetY = this.toMapY(mapY, size, target.areaZ());
         int targetRadius = Math.max(5, Math.min(18, Math.round(target.areaRadius() / (ContractClientState.getZoneRadius() * 2.0F) * size)));
         int fillColor = targetIndex % 2 == 0 ? 1727996450 : 1728041503;
         int outlineColor = targetIndex % 2 == 0 ? -1426107051 : -1426063531;
         this.drawFilledCircle(g, targetX, targetY, targetRadius, fillColor);
         this.drawCircleOutline(g, targetX, targetY, targetRadius, outlineColor);
         targetIndex++;
      }

      int playerX = this.toMapX(mapX, size, ContractClientState.getPlayerX());
      int playerY = this.toMapY(mapY, size, ContractClientState.getPlayerZ());
      this.drawDot(g, playerX, playerY, 3, -1);
   }

   private int toMapX(int mapX, int size, int worldX) {
      int minX = ContractClientState.getZoneCenterX() - ContractClientState.getZoneRadius();
      int maxX = ContractClientState.getZoneCenterX() + ContractClientState.getZoneRadius();
      return mapX + Math.round((float)(worldX - minX) / Math.max(1, maxX - minX) * size);
   }

   private int toMapY(int mapY, int size, int worldZ) {
      int minZ = ContractClientState.getZoneCenterZ() - ContractClientState.getZoneRadius();
      int maxZ = ContractClientState.getZoneCenterZ() + ContractClientState.getZoneRadius();
      return mapY + Math.round((float)(worldZ - minZ) / Math.max(1, maxZ - minZ) * size);
   }

   private void drawDot(GuiGraphics g, int cx, int cy, int r, int color) {
      g.m_280509_(cx - r, cy - r, cx + r + 1, cy + r + 1, color);
   }

   private void drawRectOutline(GuiGraphics g, int x, int y, int w, int h, int color) {
      g.m_280509_(x, y, x + w, y + 1, color);
      g.m_280509_(x, y + h - 1, x + w, y + h, color);
      g.m_280509_(x, y, x + 1, y + h, color);
      g.m_280509_(x + w - 1, y, x + w, y + h, color);
   }

   private void drawFilledCircle(GuiGraphics g, int cx, int cy, int r, int color) {
      for (int dy = -r; dy <= r; dy++) {
         int dx = (int)Math.sqrt(r * r - dy * dy);
         g.m_280509_(cx - dx, cy + dy, cx + dx + 1, cy + dy + 1, color);
      }
   }

   private void drawCircleOutline(GuiGraphics g, int cx, int cy, int r, int color) {
      for (int a = 0; a < 360; a += 8) {
         double rad = Math.toRadians(a);
         int x = cx + (int)Math.round(Math.cos(rad) * r);
         int y = cy + (int)Math.round(Math.sin(rad) * r);
         g.m_280509_(x, y, x + 1, y + 1, color);
      }
   }

   private String formatTime(long ms) {
      long totalSec = Math.max(0L, (ms + 999L) / 1000L);
      long m = totalSec / 60L;
      long s = totalSec % 60L;
      return String.format("%02d:%02d", m, s);
   }

   private static class TrackerTargetButton extends Button {
      private final ContractSelectionStatePacket.TargetEntry target;

      private TrackerTargetButton(int x, int y, int w, int h, ContractSelectionStatePacket.TargetEntry target) {
         super(Button.m_253074_(Component.m_237113_(target.name()), button -> {}).m_252987_(x, y, w, h));
         this.target = target;
         this.f_93623_ = TabletClientState.getCoins() >= target.price();
      }

      public void m_5691_() {
         if (this.f_93623_) {
            PacketHandler.sendToServer(new ContractSelectTargetPacket(this.target.uuid()));
         }
      }

      public void m_87963_(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
         boolean hover = this.m_5953_(mouseX, mouseY);
         int border = ContractClientState.difficultyColor(this.target.difficulty());
         int bg = hover && this.f_93623_ ? 1429423138 : 855638016;
         int color = this.f_93623_ ? border : -11184811;
         g.m_280509_(this.m_252754_(), this.m_252907_(), this.m_252754_() + this.f_93618_, this.m_252907_() + this.f_93619_, bg);
         g.m_280509_(this.m_252754_(), this.m_252907_(), this.m_252754_() + this.f_93618_, this.m_252907_() + 1, border);
         g.m_280509_(this.m_252754_(), this.m_252907_() + this.f_93619_ - 1, this.m_252754_() + this.f_93618_, this.m_252907_() + this.f_93619_, border);
         String text = this.target.name() + " | " + this.target.price() + " -> " + this.target.reward() + " | " + this.target.careerPercent() + "%";
         g.m_280056_(Minecraft.m_91087_().f_91062_, text, this.m_252754_() + 4, this.m_252907_() + 4, color, false);
      }
   }
}
