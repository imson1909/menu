package com.makar.tacticaltablet.tablet.client;

import com.makar.tacticaltablet.clan.ClanAcceptJoinPacket;
import com.makar.tacticaltablet.clan.ClanConstants;
import com.makar.tacticaltablet.clan.ClanCreatePacket;
import com.makar.tacticaltablet.clan.ClanDisbandPacket;
import com.makar.tacticaltablet.clan.ClanJoinRequestPacket;
import com.makar.tacticaltablet.clan.ClanKickMemberPacket;
import com.makar.tacticaltablet.clan.ClanLeavePacket;
import com.makar.tacticaltablet.clan.ClanListPacket;
import com.makar.tacticaltablet.clan.ClanRejectJoinPacket;
import com.makar.tacticaltablet.progression.PlayerProgressManager;
import com.makar.tacticaltablet.tablet.net.PacketHandler;
import com.makar.tacticaltablet.tablet.net.TabletPacket;
import com.mojang.blaze3d.systems.RenderSystem;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.BooleanSupplier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.FormattedCharSequence;

public class TabletScreen extends Screen {
   private static final ResourceLocation PANEL = new ResourceLocation("tacticaltablet", "textures/gui/tablet.png");
   private static final ResourceLocation PANEL_EPIC = new ResourceLocation("tacticaltablet", "textures/gui/tablet_epic.png");
   private static final ResourceLocation PANEL_LEGEND = new ResourceLocation("tacticaltablet", "textures/gui/tablet_legend.png");
   private static final ResourceLocation BTN = new ResourceLocation("tacticaltablet", "textures/gui/button.png");
   private static final ResourceLocation BTN_HOVER = new ResourceLocation("tacticaltablet", "textures/gui/button_hover.png");
   private static final ResourceLocation BTN_DISABLED = new ResourceLocation("tacticaltablet", "textures/gui/button_disabled.png");
   private static final ResourceLocation BTN_EPIC = new ResourceLocation("tacticaltablet", "textures/gui/button_epic.png");
   private static final ResourceLocation BTN_EPIC_HOVER = new ResourceLocation("tacticaltablet", "textures/gui/button_hover_epic.png");
   private static final ResourceLocation BTN_EPIC_DISABLED = new ResourceLocation("tacticaltablet", "textures/gui/button_disabled_epic.png");
   private static final ResourceLocation BTN_LEGEND = new ResourceLocation("tacticaltablet", "textures/gui/button_legend.png");
   private static final ResourceLocation BTN_LEGEND_HOVER = new ResourceLocation("tacticaltablet", "textures/gui/button_hover_legend.png");
   private static final ResourceLocation BTN_LEGEND_DISABLED = new ResourceLocation("tacticaltablet", "textures/gui/button_disabled_legend.png");
   private static final ResourceLocation TP_BTN = new ResourceLocation("tacticaltablet", "textures/gui/tp_button.png");
   private static final ResourceLocation TP_BTN_HOVER = new ResourceLocation("tacticaltablet", "textures/gui/tp_button_hover.png");
   private static final ResourceLocation TP_BTN_DISABLED = new ResourceLocation("tacticaltablet", "textures/gui/tp_button_disabled.png");
   private static final ResourceLocation TAB_BTN = new ResourceLocation("tacticaltablet", "textures/gui/tab_button.png");
   private static final ResourceLocation TAB_BTN_HOVER = new ResourceLocation("tacticaltablet", "textures/gui/tab_button_hover.png");
   private static final ResourceLocation TAB_BTN_ACTIVE = new ResourceLocation("tacticaltablet", "textures/gui/tab_button_active.png");
   private static final ResourceLocation CONFIRM_PANEL = new ResourceLocation("tacticaltablet", "textures/gui/confirm_panel.png");
   private static final ResourceLocation CONFIRM_BUTTON = new ResourceLocation("tacticaltablet", "textures/gui/confirm_button.png");
   private static final ResourceLocation CONFIRM_BUTTON_HOVER = new ResourceLocation("tacticaltablet", "textures/gui/confirm_button_hover.png");
   private static final ResourceLocation CONFIRM_BUTTON_DISABLED = new ResourceLocation("tacticaltablet", "textures/gui/confirm_button_disabled.png");
   private static final ResourceLocation CLAN_ROW = new ResourceLocation("tacticaltablet", "textures/gui/clan_row.png");
   private static final ResourceLocation CLAN_ROW_HOVER = new ResourceLocation("tacticaltablet", "textures/gui/clan_row_hover.png");
   private static final ResourceLocation CLAN_ROW_ACTIVE = new ResourceLocation("tacticaltablet", "textures/gui/clan_row_active.png");
   private static final ResourceLocation CLAN_CREATE_BUTTON = new ResourceLocation("tacticaltablet", "textures/gui/clan_create_button.png");
   private static final ResourceLocation CLAN_CREATE_BUTTON_HOVER = new ResourceLocation("tacticaltablet", "textures/gui/clan_create_button_hover.png");
   private static final ResourceLocation CLAN_CREATE_BUTTON_DISABLED = new ResourceLocation("tacticaltablet", "textures/gui/clan_create_button_disabled.png");
   private static final ResourceLocation CLAN_BACK_BUTTON = new ResourceLocation("tacticaltablet", "textures/gui/clan_back_button.png");
   private static final ResourceLocation CLAN_BACK_BUTTON_HOVER = new ResourceLocation("tacticaltablet", "textures/gui/clan_back_button_hover.png");
   private static final ResourceLocation CLAN_ACTION_BUTTON = new ResourceLocation("tacticaltablet", "textures/gui/clan_action_button.png");
   private static final ResourceLocation CLAN_ACTION_BUTTON_HOVER = new ResourceLocation("tacticaltablet", "textures/gui/clan_action_button_hover.png");
   private static final ResourceLocation CLAN_ACTION_BUTTON_DISABLED = new ResourceLocation("tacticaltablet", "textures/gui/clan_action_button_disabled.png");
   private static final ResourceLocation CLAN_DANGER_BUTTON = new ResourceLocation("tacticaltablet", "textures/gui/clan_danger_button.png");
   private static final ResourceLocation CLAN_DANGER_BUTTON_HOVER = new ResourceLocation("tacticaltablet", "textures/gui/clan_danger_button_hover.png");
   private static final ResourceLocation CLAN_SMALL_BUTTON = new ResourceLocation("tacticaltablet", "textures/gui/clan_small_button.png");
   private static final ResourceLocation CLAN_SMALL_BUTTON_HOVER = new ResourceLocation("tacticaltablet", "textures/gui/clan_small_button_hover.png");
   private static final ResourceLocation CLICK = new ResourceLocation("tacticaltablet", "click");
   private static final ResourceLocation TELEPORT = new ResourceLocation("tacticaltablet", "teleport");
   private static final ResourceLocation HOVER = new ResourceLocation("tacticaltablet", "hover");
   private static final int UI_WIDTH = 380;
   private static final int UI_HEIGHT = 220;
   private static final int BTN_W = 160;
   private static final int BTN_H = 30;
   private static final int TAB_W = 66;
   private static final int TAB_H = 20;
   private static final int CONFIRM_W = 240;
   private static final int CONFIRM_H = 132;
   private static final int CONFIRM_BUTTON_W = 96;
   private static final int CONFIRM_BUTTON_H = 24;
   private static final int OFFSET_X = 23;
   private static final int OFFSET_Y = 46;
   private static final int BUTTON_GAP_X = 174;
   private static final int BUTTON_GAP_Y = 40;
   private static final int INFO_LEFT = 38;
   private static final int INFO_TOP = 72;
   private static final int INFO_WIDTH = 292;
   private static final int INFO_HEIGHT = 118;
   private static final int INFO_LINE_HEIGHT = 10;
   private static final int CLAN_LIST_LEFT = 48;
   private static final int CLAN_LIST_TOP = 74;
   private static final int CLAN_ROW_W = 284;
   private static final int CLAN_ROW_H = 22;
   private static final int CLAN_ROW_GAP = 24;
   private static final int CLAN_CREATE_W = 160;
   private static final int CLAN_CREATE_H = 30;
   private static final int CLAN_BACK_W = 76;
   private static final int CLAN_BACK_H = 22;
   private static final int CLAN_ACTION_W = 120;
   private static final int CLAN_ACTION_H = 24;
   private static final int CLAN_SMALL_W = 58;
   private static final int CLAN_SMALL_H = 18;
   private static final float GUI_SOUND_VOLUME = 0.0625F;
   private static final String SERVER_INFO_TEXT = "Добро пожаловать на сервер DeluxeWarfare!\n\nЯ ZumaDeluxe - создатель сервера. Возможно, ты видел меня в TikTok или на YouTube — рад видеть тебя здесь. Сервер ещё находится в разработке, но ты уже можешь играть, тестировать режимы и помогать нам делать проект лучше.\n\nГлавная цель режима — остаться последним выжившим игроком на карте. Это похоже на Battle Royale, но без системы лута. На карте есть безопасная зона, которая постепенно сужается и каждый новый матч центр оказывается в случайном месте.\n\nУ тебя есть планшет, который ты сейчас держишь в руке. Это твой личный помощник в игре. Через него ты сможешь смотреть свой прогресс, информацию о наборах и использовать основные игровые кнопки.\n\nКак начать игру?\n\nИгра начинается автоматически, если на сервере больше одного человека. Тебе выдадут планшет, в котором есть все необходимые кнопки. Для игры есть два типа кнопок: наборы (или классы) и телепорт (rtp). При старте у тебя есть 30 секунд на подготовку (ты находишься в безопасном лобби). Нажав на кнопку с названием класса, ты можешь его выбрать, после чего тебе выдадут соответствующее снаряжение. Затем кнопки классов станут недоступны и тебе придется телепортироваться в рандомное место на карте (кнопка rtp). Если ты этого не сделаешь, то тебя телепортирует автоматически. Главное что ты должен знать - после телепортации ты находишься в игре и тебя могут убить! У каждого игрока есть по три жизни. Потратив все жизни игрок выбывает.\n\nУдачи!\n";
   private static final TabletScreen.TabletAction STORMTROOPER = TabletScreen.TabletAction.classKit("ШТУРМОВИК", "stormtrooper", 0);
   private static final TabletScreen.TabletAction SNIPER = TabletScreen.TabletAction.classKit("СНАЙПЕР", "sniper", 1);
   private static final TabletScreen.TabletAction SCOUT = TabletScreen.TabletAction.classKit("РАЗВЕДЧИК", "scout", 2);
   private static final TabletScreen.TabletAction DRONE_OPERATOR = TabletScreen.TabletAction.classKit("ДРОН ОП.", "droneoperator", 3);
   private static final TabletScreen.TabletAction MORTARMAN = TabletScreen.TabletAction.classKit("МИНОМЁТЧИК", "mortarman", 5);
   private static final TabletScreen.TabletAction TELEPORT_RTP = TabletScreen.TabletAction.rtp("ТЕЛЕПОРТ (RTP)", 7);
   private static final TabletScreen.TabletAction MACHINE_GUNNER = TabletScreen.TabletAction.classKit("ПУЛЕМЁТЧИК", "machinegunner", 8);
   private static final TabletScreen.TabletAction RPG_TROOPER = TabletScreen.TabletAction.classKit("РПГ-БОЕЦ", "rpgtrooper", 9);
   private static final TabletScreen.TabletAction BOOMGUY_SHOP = TabletScreen.TabletAction.shopClass("ПОДРЫВНИК", "boomguy", 4, 500, 2);
   private static final TabletScreen.TabletAction DREAM_SHOP = TabletScreen.TabletAction.shopClass("ДРИМ", "dream", 6, 500, 2);
   private static final TabletScreen.TabletAction TAGILLA_SHOP = TabletScreen.TabletAction.shopClass("ТАГИЛЛА", "tagilla", 10, 750, 2);
   private static final TabletScreen.TabletAction BLACK_OPS_SHOP = TabletScreen.TabletAction.shopClass("СПЕЦНАЗ", "blackops", 11, 1000, 2);
   private static final TabletScreen.TabletAction COWBOY_SHOP = TabletScreen.TabletAction.shopClass("КОВБОЙ", "cowboy", 12, 100, 1);
   private static final TabletScreen.TabletAction SOLIDER_SHOP = TabletScreen.TabletAction.shopClass("СОЛДАТ", "solider", 13, 50, 0);
   private static final TabletScreen.TabletAction REBEL_SHOP = TabletScreen.TabletAction.shopClass("ПОВСТАНЕЦ", "rebel", 14, 1000, 2);
   private static final TabletScreen.TabletAction SABOTEUR_SHOP = TabletScreen.TabletAction.shopClass("ДИВЕРСАНТ", "saboteur", 15, 1000, 2);
   private static final TabletScreen.TabletAction KILLER_EXCLUSIVE = TabletScreen.TabletAction.exclusiveClass("КИЛЛЕР", "killer", 16);
   private static final TabletScreen.TabletAction MINIBOSS_EXCLUSIVE = TabletScreen.TabletAction.exclusiveClass("МИНИ-БОСС", "miniboss", 17);
   private static final TabletScreen.TabletAction SHAHED_EXCLUSIVE = TabletScreen.TabletAction.exclusiveClass("ШАХЕД ОП.", "shahed", 18, 2);
   private static final TabletScreen.TabletAction KROT_EXCLUSIVE = TabletScreen.TabletAction.exclusiveClass("КРОТ", "krot", 19, 1);
   private static final TabletScreen.TabletAction SOON = TabletScreen.TabletAction.locked("СКОРО...");
   private static final TabletScreen.TabletAction LOCKED = TabletScreen.TabletAction.locked("???");
   private static final TabletScreen.TabletPage[] PAGES = new TabletScreen.TabletPage[]{
      new TabletScreen.TabletPage(
         "КЛАССЫ",
         TabletScreen.PageType.ACTIONS,
         new TabletScreen.TabletAction[]{STORMTROOPER, SNIPER, SCOUT, DRONE_OPERATOR, MACHINE_GUNNER, MORTARMAN, RPG_TROOPER, TELEPORT_RTP}
      ),
      new TabletScreen.TabletPage("КЛАНЫ", TabletScreen.PageType.CLAN, new TabletScreen.TabletAction[0]),
      new TabletScreen.TabletPage("ПРОФИЛЬ", TabletScreen.PageType.PROFILE, new TabletScreen.TabletAction[0]),
      new TabletScreen.TabletPage(
         "МАГАЗИН",
         TabletScreen.PageType.ACTIONS,
         new TabletScreen.TabletAction[]{BOOMGUY_SHOP, DREAM_SHOP, TAGILLA_SHOP, BLACK_OPS_SHOP, COWBOY_SHOP, SOLIDER_SHOP, REBEL_SHOP, SABOTEUR_SHOP}
      ),
      new TabletScreen.TabletPage(
         "ЭКСКЛЮЗИВ",
         TabletScreen.PageType.ACTIONS,
         new TabletScreen.TabletAction[]{KILLER_EXCLUSIVE, MINIBOSS_EXCLUSIVE, SHAHED_EXCLUSIVE, KROT_EXCLUSIVE, SOON, SOON, SOON, SOON}
      )
   };
   private int currentPage;
   private int infoScroll;
   private int selectedClanIndex = -1;
   private int clanScrollOffset;
   private int pendingScrollOffset;
   private final Set<String> dismissedUpgradePrompts = new HashSet<>();

   public TabletScreen() {
      super(Component.m_237113_("ТАКТИЧЕСКИЙ ПЛАНШЕТ"));
   }

   protected void m_7856_() {
      this.m_169413_();
      int x0 = (this.f_96543_ - 380) / 2;
      int y0 = (this.f_96544_ - 220) / 2;
      this.addPageTabs(x0, y0);
      TabletScreen.TabletPage page = PAGES[this.currentPage];
      if (page.type() == TabletScreen.PageType.ACTIONS) {
         this.addActionButtons(x0, y0, page);
      } else if (page.type() == TabletScreen.PageType.CLAN) {
         this.addClanButtons(x0, y0);
      }
   }

   private void addPageTabs(int x0, int y0) {
      int totalWidth = PAGES.length * 66 + (PAGES.length - 1) * 6;
      int startX = x0 + (380 - totalWidth) / 2;
      int y = y0 + 17;

      for (int i = 0; i < PAGES.length; i++) {
         this.m_142416_(new TabletScreen.PageTabButton(startX + i * 72, y, i));
      }
   }

   private void addActionButtons(int x0, int y0, TabletScreen.TabletPage page) {
      TabletScreen.TabletAction[] actions = Arrays.copyOf(page.actions(), 8);

      for (int i = 0; i < actions.length; i++) {
         TabletScreen.TabletAction action = actions[i] == null ? LOCKED : actions[i];
         int x = x0 + 23 + i % 2 * 174;
         int y = y0 + 46 + i / 2 * 40;
         this.m_142416_(new TabletScreen.TabletActionButton(x, y, 160, 30, action));
      }
   }

   private void addClanButtons(int x0, int y0) {
      List<ClanListPacket.ClanEntry> clans = TabletClientState.getClans();
      this.selectedClanIndex = clamp(this.selectedClanIndex, -1, clans.size() - 1);
      this.clanScrollOffset = clamp(this.clanScrollOffset, 0, Math.max(0, clans.size() - 4));
      if (this.selectedClanIndex < 0) {
         int rowCount = Math.min(4, clans.size() - this.clanScrollOffset);

         for (int i = 0; i < rowCount; i++) {
            int index = this.clanScrollOffset + i;
            ClanListPacket.ClanEntry clan = clans.get(index);
            this.m_142416_(
               new TabletScreen.ClanTextureButton(
                  x0 + 48,
                  y0 + 74 + i * 24,
                  284,
                  22,
                  CLAN_ROW,
                  CLAN_ROW_HOVER,
                  CLAN_ROW_ACTIVE,
                  Component.m_237113_("[" + clan.tag() + "] " + clan.name()),
                  () -> {
                     this.selectedClanIndex = index;
                     this.pendingScrollOffset = 0;
                     this.m_7856_();
                  }
               )
            );
         }

         this.m_142416_(
            new TabletScreen.ClanTextureButton(
               x0 + 110,
               y0 + 184,
               160,
               30,
               CLAN_CREATE_BUTTON,
               CLAN_CREATE_BUTTON_HOVER,
               CLAN_CREATE_BUTTON_DISABLED,
               Component.m_237113_("Создать"),
               () -> Minecraft.m_91087_().m_91152_(new TabletScreen.ClanCreateConfirmScreen())
            )
         );
      } else {
         ClanListPacket.ClanEntry clan = clans.get(this.selectedClanIndex);
         this.m_142416_(
            new TabletScreen.ClanTextureButton(
               x0 + 38, y0 + 50, 76, 22, CLAN_BACK_BUTTON, CLAN_BACK_BUTTON_HOVER, CLAN_BACK_BUTTON, Component.m_237113_("Назад"), () -> {
                  this.selectedClanIndex = -1;
                  this.pendingScrollOffset = 0;
                  this.m_7856_();
               }
            )
         );
         if (!clan.owner() && !clan.member() && !clan.pending()) {
            this.m_142416_(
               this.newClanActionButton(x0 + 130, y0 + 184, "Вступить", () -> Minecraft.m_91087_().m_91152_(new TabletScreen.ClanJoinConfirmScreen(clan)))
            );
         } else if (clan.member() && !clan.owner()) {
            this.m_142416_(
               this.newClanDangerButton(
                  x0 + 130,
                  y0 + 184,
                  "Выйти",
                  () -> Minecraft.m_91087_()
                     .m_91152_(
                        new TabletScreen.ClanSimpleConfirmScreen("ВЫЙТИ ИЗ КЛАНА?", clan.name(), () -> PacketHandler.sendToServer(new ClanLeavePacket()))
                     )
               )
            );
         } else if (clan.owner()) {
            this.m_142416_(
               this.newClanDangerButton(
                  x0 + 254,
                  y0 + 184,
                  "Распустить",
                  () -> Minecraft.m_91087_()
                     .m_91152_(
                        new TabletScreen.ClanSimpleConfirmScreen(
                           "РАСПУСТИТЬ КЛАН?", clan.name(), () -> PacketHandler.sendToServer(new ClanDisbandPacket(clan.id()))
                        )
                     )
               )
            );
            this.addOwnerRequestButtons(x0, y0, clan);
            this.addOwnerKickButtons(x0, y0, clan);
         }
      }
   }

   private TabletScreen.ClanTextureButton newClanActionButton(int x, int y, String label, Runnable action) {
      return new TabletScreen.ClanTextureButton(
         x, y, 120, 24, CLAN_ACTION_BUTTON, CLAN_ACTION_BUTTON_HOVER, CLAN_ACTION_BUTTON_DISABLED, Component.m_237113_(label), action
      );
   }

   private TabletScreen.ClanTextureButton newClanDangerButton(int x, int y, String label, Runnable action) {
      return new TabletScreen.ClanTextureButton(
         x, y, 120, 24, CLAN_DANGER_BUTTON, CLAN_DANGER_BUTTON_HOVER, CLAN_ACTION_BUTTON_DISABLED, Component.m_237113_(label), action
      );
   }

   private TabletScreen.ClanTextureButton newClanSmallButton(int x, int y, String label, Runnable action) {
      return new TabletScreen.ClanTextureButton(x, y, 58, 18, CLAN_SMALL_BUTTON, CLAN_SMALL_BUTTON_HOVER, CLAN_SMALL_BUTTON, Component.m_237113_(label), action);
   }

   private void addOwnerRequestButtons(int x0, int y0, ClanListPacket.ClanEntry clan) {
      if (clan.pendingEntries() != null && !clan.pendingEntries().isEmpty()) {
         this.pendingScrollOffset = clamp(this.pendingScrollOffset, 0, Math.max(0, clan.pendingEntries().size() - 2));
         int count = Math.min(2, clan.pendingEntries().size() - this.pendingScrollOffset);

         for (int i = 0; i < count; i++) {
            ClanListPacket.PendingEntry pending = clan.pendingEntries().get(this.pendingScrollOffset + i);
            int y = y0 + 121 + i * 24;
            this.m_142416_(
               this.newClanSmallButton(x0 + 198, y, "Принять", () -> PacketHandler.sendToServer(new ClanAcceptJoinPacket(clan.id(), pending.uuid())))
            );
            this.m_142416_(this.newClanSmallButton(x0 + 260, y, "Откл.", () -> PacketHandler.sendToServer(new ClanRejectJoinPacket(clan.id(), pending.uuid()))));
         }
      }
   }

   private void addOwnerKickButtons(int x0, int y0, ClanListPacket.ClanEntry clan) {
      if (clan.pendingEntries() == null || clan.pendingEntries().isEmpty()) {
         if (clan.memberEntries() != null) {
            int row = 0;

            for (ClanListPacket.MemberEntry member : clan.memberEntries()) {
               if (!member.uuid().equals(clan.ownerUuid())) {
                  if (row >= 2) {
                     break;
                  }

                  int y = y0 + 121 + row * 24;
                  this.m_142416_(
                     this.newClanSmallButton(
                        x0 + 260,
                        y,
                        "Кик",
                        () -> Minecraft.m_91087_()
                           .m_91152_(
                              new TabletScreen.ClanSimpleConfirmScreen(
                                 "ИСКЛЮЧИТЬ?", member.name(), () -> PacketHandler.sendToServer(new ClanKickMemberPacket(clan.id(), member.uuid()))
                              )
                           )
                     )
                  );
                  row++;
               }
            }
         }
      }
   }

   public void m_86600_() {
      super.m_86600_();

      for (Renderable widget : this.f_169369_) {
         if (widget instanceof TabletScreen.TabletActionButton button) {
            button.updateState();
         }
      }

      if (TabletClientState.shouldClose()) {
         Minecraft.m_91087_().m_91152_(null);
         TabletClientState.resetCloseFlag();
      }
   }

   public void m_88315_(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
      this.m_280273_(g);
      int x = (this.f_96543_ - 380) / 2;
      int y = (this.f_96544_ - 220) / 2;
      RenderSystem.setShader(GameRenderer::m_172817_);
      ResourceLocation panel = this.getPanelTexture();
      RenderSystem.setShaderTexture(0, panel);
      g.m_280163_(panel, x, y, 0.0F, 0.0F, 380, 220, 380, 220);
      this.renderPageContent(g, x, y);
      super.m_88315_(g, mouseX, mouseY, partialTick);
   }

   private void renderPageContent(GuiGraphics g, int x, int y) {
      TabletScreen.TabletPage page = PAGES[this.currentPage];
      if (page.type() == TabletScreen.PageType.CLAN) {
         this.drawClans(g, x, y);
      } else if (page.type() == TabletScreen.PageType.SERVER_INFO) {
         this.drawServerInfo(g, x, y);
      } else {
         if (page.type() == TabletScreen.PageType.PROFILE) {
            this.drawHeader(g, x, y, "ПРОФИЛЬ ИГРОКА");
            this.drawInfoLine(g, x, y, 0, "Баланс", TabletClientState.getCoins() + " монет", -9882);
            this.drawInfoLine(g, x, y, 1, "Победы", String.valueOf(TabletClientState.getWins()));
            this.drawInfoLine(g, x, y, 2, "Матчи", String.valueOf(TabletClientState.getMatchesPlayed()));
            this.drawInfoLine(g, x, y, 3, "Убийства", String.valueOf(TabletClientState.getKills()));
            this.drawInfoLine(g, x, y, 4, "Смерти", String.valueOf(TabletClientState.getDeaths()));
            this.drawInfoLine(g, x, y, 5, "У/С", TabletClientState.getKdaText());
            this.drawInfoLine(g, x, y, 6, "Карьера", TabletClientState.getCareerProgressPercent() + "%");
         }
      }
   }

   private ResourceLocation getPanelTexture() {
      int tier = TabletClientState.getTabletAppearanceTier();
      if (tier >= 2) {
         return PANEL_LEGEND;
      } else {
         return tier == 1 ? PANEL_EPIC : PANEL;
      }
   }

   private void drawClans(GuiGraphics g, int x, int y) {
      List<ClanListPacket.ClanEntry> clans = TabletClientState.getClans();
      if (this.selectedClanIndex >= 0 && this.selectedClanIndex < clans.size()) {
         this.drawClanDetails(g, x, y, clans.get(this.selectedClanIndex));
      } else {
         this.drawHeader(g, x, y, "КЛАНЫ");
         int listLeft = x + 48;
         int listTop = y + 74;
         if (clans.isEmpty()) {
            this.drawWrappedText(g, "На сервере пока нет кланов.", listLeft, listTop + 12, 284, -1644826);
            this.drawWrappedText(g, "Создание клана стоит 1000 монет.", listLeft, listTop + 38, 284, -5592406);
         } else {
            this.clanScrollOffset = clamp(this.clanScrollOffset, 0, Math.max(0, clans.size() - 4));
            int rowCount = Math.min(4, clans.size() - this.clanScrollOffset);

            for (int i = 0; i < rowCount; i++) {
               int index = this.clanScrollOffset + i;
               ClanListPacket.ClanEntry clan = clans.get(index);
               int rowTop = listTop + i * 24;
               g.m_280509_(listLeft, rowTop + 2, listLeft + 8, rowTop + 10, clan.color());
            }

            if (clans.size() > 4) {
               this.drawInfoScrollbar(g, listLeft + 284 + 7, listTop, 92, this.clanScrollOffset, Math.max(1, clans.size() - 4));
            }
         }
      }
   }

   private void drawClanDetails(GuiGraphics g, int x, int y, ClanListPacket.ClanEntry clan) {
      int titleY = y + 58;
      g.m_280137_(Minecraft.m_91087_().f_91062_, "[" + clan.tag() + "] " + clan.name(), x + 190, titleY, clan.color());
      g.m_280056_(Minecraft.m_91087_().f_91062_, "КК: " + clan.clanCoins(), x + 52, y + 152, -10027162, false);
      String status;
      int statusColor;
      if (clan.owner()) {
         status = "СТАТУС: ГЛАВА";
         statusColor = -9882;
      } else if (clan.member()) {
         status = "СТАТУС: УЧАСТНИК";
         statusColor = -10027162;
      } else if (clan.pending()) {
         status = "СТАТУС: ЗАЯВКА";
         statusColor = -9882;
      } else {
         status = "СТАТУС: НЕ СОСТОИТ";
         statusColor = -5592406;
      }

      g.m_280056_(Minecraft.m_91087_().f_91062_, status, x + 52, y + 98, statusColor, false);
      g.m_280056_(Minecraft.m_91087_().f_91062_, "Глава:", x + 52, y + 116, -5592406, false);
      g.m_280056_(Minecraft.m_91087_().f_91062_, clan.ownerName(), x + 52, y + 128, -1644826, false);
      g.m_280056_(Minecraft.m_91087_().f_91062_, "Участников: " + clan.memberCount() + "/5", x + 52, y + 140, -1644826, false);
      if (clan.owner()) {
         int pending = clan.pendingEntries() == null ? 0 : clan.pendingEntries().size();
         g.m_280056_(Minecraft.m_91087_().f_91062_, "Заявки: " + pending, x + 212, y + 98, -9882, false);
         if (pending > 0) {
            this.pendingScrollOffset = clamp(this.pendingScrollOffset, 0, Math.max(0, pending - 2));
            int count = Math.min(2, pending - this.pendingScrollOffset);

            for (int i = 0; i < count; i++) {
               ClanListPacket.PendingEntry entry = clan.pendingEntries().get(this.pendingScrollOffset + i);
               g.m_280056_(Minecraft.m_91087_().f_91062_, entry.name(), x + 212, y + 113 + i * 24, -1644826, false);
            }

            if (pending > 2) {
               this.drawInfoScrollbar(g, x + 342, y + 112, 44, this.pendingScrollOffset, Math.max(1, pending - 2));
            }
         } else {
            this.drawMembersPreview(g, clan, x + 212, y + 112);
         }
      } else {
         g.m_280056_(Minecraft.m_91087_().f_91062_, "Участники", x + 212, y + 98, -5592406, false);
         this.drawMembersPreview(g, clan, x + 212, y + 112);
      }
   }

   private void drawMembersPreview(GuiGraphics g, ClanListPacket.ClanEntry clan, int x, int y) {
      List<ClanListPacket.MemberEntry> members = clan.memberEntries() == null ? List.of() : clan.memberEntries();
      g.m_280056_(Minecraft.m_91087_().f_91062_, "Участники:", x, y, -5592406, false);
      int count = Math.min(3, members.size());

      for (int i = 0; i < count; i++) {
         g.m_280056_(Minecraft.m_91087_().f_91062_, members.get(i).name(), x, y + 11 + i * 10, -1644826, false);
      }

      if (members.size() > count) {
         g.m_280056_(Minecraft.m_91087_().f_91062_, "+" + (members.size() - count) + " еще", x, y + 11 + count * 10, -5592406, false);
      }
   }

   private void drawWrappedText(GuiGraphics g, String text, int x, int y, int width, int color) {
      int lineY = y;

      for (FormattedCharSequence line : Minecraft.m_91087_().f_91062_.m_92923_(Component.m_237113_(text), width)) {
         g.m_280649_(Minecraft.m_91087_().f_91062_, line, x, lineY, color, false);
         lineY += 10;
      }
   }

   private void drawServerInfo(GuiGraphics g, int x, int y) {
      this.drawHeader(g, x, y, "ИНФОРМАЦИЯ О СЕРВЕРЕ");
      List<FormattedCharSequence> lines = this.getServerInfoLines(292);
      int maxScroll = this.getMaxInfoScroll(lines);
      this.infoScroll = clamp(this.infoScroll, 0, maxScroll);
      int left = x + 38;
      int top = y + 72;
      int right = left + 292;
      int bottom = top + 118;
      g.m_280588_(left, top, right, bottom);
      int lineY = top - this.infoScroll;

      for (FormattedCharSequence line : lines) {
         if (lineY > top - 10 && lineY < bottom) {
            g.m_280649_(Minecraft.m_91087_().f_91062_, line, left, lineY, -1644826, false);
         }

         lineY += 10;
      }

      g.m_280618_();
      this.drawInfoScrollbar(g, right + 5, top, 118, this.infoScroll, maxScroll);
   }

   private List<FormattedCharSequence> getServerInfoLines(int width) {
      List<FormattedCharSequence> result = new ArrayList<>();

      for (String paragraph : "Добро пожаловать на сервер DeluxeWarfare!\n\nЯ ZumaDeluxe - создатель сервера. Возможно, ты видел меня в TikTok или на YouTube — рад видеть тебя здесь. Сервер ещё находится в разработке, но ты уже можешь играть, тестировать режимы и помогать нам делать проект лучше.\n\nГлавная цель режима — остаться последним выжившим игроком на карте. Это похоже на Battle Royale, но без системы лута. На карте есть безопасная зона, которая постепенно сужается и каждый новый матч центр оказывается в случайном месте.\n\nУ тебя есть планшет, который ты сейчас держишь в руке. Это твой личный помощник в игре. Через него ты сможешь смотреть свой прогресс, информацию о наборах и использовать основные игровые кнопки.\n\nКак начать игру?\n\nИгра начинается автоматически, если на сервере больше одного человека. Тебе выдадут планшет, в котором есть все необходимые кнопки. Для игры есть два типа кнопок: наборы (или классы) и телепорт (rtp). При старте у тебя есть 30 секунд на подготовку (ты находишься в безопасном лобби). Нажав на кнопку с названием класса, ты можешь его выбрать, после чего тебе выдадут соответствующее снаряжение. Затем кнопки классов станут недоступны и тебе придется телепортироваться в рандомное место на карте (кнопка rtp). Если ты этого не сделаешь, то тебя телепортирует автоматически. Главное что ты должен знать - после телепортации ты находишься в игре и тебя могут убить! У каждого игрока есть по три жизни. Потратив все жизни игрок выбывает.\n\nУдачи!\n"
         .split("\\R", -1)) {
         if (paragraph.isBlank()) {
            result.add(FormattedCharSequence.f_13691_);
         } else {
            result.addAll(Minecraft.m_91087_().f_91062_.m_92923_(Component.m_237113_(paragraph), width));
         }
      }

      return result;
   }

   private int getMaxInfoScroll(List<FormattedCharSequence> lines) {
      int contentHeight = lines.size() * 10;
      return Math.max(0, contentHeight - 118);
   }

   private void drawInfoScrollbar(GuiGraphics g, int x, int y, int height, int scroll, int maxScroll) {
      g.m_280509_(x, y, x + 3, y + height, 1711276032);
      if (maxScroll <= 0) {
         g.m_280509_(x, y, x + 3, y + height, -10027162);
      } else {
         int thumbHeight = Math.max(16, height * height / (height + maxScroll));
         int thumbY = y + (height - thumbHeight) * scroll / maxScroll;
         g.m_280509_(x, thumbY, x + 3, thumbY + thumbHeight, -10027162);
      }
   }

   private boolean isMouseOverInfoArea(double mouseX, double mouseY) {
      int x = (this.f_96543_ - 380) / 2 + 38;
      int y = (this.f_96544_ - 220) / 2 + 72;
      return mouseX >= x && mouseX <= x + 292 + 10 && mouseY >= y && mouseY <= y + 118;
   }

   private boolean isMouseOverClanList(double mouseX, double mouseY) {
      int x = (this.f_96543_ - 380) / 2 + 48;
      int y = (this.f_96544_ - 220) / 2 + 74;
      return mouseX >= x && mouseX <= x + 284 + 14 && mouseY >= y && mouseY <= y + 98;
   }

   private boolean isMouseOverPendingList(double mouseX, double mouseY) {
      int x = (this.f_96543_ - 380) / 2 + 198;
      int y = (this.f_96544_ - 220) / 2 + 104;
      return mouseX >= x && mouseX <= x + 154 && mouseY >= y && mouseY <= y + 72;
   }

   public boolean m_6050_(double mouseX, double mouseY, double delta) {
      if (PAGES[this.currentPage].type() == TabletScreen.PageType.CLAN) {
         List<ClanListPacket.ClanEntry> clans = TabletClientState.getClans();
         if (this.isMouseOverClanList(mouseX, mouseY)) {
            this.clanScrollOffset = clamp(this.clanScrollOffset - (int)Math.signum(delta), 0, Math.max(0, clans.size() - 4));
            this.m_7856_();
            return true;
         }

         if (this.selectedClanIndex >= 0 && this.selectedClanIndex < clans.size() && this.isMouseOverPendingList(mouseX, mouseY)) {
            ClanListPacket.ClanEntry clan = clans.get(this.selectedClanIndex);
            int pending = clan.pendingEntries() == null ? 0 : clan.pendingEntries().size();
            this.pendingScrollOffset = clamp(this.pendingScrollOffset - (int)Math.signum(delta), 0, Math.max(0, pending - 2));
            this.m_7856_();
            return true;
         }
      }

      if (PAGES[this.currentPage].type() == TabletScreen.PageType.SERVER_INFO && this.isMouseOverInfoArea(mouseX, mouseY)) {
         int maxScroll = this.getMaxInfoScroll(this.getServerInfoLines(292));
         this.infoScroll = clamp(this.infoScroll - (int)Math.round(delta * 10.0 * 3.0), 0, maxScroll);
         return true;
      } else {
         return super.m_6050_(mouseX, mouseY, delta);
      }
   }

   private static int clamp(int value, int min, int max) {
      return Math.max(min, Math.min(max, value));
   }

   private void drawHeader(GuiGraphics g, int x, int y, String text) {
      g.m_280137_(Minecraft.m_91087_().f_91062_, text, x + 190, y + 52, -10027162);
   }

   private void drawInfoLine(GuiGraphics g, int x, int y, int row, String label, String value) {
      this.drawInfoLine(g, x, y, row, label, value, -1);
   }

   private void drawInfoLine(GuiGraphics g, int x, int y, int row, String label, String value, int valueColor) {
      int left = x + 52;
      int top = y + 72 + row * 17;
      g.m_280056_(Minecraft.m_91087_().f_91062_, label + ":", left, top, -5592406, false);
      g.m_280056_(Minecraft.m_91087_().f_91062_, value, left + 112, top, valueColor, false);
   }

   private void playSound(ResourceLocation sound) {
      Minecraft.m_91087_().m_91106_().m_120367_(SimpleSoundInstance.m_119755_(SoundEvent.m_262824_(sound), 1.0F, 0.0625F));
   }

   private String formatTime(long ms) {
      long totalSec = ms / 1000L;
      long m = totalSec / 60L;
      long s = totalSec % 60L;
      return String.format("%02d:%02d", m, s);
   }

   private void showPurchaseConfirmation(TabletScreen.TabletAction action) {
      Minecraft.m_91087_().m_91152_(new TabletScreen.TabletConfirmScreen(action, TabletScreen.ConfirmAction.SHOP_PURCHASE, 0));
   }

   private void showBaseUnlockConfirmation(TabletScreen.TabletAction action) {
      Minecraft.m_91087_().m_91152_(new TabletScreen.TabletConfirmScreen(action, TabletScreen.ConfirmAction.BASE_UNLOCK, 0));
   }

   private void showTierUpgradeConfirmation(TabletScreen.TabletAction action, int targetTier) {
      Minecraft.m_91087_().m_91152_(new TabletScreen.TabletConfirmScreen(action, TabletScreen.ConfirmAction.TIER_UPGRADE, targetTier));
   }

   private class ClanColorButton extends Button {
      private final int color;
      private final BooleanSupplier selected;
      private final Runnable action;

      private ClanColorButton(int x, int y, int color, BooleanSupplier selected, Runnable action) {
         super(Button.m_253074_(Component.m_237119_(), button -> {}).m_252987_(x, y, 16, 16));
         this.color = color;
         this.selected = selected;
         this.action = action;
      }

      public void m_5691_() {
         TabletScreen.this.playSound(TabletScreen.CLICK);
         this.action.run();
      }

      public void m_87963_(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
         boolean hover = this.m_5953_(mouseX, mouseY);
         int border = this.selected.getAsBoolean() ? -1 : (hover ? -5592406 : -12303292);
         g.m_280509_(this.m_252754_() - 1, this.m_252907_() - 1, this.m_252754_() + this.f_93618_ + 1, this.m_252907_() + this.f_93619_ + 1, border);
         g.m_280509_(this.m_252754_(), this.m_252907_(), this.m_252754_() + this.f_93618_, this.m_252907_() + this.f_93619_, this.color);
      }
   }

   private class ClanCreateConfirmScreen extends Screen {
      private ClanCreateConfirmScreen() {
         super(Component.m_237113_("Создание клана"));
      }

      protected void m_7856_() {
         int x = (this.f_96543_ - 240) / 2;
         int y = (this.f_96544_ - 132) / 2;
         int buttonY = y + 94;
         this.m_142416_(TabletScreen.this.new ConfirmTextureButton(x + 18, buttonY, Component.m_237113_("НЕТ"), this::returnToTablet));
         this.m_142416_(
            TabletScreen.this.new ConfirmTextureButton(
               x + 240 - 96 - 18, buttonY, Component.m_237113_("ДА"), () -> Minecraft.m_91087_().m_91152_(TabletScreen.this.new ClanCreateScreen())
            )
         );
      }

      public void m_88315_(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
         this.m_280273_(g);
         g.m_280509_(0, 0, this.f_96543_, this.f_96544_, -1442840576);
         int x = (this.f_96543_ - 240) / 2;
         int y = (this.f_96544_ - 132) / 2;
         RenderSystem.setShader(GameRenderer::m_172817_);
         RenderSystem.setShaderTexture(0, TabletScreen.CONFIRM_PANEL);
         g.m_280163_(TabletScreen.CONFIRM_PANEL, x, y, 0.0F, 0.0F, 240, 132, 240, 132);
         g.m_280137_(Minecraft.m_91087_().f_91062_, "СОЗДАТЬ КЛАН?", x + 120, y + 18, -10027162);
         g.m_280137_(Minecraft.m_91087_().f_91062_, "Стоимость: 1000 монет", x + 120, y + 48, -1);
         g.m_280137_(Minecraft.m_91087_().f_91062_, "Баланс: " + TabletClientState.getCoins(), x + 120, y + 66, -5592406);
         super.m_88315_(g, mouseX, mouseY, partialTick);
      }

      public void m_7379_() {
         this.returnToTablet();
      }

      private void returnToTablet() {
         Minecraft.m_91087_().m_91152_(TabletScreen.this);
      }
   }

   private class ClanCreateScreen extends Screen {
      private EditBox nameBox;
      private EditBox tagBox;
      private int selectedColor = ClanConstants.ALLOWED_COLORS[0];
      private TabletScreen.ConfirmTextureButton createButton;
      private String errorMessage = "";

      private ClanCreateScreen() {
         super(Component.m_237113_("Создание клана"));
      }

      protected void m_7856_() {
         int x = (this.f_96543_ - 380) / 2;
         int y = (this.f_96544_ - 220) / 2;
         this.nameBox = new EditBox(Minecraft.m_91087_().f_91062_, x + 82, y + 78, 190, 18, Component.m_237113_("Название"));
         this.nameBox.m_94199_(24);
         this.m_142416_(this.nameBox);
         this.tagBox = new EditBox(Minecraft.m_91087_().f_91062_, x + 82, y + 112, 68, 18, Component.m_237113_("Тег"));
         this.tagBox.m_94199_(4);
         this.m_142416_(this.tagBox);

         for (int i = 0; i < ClanConstants.ALLOWED_COLORS.length; i++) {
            int color = ClanConstants.ALLOWED_COLORS[i];
            int index = i;
            this.m_142416_(
               TabletScreen.this.new ClanColorButton(
                  x + 170 + i * 22, y + 111, color, () -> this.selectedColor == color, () -> this.selectedColor = ClanConstants.ALLOWED_COLORS[index]
               )
            );
         }

         this.m_142416_(TabletScreen.this.new ConfirmTextureButton(x + 70, y + 170, Component.m_237113_("ОТМЕНА"), this::returnToTablet));
         this.createButton = TabletScreen.this.new ConfirmTextureButton(x + 380 - 96 - 70, y + 170, Component.m_237113_("СОЗДАТЬ"), this::createClan);
         this.createButton.f_93623_ = false;
         this.m_142416_(this.createButton);
      }

      public void m_86600_() {
         super.m_86600_();
         if (this.createButton != null) {
            this.createButton.f_93623_ = this.isValidInput();
         }
      }

      public void m_88315_(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
         this.m_280273_(g);
         int x = (this.f_96543_ - 380) / 2;
         int y = (this.f_96544_ - 220) / 2;
         ResourceLocation panel = TabletScreen.this.getPanelTexture();
         RenderSystem.setShader(GameRenderer::m_172817_);
         RenderSystem.setShaderTexture(0, panel);
         g.m_280163_(panel, x, y, 0.0F, 0.0F, 380, 220, 380, 220);
         TabletScreen.this.drawHeader(g, x, y, "НОВЫЙ КЛАН");
         g.m_280056_(Minecraft.m_91087_().f_91062_, "Название", x + 82, y + 66, -5592406, false);
         g.m_280056_(Minecraft.m_91087_().f_91062_, "Тег", x + 82, y + 100, -5592406, false);
         g.m_280056_(Minecraft.m_91087_().f_91062_, "Цвет", x + 170, y + 100, -5592406, false);
         g.m_280056_(Minecraft.m_91087_().f_91062_, "Название: 3-24 символа", x + 82, y + 134, -8947849, false);
         g.m_280056_(Minecraft.m_91087_().f_91062_, "Тег: 1-4 символа", x + 82, y + 146, -8947849, false);
         g.m_280056_(Minecraft.m_91087_().f_91062_, "Стоимость: 1000 монет", x + 82, y + 158, -9882, false);
         if (!this.errorMessage.isBlank()) {
            g.m_280056_(Minecraft.m_91087_().f_91062_, this.errorMessage, x + 82, y + 184, -39322, false);
         }

         super.m_88315_(g, mouseX, mouseY, partialTick);
      }

      public void m_7379_() {
         this.returnToTablet();
      }

      private void createClan() {
         String name = this.nameBox.m_94155_().trim();
         String tag = this.tagBox.m_94155_().trim();
         if (!this.isValidInput()) {
            this.errorMessage = "Заполните название и тег.";
         } else {
            PacketHandler.sendToServer(new ClanCreatePacket(name, this.selectedColor, tag));
            this.returnToTablet();
         }
      }

      private boolean isValidInput() {
         String name = this.nameBox == null ? "" : this.nameBox.m_94155_().trim();
         String tag = this.tagBox == null ? "" : this.tagBox.m_94155_().trim();
         return name.length() >= 3 && name.length() <= 24 && !tag.isBlank() && tag.length() <= 4;
      }

      private void returnToTablet() {
         Minecraft.m_91087_().m_91152_(TabletScreen.this);
      }
   }

   private class ClanJoinConfirmScreen extends Screen {
      private final ClanListPacket.ClanEntry clan;

      private ClanJoinConfirmScreen(ClanListPacket.ClanEntry clan) {
         super(Component.m_237113_("Заявка в клан"));
         this.clan = clan;
      }

      protected void m_7856_() {
         int x = (this.f_96543_ - 240) / 2;
         int y = (this.f_96544_ - 132) / 2;
         int buttonY = y + 94;
         this.m_142416_(TabletScreen.this.new ConfirmTextureButton(x + 18, buttonY, Component.m_237113_("НЕТ"), this::returnToTablet));
         this.m_142416_(TabletScreen.this.new ConfirmTextureButton(x + 240 - 96 - 18, buttonY, Component.m_237113_("ДА"), () -> {
            PacketHandler.sendToServer(new ClanJoinRequestPacket(this.clan.id()));
            this.returnToTablet();
         }));
      }

      public void m_88315_(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
         this.m_280273_(g);
         g.m_280509_(0, 0, this.f_96543_, this.f_96544_, -1442840576);
         int x = (this.f_96543_ - 240) / 2;
         int y = (this.f_96544_ - 132) / 2;
         RenderSystem.setShader(GameRenderer::m_172817_);
         RenderSystem.setShaderTexture(0, TabletScreen.CONFIRM_PANEL);
         g.m_280163_(TabletScreen.CONFIRM_PANEL, x, y, 0.0F, 0.0F, 240, 132, 240, 132);
         g.m_280137_(Minecraft.m_91087_().f_91062_, "ВСТУПИТЬ В КЛАН?", x + 120, y + 18, -10027162);
         g.m_280137_(Minecraft.m_91087_().f_91062_, "[" + this.clan.tag() + "] " + this.clan.name(), x + 120, y + 48, this.clan.color());
         g.m_280137_(Minecraft.m_91087_().f_91062_, "Глава получит заявку", x + 120, y + 66, -5592406);
         super.m_88315_(g, mouseX, mouseY, partialTick);
      }

      public void m_7379_() {
         this.returnToTablet();
      }

      private void returnToTablet() {
         Minecraft.m_91087_().m_91152_(TabletScreen.this);
      }
   }

   private class ClanSimpleConfirmScreen extends Screen {
      private final String title;
      private final String detail;
      private final Runnable confirmAction;

      private ClanSimpleConfirmScreen(String title, String detail, Runnable confirmAction) {
         super(Component.m_237113_(title));
         this.title = title;
         this.detail = detail == null ? "" : detail;
         this.confirmAction = confirmAction;
      }

      protected void m_7856_() {
         int x = (this.f_96543_ - 240) / 2;
         int y = (this.f_96544_ - 132) / 2;
         int buttonY = y + 94;
         this.m_142416_(TabletScreen.this.new ConfirmTextureButton(x + 18, buttonY, Component.m_237113_("НЕТ"), this::returnToTablet));
         this.m_142416_(TabletScreen.this.new ConfirmTextureButton(x + 240 - 96 - 18, buttonY, Component.m_237113_("ДА"), () -> {
            this.confirmAction.run();
            this.returnToTablet();
         }));
      }

      public void m_88315_(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
         this.m_280273_(g);
         g.m_280509_(0, 0, this.f_96543_, this.f_96544_, -1442840576);
         int x = (this.f_96543_ - 240) / 2;
         int y = (this.f_96544_ - 132) / 2;
         RenderSystem.setShader(GameRenderer::m_172817_);
         RenderSystem.setShaderTexture(0, TabletScreen.CONFIRM_PANEL);
         g.m_280163_(TabletScreen.CONFIRM_PANEL, x, y, 0.0F, 0.0F, 240, 132, 240, 132);
         g.m_280137_(Minecraft.m_91087_().f_91062_, this.title, x + 120, y + 18, -10027162);
         g.m_280137_(Minecraft.m_91087_().f_91062_, this.detail, x + 120, y + 52, -1);
         super.m_88315_(g, mouseX, mouseY, partialTick);
      }

      public void m_7379_() {
         this.returnToTablet();
      }

      private void returnToTablet() {
         Minecraft.m_91087_().m_91152_(TabletScreen.this);
      }
   }

   private class ClanTextureButton extends Button {
      private final Runnable action;
      private final ResourceLocation normalTexture;
      private final ResourceLocation hoverTexture;
      private final ResourceLocation disabledTexture;
      private boolean wasHovered;

      private ClanTextureButton(int x, int y, int width, int height, Component label, Runnable action) {
         this(x, y, width, height, TabletScreen.BTN, TabletScreen.BTN_HOVER, TabletScreen.BTN_DISABLED, label, action);
      }

      private ClanTextureButton(
         int x,
         int y,
         int width,
         int height,
         ResourceLocation normalTexture,
         ResourceLocation hoverTexture,
         ResourceLocation disabledTexture,
         Component label,
         Runnable action
      ) {
         super(Button.m_253074_(label, button -> {}).m_252987_(x, y, width, height));
         this.action = action;
         this.normalTexture = normalTexture;
         this.hoverTexture = hoverTexture;
         this.disabledTexture = disabledTexture;
      }

      public void m_5691_() {
         TabletScreen.this.playSound(TabletScreen.CLICK);
         this.action.run();
      }

      public void m_7435_(SoundManager soundManager) {
      }

      public void m_87963_(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
         boolean hover = this.m_5953_(mouseX, mouseY);
         if (hover && !this.wasHovered) {
            TabletScreen.this.playSound(TabletScreen.HOVER);
         }

         this.wasHovered = hover;
         ResourceLocation texture = !this.f_93623_ ? this.disabledTexture : (hover ? this.hoverTexture : this.normalTexture);
         RenderSystem.setShader(GameRenderer::m_172817_);
         RenderSystem.setShaderTexture(0, texture);
         g.m_280163_(texture, this.m_252754_(), this.m_252907_(), 0.0F, 0.0F, this.f_93618_, this.f_93619_, this.f_93618_, this.f_93619_);
         g.m_280653_(
            Minecraft.m_91087_().f_91062_,
            this.m_6035_(),
            this.m_252754_() + this.f_93618_ / 2,
            this.m_252907_() + (this.f_93619_ - 8) / 2,
            !this.f_93623_ ? -11184811 : (hover ? -1 : -10027162)
         );
      }
   }

   private enum ConfirmAction {
      SHOP_PURCHASE,
      BASE_UNLOCK,
      TIER_UPGRADE;
   }

   private class ConfirmTextureButton extends Button {
      private boolean wasHovered;
      private final Runnable action;

      private ConfirmTextureButton(int x, int y, Component label, Runnable action) {
         super(Button.m_253074_(label, button -> {}).m_252987_(x, y, 96, 24));
         this.action = action;
      }

      public void m_5691_() {
         TabletScreen.this.playSound(TabletScreen.CLICK);
         this.action.run();
      }

      public void m_7435_(SoundManager soundManager) {
      }

      public void m_87963_(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
         boolean hover = this.m_5953_(mouseX, mouseY);
         if (hover && !this.wasHovered) {
            TabletScreen.this.playSound(TabletScreen.HOVER);
         }

         this.wasHovered = hover;
         ResourceLocation texture = !this.f_93623_
            ? TabletScreen.CONFIRM_BUTTON_DISABLED
            : (hover ? TabletScreen.CONFIRM_BUTTON_HOVER : TabletScreen.CONFIRM_BUTTON);
         RenderSystem.setShader(GameRenderer::m_172817_);
         RenderSystem.setShaderTexture(0, texture);
         g.m_280163_(texture, this.m_252754_(), this.m_252907_(), 0.0F, 0.0F, this.f_93618_, this.f_93619_, this.f_93618_, this.f_93619_);
         g.m_280653_(
            Minecraft.m_91087_().f_91062_,
            this.m_6035_(),
            this.m_252754_() + this.f_93618_ / 2,
            this.m_252907_() + (this.f_93619_ - 8) / 2,
            this.f_93623_ ? (hover ? -1 : -10027162) : -11184811
         );
      }
   }

   private class PageTabButton extends Button {
      private final int pageIndex;
      private boolean wasHovered;

      private PageTabButton(int x, int y, int pageIndex) {
         super(Button.m_253074_(Component.m_237113_(TabletScreen.PAGES[pageIndex].title()), button -> {}).m_252987_(x, y, 66, 20));
         this.pageIndex = pageIndex;
      }

      public void m_5691_() {
         if (TabletScreen.this.currentPage != this.pageIndex) {
            TabletScreen.this.playSound(TabletScreen.CLICK);
            TabletScreen.this.currentPage = this.pageIndex;
            TabletScreen.this.m_7856_();
         }
      }

      public void m_7435_(SoundManager soundManager) {
      }

      public void m_87963_(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
         boolean hover = this.m_5953_(mouseX, mouseY);
         if (hover && !this.wasHovered && TabletScreen.this.currentPage != this.pageIndex) {
            TabletScreen.this.playSound(TabletScreen.HOVER);
         }

         this.wasHovered = hover;
         ResourceLocation tex = TabletScreen.this.currentPage == this.pageIndex
            ? TabletScreen.TAB_BTN_ACTIVE
            : (hover ? TabletScreen.TAB_BTN_HOVER : TabletScreen.TAB_BTN);
         RenderSystem.setShader(GameRenderer::m_172817_);
         RenderSystem.setShaderTexture(0, tex);
         g.m_280163_(tex, this.m_252754_(), this.m_252907_(), 0.0F, 0.0F, this.f_93618_, this.f_93619_, this.f_93618_, this.f_93619_);
         int color = TabletScreen.this.currentPage == this.pageIndex ? -1 : (hover ? -1638426 : -10027162);
         g.m_280137_(
            Minecraft.m_91087_().f_91062_,
            TabletScreen.PAGES[this.pageIndex].title(),
            this.m_252754_() + this.f_93618_ / 2,
            this.m_252907_() + (this.f_93619_ - 8) / 2,
            color
         );
      }
   }

   private enum PageType {
      ACTIONS,
      SERVER_INFO,
      PROFILE,
      CLAN;
   }

   private record TabletAction(
      String label, String classKey, int actionId, boolean rtp, boolean locked, boolean shop, boolean exclusive, int price, int fixedLevel
   ) {
      private static TabletScreen.TabletAction classKit(String label, String classKey, int actionId) {
         return new TabletScreen.TabletAction(label, classKey, actionId, false, false, false, false, 0, -1);
      }

      private static TabletScreen.TabletAction shopClass(String label, String classKey, int actionId, int price, int fixedLevel) {
         return new TabletScreen.TabletAction(label, classKey, actionId, false, false, true, false, price, fixedLevel);
      }

      private static TabletScreen.TabletAction exclusiveClass(String label, String classKey, int actionId) {
         return exclusiveClass(label, classKey, actionId, 2);
      }

      private static TabletScreen.TabletAction exclusiveClass(String label, String classKey, int actionId, int fixedLevel) {
         return new TabletScreen.TabletAction(label, classKey, actionId, false, false, false, true, 0, fixedLevel);
      }

      private static TabletScreen.TabletAction rtp(String label, int actionId) {
         return new TabletScreen.TabletAction(label, "", actionId, true, false, false, false, 0, -1);
      }

      private static TabletScreen.TabletAction locked(String label) {
         return new TabletScreen.TabletAction(label, "", -1, false, true, false, false, 0, -1);
      }
   }

   private class TabletActionButton extends Button {
      private final TabletScreen.TabletAction action;
      private boolean wasHovered;

      private TabletActionButton(int x, int y, int w, int h, TabletScreen.TabletAction action) {
         super(Button.m_253074_(Component.m_237113_(action.label()), button -> {}).m_252987_(x, y, w, h));
         this.action = action;
         this.f_93623_ = !action.locked() && (!action.exclusive() || TabletClientState.isClassPurchased(action.classKey()));
      }

      public void updateState() {
         if (this.action.locked()) {
            this.f_93623_ = false;
         } else if (this.action.exclusive() && !TabletClientState.isClassPurchased(this.action.classKey())) {
            this.f_93623_ = false;
         } else if (TabletClientState.isCompetitiveSet() && this.action.shop()) {
            this.f_93623_ = false;
         } else {
            boolean purchased = this.action.shop() && TabletClientState.isClassPurchased(this.action.classKey());
            if (this.action.shop() && !purchased) {
               this.f_93623_ = TabletClientState.getCoins() >= this.action.price();
            } else {
               boolean running = TabletClientState.isGameRunning();
               if (this.isBaseClassAction() && !TabletClientState.isBaseClassUnlocked(this.action.classKey())) {
                  this.f_93623_ = TabletClientState.getCoins() >= 25;
               } else {
                  if (this.isBaseClassAction()
                     && TabletClientState.isBaseClassUnlocked(this.action.classKey())
                     && !TabletScreen.this.dismissedUpgradePrompts.contains(this.action.classKey())) {
                     int targetTier = this.getAvailableUpgradeTier();
                     if (targetTier > 0 && TabletClientState.getCoins() >= PlayerProgressManager.getUpgradeCost(targetTier)) {
                        this.f_93623_ = true;
                        return;
                     }
                  }

                  boolean kitUsed = TabletClientState.isKitUsed();
                  boolean rtpUsed = TabletClientState.isRtpUsed();
                  long cooldown = this.action.rtp() ? 0L : TabletClientState.getCooldown(this.action.actionId());
                  if (!running) {
                     this.f_93623_ = false;
                  } else {
                     if (this.action.rtp()) {
                        this.f_93623_ = !rtpUsed;
                     } else {
                        this.f_93623_ = !kitUsed && cooldown <= 0L;
                     }
                  }
               }
            }
         }
      }

      public void m_5691_() {
         if (this.f_93623_ && !this.action.locked()) {
            if (!TabletClientState.isCompetitiveSet() || !this.action.shop()) {
               if (this.action.shop() && !TabletClientState.isClassPurchased(this.action.classKey())) {
                  TabletScreen.this.showPurchaseConfirmation(this.action);
               } else {
                  boolean running = TabletClientState.isGameRunning();
                  if (this.isBaseClassAction() && !TabletClientState.isBaseClassUnlocked(this.action.classKey())) {
                     TabletScreen.this.showBaseUnlockConfirmation(this.action);
                  } else {
                     if (this.isBaseClassAction() && !TabletScreen.this.dismissedUpgradePrompts.contains(this.action.classKey())) {
                        int targetTier = this.getAvailableUpgradeTier();
                        if (targetTier == 1 && TabletClientState.getCoins() >= PlayerProgressManager.getUpgradeCost(targetTier)) {
                           TabletScreen.this.showTierUpgradeConfirmation(this.action, targetTier);
                           return;
                        }

                        if (targetTier == 2 && TabletClientState.getCoins() >= PlayerProgressManager.getUpgradeCost(targetTier)) {
                           TabletScreen.this.showTierUpgradeConfirmation(this.action, targetTier);
                           return;
                        }
                     }

                     TabletScreen.this.playSound(this.action.rtp() ? TabletScreen.TELEPORT : TabletScreen.CLICK);
                     PacketHandler.sendToServer(new TabletPacket(this.action.actionId()));
                  }
               }
            }
         }
      }

      public void m_7435_(SoundManager soundManager) {
      }

      public void m_87963_(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
         boolean hover = this.m_5953_(mouseX, mouseY);
         if (hover && !this.wasHovered && !this.action.locked()) {
            TabletScreen.this.playSound(TabletScreen.HOVER);
         }

         this.wasHovered = hover;
         String label = this.getRenderedLabel();
         ResourceLocation texture = this.getTexture(hover);
         RenderSystem.setShader(GameRenderer::m_172817_);
         RenderSystem.setShaderTexture(0, texture);
         g.m_280163_(texture, this.m_252754_(), this.m_252907_(), 0.0F, 0.0F, this.f_93618_, this.f_93619_, this.f_93618_, this.f_93619_);
         int color = this.f_93623_ ? (hover ? -1 : -10027162) : -11184811;
         g.m_280137_(Minecraft.m_91087_().f_91062_, label, this.m_252754_() + this.f_93618_ / 2, this.m_252907_() + (this.f_93619_ - 8) / 2 - 1, color);
      }

      private String getRenderedLabel() {
         if (this.action.locked()) {
            return this.action.label();
         }

         boolean running = TabletClientState.isGameRunning();
         if (TabletClientState.isCompetitiveSet() && this.action.shop()) {
            return this.action.label() + " [НЕДОСТУПНО]";
         }

         if (this.action.rtp()) {
            long timeLeft = TabletClientState.getRtpTimeLeft();
            if (!running) {
               return this.action.label() + " [ОЖИД.]";
            } else if (!this.f_93623_) {
               return this.action.label() + " [ИСП.]";
            } else {
               return timeLeft > 0L ? this.action.label() + " [" + TabletScreen.this.formatTime(timeLeft) + "]" : this.action.label();
            }
         } else {
            if (this.action.shop() && !TabletClientState.isClassPurchased(this.action.classKey())) {
               return this.action.label() + " [" + this.action.price() + " МОНЕТ]";
            }

            if (this.isBaseClassAction() && !TabletClientState.isBaseClassUnlocked(this.action.classKey())) {
               return this.action.label() + " [ОТКР. 25]";
            }

            if (this.isBaseClassAction() && !TabletScreen.this.dismissedUpgradePrompts.contains(this.action.classKey())) {
               int targetTier = this.getAvailableUpgradeTier();
               if (targetTier == 1 && TabletClientState.getCoins() >= PlayerProgressManager.getUpgradeCost(targetTier)) {
                  return this.action.label() + " [УЛУЧШИТЬ EPIC]";
               }

               if (targetTier == 2 && TabletClientState.getCoins() >= PlayerProgressManager.getUpgradeCost(targetTier)) {
                  return this.action.label() + " [УЛУЧШИТЬ LEGEND]";
               }
            }

            String classLabel = this.getProgressPrefix() + this.action.label();
            long cooldown = TabletClientState.getCooldown(this.action.actionId());
            if (!running) {
               return classLabel + " [ОЖИД.]";
            } else if (cooldown > 0L) {
               return classLabel + " [КД " + TabletScreen.this.formatTime(cooldown) + "]";
            } else {
               return !this.f_93623_ ? classLabel + " [ИСП.]" : classLabel;
            }
         }
      }

      private String getProgressPrefix() {
         if (this.action.exclusive()) {
            return "";
         } else if (this.action.shop()) {
            return "КУПЛЕНО ";
         } else {
            int level = TabletClientState.getClassTier(this.action.classKey());
            int xp = TabletClientState.getXP(this.action.classKey());
            if (level >= 2) {
               return "МАКС. ";
            } else {
               return level == 1 ? xp + " / 800 " : xp + " / 300 ";
            }
         }
      }

      private ResourceLocation getTexture(boolean hover) {
         if (this.action.locked()) {
            return TabletScreen.BTN_DISABLED;
         }

         if (this.action.rtp()) {
            return !this.f_93623_ ? TabletScreen.TP_BTN_DISABLED : (hover ? TabletScreen.TP_BTN_HOVER : TabletScreen.TP_BTN);
         }

         int level = this.action.fixedLevel() >= 0 ? this.action.fixedLevel() : TabletClientState.getClassTier(this.action.classKey());
         if (!this.f_93623_) {
            if (level >= 2) {
               return TabletScreen.BTN_LEGEND_DISABLED;
            } else {
               return level == 1 ? TabletScreen.BTN_EPIC_DISABLED : TabletScreen.BTN_DISABLED;
            }
         } else if (level >= 2) {
            return hover ? TabletScreen.BTN_LEGEND_HOVER : TabletScreen.BTN_LEGEND;
         } else if (level == 1) {
            return hover ? TabletScreen.BTN_EPIC_HOVER : TabletScreen.BTN_EPIC;
         } else {
            return hover ? TabletScreen.BTN_HOVER : TabletScreen.BTN;
         }
      }

      private boolean isBaseClassAction() {
         return !this.action.shop() && !this.action.exclusive() && !this.action.rtp() && !this.action.locked();
      }

      private int getAvailableUpgradeTier() {
         int tier = TabletClientState.getClassTier(this.action.classKey());
         int xp = TabletClientState.getXP(this.action.classKey());
         if (tier == 0 && xp >= 300) {
            return 1;
         } else {
            return tier == 1 && xp >= 800 ? 2 : 0;
         }
      }
   }

   private class TabletConfirmScreen extends Screen {
      private final TabletScreen.TabletAction action;
      private final TabletScreen.ConfirmAction confirmAction;
      private final int targetTier;

      private TabletConfirmScreen(TabletScreen.TabletAction action, TabletScreen.ConfirmAction confirmAction, int targetTier) {
         super(Component.m_237113_("Подтверждение покупки"));
         this.action = action;
         this.confirmAction = confirmAction;
         this.targetTier = targetTier;
      }

      protected void m_7856_() {
         this.m_169413_();
         int x = (this.f_96543_ - 240) / 2;
         int y = (this.f_96544_ - 132) / 2;
         int buttonY = y + 94;
         this.m_142416_(TabletScreen.this.new ConfirmTextureButton(x + 18, buttonY, Component.m_237113_("ОТМЕНА"), this::cancel));
         this.m_142416_(TabletScreen.this.new ConfirmTextureButton(x + 240 - 96 - 18, buttonY, Component.m_237113_("КУПИТЬ"), this::confirm));
         if (this.confirmAction != TabletScreen.ConfirmAction.SHOP_PURCHASE) {
            this.m_169413_();
            this.m_142416_(TabletScreen.this.new ConfirmTextureButton(x + 18, buttonY, Component.m_237113_("НЕТ"), this::cancel));
            this.m_142416_(TabletScreen.this.new ConfirmTextureButton(x + 240 - 96 - 18, buttonY, Component.m_237113_("ДА"), this::confirm));
         }
      }

      public void m_88315_(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
         this.m_280273_(g);
         g.m_280509_(0, 0, this.f_96543_, this.f_96544_, -1442840576);
         int x = (this.f_96543_ - 240) / 2;
         int y = (this.f_96544_ - 132) / 2;
         RenderSystem.setShader(GameRenderer::m_172817_);
         RenderSystem.setShaderTexture(0, TabletScreen.CONFIRM_PANEL);
         g.m_280163_(TabletScreen.CONFIRM_PANEL, x, y, 0.0F, 0.0F, 240, 132, 240, 132);
         g.m_280137_(Minecraft.m_91087_().f_91062_, this.getConfirmTitle(), x + 120, y + 17, -10027162);
         g.m_280137_(Minecraft.m_91087_().f_91062_, this.action.label(), x + 120, y + 44, this.getShopTitleColor(this.action));
         g.m_280137_(Minecraft.m_91087_().f_91062_, this.getPrice() + " монет", x + 120, y + 58, -1);
         g.m_280137_(Minecraft.m_91087_().f_91062_, "Баланс: " + TabletClientState.getCoins() + " монет", x + 120, y + 72, -5592406);
         super.m_88315_(g, mouseX, mouseY, partialTick);
      }

      public void m_7379_() {
         this.returnToTablet();
      }

      private void confirm() {
         if (this.confirmAction == TabletScreen.ConfirmAction.SHOP_PURCHASE) {
            PacketHandler.sendToServer(new TabletPacket(this.action.actionId()));
         } else if (this.confirmAction == TabletScreen.ConfirmAction.BASE_UNLOCK) {
            PacketHandler.sendToServer(new TabletPacket(TabletPacket.unlockBaseActionId(this.action.actionId())));
         } else if (this.targetTier == 1) {
            PacketHandler.sendToServer(new TabletPacket(TabletPacket.upgradeEpicActionId(this.action.actionId())));
         } else if (this.targetTier == 2) {
            PacketHandler.sendToServer(new TabletPacket(TabletPacket.upgradeLegendActionId(this.action.actionId())));
         }

         this.returnToTablet();
      }

      private void cancel() {
         if (this.confirmAction == TabletScreen.ConfirmAction.TIER_UPGRADE) {
            TabletScreen.this.dismissedUpgradePrompts.add(this.action.classKey());
         }

         this.returnToTablet();
      }

      private void returnToTablet() {
         Minecraft.m_91087_().m_91152_(TabletScreen.this);
      }

      private int getPrice() {
         if (this.confirmAction == TabletScreen.ConfirmAction.SHOP_PURCHASE) {
            return this.action.price();
         } else {
            return this.confirmAction == TabletScreen.ConfirmAction.BASE_UNLOCK ? 25 : PlayerProgressManager.getUpgradeCost(this.targetTier);
         }
      }

      private String getConfirmTitle() {
         if (this.confirmAction == TabletScreen.ConfirmAction.BASE_UNLOCK) {
            return "ОТКРЫТЬ КЛАСС?";
         } else {
            return this.confirmAction == TabletScreen.ConfirmAction.TIER_UPGRADE ? "УЛУЧШИТЬ КЛАСС?" : "ПОДТВЕРДИТЬ ПОКУПКУ";
         }
      }

      private int getShopTitleColor(TabletScreen.TabletAction action) {
         if (action.fixedLevel() >= 2) {
            return -9882;
         } else {
            return action.fixedLevel() == 1 ? -5085441 : -10027162;
         }
      }
   }

   private record TabletPage(String title, TabletScreen.PageType type, TabletScreen.TabletAction[] actions) {
   }
}
