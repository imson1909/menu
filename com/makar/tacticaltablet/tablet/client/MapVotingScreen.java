package com.makar.tacticaltablet.tablet.client;

import com.makar.tacticaltablet.game.MatchPhase;
import com.makar.tacticaltablet.tablet.net.PacketHandler;
import com.makar.tacticaltablet.tablet.net.SetClanWarPacket;
import com.makar.tacticaltablet.tablet.net.SetCompetitivePacket;
import com.makar.tacticaltablet.tablet.net.VoteMapPacket;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class MapVotingScreen extends Screen {
   private static final int PANEL_W = 366;
   private static final int PANEL_H = 268;
   private static final int CARD_W = 110;
   private static final int CARD_H = 68;
   private static final int IMAGE_SIZE = 22;
   private static final int GAP_X = 6;
   private static final int GAP_Y = 4;
   private Button competitiveToggleButton;
   private Button clanWarToggleButton;

   public MapVotingScreen() {
      super(Component.m_237113_("Выбор карты"));
   }

   protected void m_7856_() {
      this.m_169413_();
      List<String> maps = MapVoteClientState.getMaps();
      if (!maps.isEmpty()) {
         int panelX = (this.f_96543_ - 366) / 2;
         int panelY = (this.f_96544_ - 268) / 2;
         int columns = Math.min(3, maps.size());
         int gridWidth = columns * 110 + Math.max(0, columns - 1) * 6;
         int gridX = panelX + (366 - gridWidth) / 2;
         int gridY = panelY + 30;

         for (int i = 0; i < maps.size(); i++) {
            int column = i % columns;
            int row = i / columns;
            this.m_142416_(new MapVotingScreen.MapCardButton(gridX + column * 116, gridY + row * 72, maps.get(i)));
         }

         if (MapVoteClientState.isOperator()) {
            int toggleW = 172;
            int toggleGap = 8;
            int totalToggleW = toggleW * 2 + toggleGap;
            int toggleX = panelX + (366 - totalToggleW) / 2;
            int toggleY = panelY + 268 - 22;
            this.competitiveToggleButton = (Button)this.m_142416_(
               Button.m_253074_(
                     competitiveToggleLabel(), button -> PacketHandler.sendToServer(new SetCompetitivePacket(!MapVoteClientState.isNextSetCompetitive()))
                  )
                  .m_252987_(toggleX, toggleY, toggleW, 18)
                  .m_253136_()
            );
            this.clanWarToggleButton = (Button)this.m_142416_(
               Button.m_253074_(clanWarToggleLabel(), button -> PacketHandler.sendToServer(new SetClanWarPacket(!MapVoteClientState.isNextSetClanWar())))
                  .m_252987_(toggleX + toggleW + toggleGap, toggleY, toggleW, 18)
                  .m_253136_()
            );
         }
      }
   }

   public void m_86600_() {
      if (this.competitiveToggleButton != null) {
         this.competitiveToggleButton.m_93666_(competitiveToggleLabel());
      }

      if (this.clanWarToggleButton != null) {
         this.clanWarToggleButton.m_93666_(clanWarToggleLabel());
      }

      if (TabletClientState.getMatchPhase() != MatchPhase.MAP_VOTING || !MapVoteClientState.isActive()) {
         Minecraft.m_91087_().m_91152_(null);
      }
   }

   public void m_88315_(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
      this.m_280273_(graphics);
      graphics.m_280509_(0, 0, this.f_96543_, this.f_96544_, -1442840576);
      int x = (this.f_96543_ - 366) / 2;
      int y = (this.f_96544_ - 268) / 2;
      graphics.m_280509_(x, y, x + 366, y + 268, -267249902);
      drawBorder(graphics, x, y, 366, 268, -10027162);
      graphics.m_280137_(this.f_96547_, "ВЫБЕРИТЕ СЛЕДУЮЩУЮ КАРТУ", this.f_96543_ / 2, y + 3, -10027162);
      graphics.m_280137_(this.f_96547_, "До завершения голосования: " + MapVoteClientState.getSecondsLeft() + " сек.", this.f_96543_ / 2, y + 15, -1);
      if (MapVoteClientState.isNextSetCompetitive() && !MapVoteClientState.isOperator()) {
         graphics.m_280488_(this.f_96547_, "Следующий сет: СОРЕВНОВАТЕЛЬНЫЙ", x + 8, y + 268 - 20, -22016);
      }

      if (MapVoteClientState.isNextSetClanWar() && !MapVoteClientState.isOperator()) {
         graphics.m_280488_(this.f_96547_, "Следующий сет: ВОЙНА КЛАНОВ", x + 8, y + 268 - 20, -22016);
      }

      super.m_88315_(graphics, mouseX, mouseY, partialTick);
   }

   public boolean m_6913_() {
      return false;
   }

   public boolean m_7043_() {
      return false;
   }

   private static void drawBorder(GuiGraphics graphics, int x, int y, int w, int h, int color) {
      graphics.m_280509_(x, y, x + w, y + 1, color);
      graphics.m_280509_(x, y + h - 1, x + w, y + h, color);
      graphics.m_280509_(x, y, x + 1, y + h, color);
      graphics.m_280509_(x + w - 1, y, x + w, y + h, color);
   }

   private static Component competitiveToggleLabel() {
      return Component.m_237113_("Соревновательный режим [" + (MapVoteClientState.isNextSetCompetitive() ? "X" : " ") + "]");
   }

   private static Component clanWarToggleLabel() {
      return Component.m_237113_("Война кланов [" + (MapVoteClientState.isNextSetClanWar() ? "X" : " ") + "]");
   }

   private static final class MapCardButton extends Button {
      private final String mapName;

      private MapCardButton(int x, int y, String mapName) {
         super(Button.m_253074_(Component.m_237113_(mapName), ignored -> {}).m_252987_(x, y, 110, 68));
         this.mapName = mapName;
      }

      public void m_5691_() {
         PacketHandler.sendToServer(new VoteMapPacket(this.mapName));
      }

      public void m_87963_(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
         boolean selected = this.mapName.equals(MapVoteClientState.getSelectedMap());
         boolean hovered = this.m_5953_(mouseX, mouseY);
         int background = selected ? -584155854 : (hovered ? -584698330 : -585687785);
         int border = selected ? -1 : (hovered ? -5570646 : -10027162);
         graphics.m_280509_(this.m_252754_(), this.m_252907_(), this.m_252754_() + this.f_93618_, this.m_252907_() + this.f_93619_, background);
         MapVotingScreen.drawBorder(graphics, this.m_252754_(), this.m_252907_(), this.f_93618_, this.f_93619_, border);
         int imageX = this.m_252754_() + (this.f_93618_ - 22) / 2;
         int imageY = this.m_252907_() + 2;
         graphics.m_280509_(imageX, imageY, imageX + 22, imageY + 22, -1);
         MapVotingScreen.drawBorder(graphics, imageX, imageY, 22, 22, -3355444);
         graphics.m_280137_(Minecraft.m_91087_().f_91062_, this.mapName, this.m_252754_() + this.f_93618_ / 2, this.m_252907_() + 27, selected ? -1 : -1638426);
         String[] builders = builderLines(this.mapName);
         if (builders.length > 0) {
            graphics.m_280137_(Minecraft.m_91087_().f_91062_, builders[0], this.m_252754_() + this.f_93618_ / 2, this.m_252907_() + 38, -4473925);
         }

         if (builders.length > 1) {
            graphics.m_280137_(Minecraft.m_91087_().f_91062_, builders[1], this.m_252754_() + this.f_93618_ / 2, this.m_252907_() + 47, -4473925);
         }

         graphics.m_280137_(
            Minecraft.m_91087_().f_91062_,
            "Голосов: " + MapVoteClientState.getVoteCount(this.mapName),
            this.m_252754_() + this.f_93618_ / 2,
            this.m_252907_() + (builders.length > 0 ? 58 : 46),
            -4473925
         );
      }

      private static String[] builderLines(String mapName) {
         if ("Дикий Запад".equals(mapName)) {
            return new String[]{"Строители Илюха", "и ZumaDeluxe"};
         } else {
            return "Глубокая пещера".equals(mapName) ? new String[]{"Строители Xeno", "и SADER"} : new String[0];
         }
      }
   }
}
