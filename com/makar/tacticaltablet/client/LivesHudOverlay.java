package com.makar.tacticaltablet.client;

import com.makar.tacticaltablet.tablet.client.TabletClientState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent.Post;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

@EventBusSubscriber(modid = "tacticaltablet", value = Dist.CLIENT)
public final class LivesHudOverlay {
   private static final ResourceLocation HEART_TEXTURE = new ResourceLocation("tacticaltablet", "textures/gui/heart.png");
   private static final ResourceLocation PLAYERS_TEXTURE = new ResourceLocation("tacticaltablet", "textures/gui/players_count.png");
   private static final int HEART_SIZE = 16;
   private static final int HEART_TEXTURE_SIZE = 16;
   private static final int PLAYERS_SIZE = 16;
   private static final int PLAYERS_TEXTURE_SIZE = 32;
   private static final int HOTBAR_WIDTH = 182;
   private static final int HOTBAR_HEIGHT = 22;
   private static final int SIDE_PADDING = 6;
   private static final int COUNTER_GAP = 10;
   private static final int TEXT_OFFSET_X = 19;
   private static final int TEXT_OFFSET_Y = 5;

   private LivesHudOverlay() {
   }

   @SubscribeEvent
   public static void onRenderHotbar(Post event) {
      if (VanillaGuiOverlay.HOTBAR.id().equals(event.getOverlay().id())) {
         Minecraft minecraft = Minecraft.m_91087_();
         Player player = minecraft.f_91074_;
         if (player != null && !player.m_5833_() && !minecraft.f_91066_.f_92062_) {
            if (minecraft.f_91080_ == null) {
               if (TabletClientState.isGameRunning()) {
                  int lives = TabletClientState.getLives();
                  if (lives > 0) {
                     GuiGraphics graphics = event.getGuiGraphics();
                     int screenWidth = event.getWindow().m_85445_();
                     int screenHeight = event.getWindow().m_85446_();
                     int hotbarRight = screenWidth / 2 + 91;
                     int y = screenHeight - 22 + 3;
                     String livesText = "X" + lives;
                     int alivePlayers = TabletClientState.getAlivePlayers();
                     int remainingLivesTotal = TabletClientState.getRemainingLivesTotal();
                     String playersText = alivePlayers > 0 ? "X" + alivePlayers + " (" + remainingLivesTotal + ")" : "";
                     int livesWidth = 19 + minecraft.f_91062_.m_92895_(livesText);
                     int playersWidth = playersText.isEmpty() ? 0 : 19 + minecraft.f_91062_.m_92895_(playersText);
                     int totalWidth = livesWidth + (playersWidth > 0 ? 10 + playersWidth : 0);
                     int x = Math.min(hotbarRight + 6, screenWidth - totalWidth - 2);
                     x = Math.max(2, x);
                     graphics.m_280163_(HEART_TEXTURE, x, y, 0.0F, 0.0F, 16, 16, 16, 16);
                     graphics.m_280056_(minecraft.f_91062_, livesText, x + 19, y + 5, -1, true);
                     if (!playersText.isEmpty()) {
                        int playersX = x + livesWidth + 10;
                        graphics.m_280163_(PLAYERS_TEXTURE, playersX, y, 0.0F, 0.0F, 16, 16, 32, 32);
                        graphics.m_280056_(minecraft.f_91062_, playersText, playersX + 19, y + 5, -1, true);
                     }
                  }
               }
            }
         }
      }
   }
}
