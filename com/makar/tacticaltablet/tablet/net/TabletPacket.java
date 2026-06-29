package com.makar.tacticaltablet.tablet.net;

import com.makar.tacticaltablet.anticheat.AntiCheatManager;
import com.makar.tacticaltablet.anticheat.Severity;
import com.makar.tacticaltablet.anticheat.ViolationType;
import com.makar.tacticaltablet.clan.ClanManager;
import com.makar.tacticaltablet.game.GameStateManager;
import com.makar.tacticaltablet.game.MapSetManager;
import com.makar.tacticaltablet.game.clanwar.ClanWarManager;
import com.makar.tacticaltablet.game.lives.LivesManager;
import com.makar.tacticaltablet.game.lobby.LobbyManager;
import com.makar.tacticaltablet.game.respawn.RtpTimerManager;
import com.makar.tacticaltablet.inventory.InventoryManager;
import com.makar.tacticaltablet.progression.ClassCooldownManager;
import com.makar.tacticaltablet.progression.PlayerProgressManager;
import com.makar.tacticaltablet.progression.kit.KitManager;
import com.makar.tacticaltablet.tablet.PlayerTabletState;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent.Context;

public class TabletPacket {
   private static final int RTP_ACTION_ID = 7;
   private static final int MIN_ACTION_ID = 0;
   private static final int MAX_ACTION_ID = 314;
   private static final int UNLOCK_BASE_ACTION_OFFSET = 100;
   private static final int UPGRADE_EPIC_ACTION_OFFSET = 200;
   private static final int UPGRADE_LEGEND_ACTION_OFFSET = 300;
   private static final int MAX_TABLET_ACTIONS = 3;
   private static final long TABLET_RATE_WINDOW_MS = 2000L;
   private static final Map<UUID, Deque<Long>> tabletActionTimes = new HashMap<>();
   private static final Map<Integer, String> KITS = Map.ofEntries(
      Map.entry(0, "stormtrooper"),
      Map.entry(1, "sniper"),
      Map.entry(2, "scout"),
      Map.entry(3, "droneoperator"),
      Map.entry(4, "boomguy"),
      Map.entry(5, "mortarman"),
      Map.entry(6, "dream"),
      Map.entry(8, "machinegunner"),
      Map.entry(9, "rpgtrooper"),
      Map.entry(10, "tagilla"),
      Map.entry(11, "blackops"),
      Map.entry(12, "cowboy"),
      Map.entry(13, "solider"),
      Map.entry(14, "rebel"),
      Map.entry(15, "saboteur"),
      Map.entry(16, "killer"),
      Map.entry(17, "miniboss"),
      Map.entry(18, "shahed"),
      Map.entry(19, "krot")
   );
   private final int actionId;

   public TabletPacket(int actionId) {
      this.actionId = actionId;
   }

   public TabletPacket(FriendlyByteBuf buf) {
      this.actionId = buf.readInt();
   }

   public void encode(FriendlyByteBuf buf) {
      buf.writeInt(this.actionId);
   }

   public static void reset(ServerPlayer player) {
      if (player != null) {
         tabletActionTimes.remove(player.m_20148_());
      }
   }

   public static void resetAll() {
      tabletActionTimes.clear();
   }

   public void handle(Supplier<Context> ctx) {
      ctx.get().enqueueWork(() -> {
         ServerPlayer player = ctx.get().getSender();
         if (player != null) {
            if (this.actionId < 0 || this.actionId > 314) {
               AntiCheatManager.record(player, ViolationType.INVALID_TABLET_PACKET, Severity.HIGH, "invalid actionId=" + this.actionId);
            } else if (!this.allowTabletAction(player)) {
               AntiCheatManager.record(player, ViolationType.PACKET_SPAM, Severity.HIGH, "tablet packet rate exceeded actionId=" + this.actionId);
            } else if (!InventoryManager.hasTablet(player)) {
               AntiCheatManager.record(player, ViolationType.INVALID_TABLET_PACKET, Severity.HIGH, "tablet packet without tablet actionId=" + this.actionId);
               LobbyManager.sync(player);
            } else if (this.actionId == 7) {
               this.handleRtp(player);
            } else if (isUnlockBaseAction(this.actionId)) {
               this.handleBaseUnlock(player, this.actionId - 100);
            } else if (isUpgradeEpicAction(this.actionId)) {
               this.handleTierUpgrade(player, this.actionId - 200, 1);
            } else if (isUpgradeLegendAction(this.actionId)) {
               this.handleTierUpgrade(player, this.actionId - 300, 2);
            } else {
               String kit = KITS.get(this.actionId);
               if (kit == null) {
                  AntiCheatManager.record(player, ViolationType.INVALID_TABLET_PACKET, Severity.HIGH, "unknown actionId=" + this.actionId);
               } else if (PlayerProgressManager.isShopClass(kit) && !PlayerProgressManager.isClassPurchased(player, kit)) {
                  this.handleShopPurchase(player, kit);
               } else {
                  this.handleKit(player, kit);
               }
            }
         }
      });
      ctx.get().setPacketHandled(true);
   }

   public static int unlockBaseActionId(int classActionId) {
      return 100 + classActionId;
   }

   public static int upgradeEpicActionId(int classActionId) {
      return 200 + classActionId;
   }

   public static int upgradeLegendActionId(int classActionId) {
      return 300 + classActionId;
   }

   private static boolean isUnlockBaseAction(int id) {
      return id >= 100 && id < 200;
   }

   private static boolean isUpgradeEpicAction(int id) {
      return id >= 200 && id < 300;
   }

   private static boolean isUpgradeLegendAction(int id) {
      return id >= 300 && id <= 314;
   }

   private boolean allowTabletAction(ServerPlayer player) {
      long now = System.currentTimeMillis();
      Deque<Long> timestamps = tabletActionTimes.computeIfAbsent(player.m_20148_(), uuid -> new ArrayDeque<>());

      while (!timestamps.isEmpty() && now - timestamps.peekFirst() > 2000L) {
         timestamps.removeFirst();
      }

      if (timestamps.size() >= 3) {
         return false;
      }

      timestamps.addLast(now);
      return true;
   }

   private void handleShopPurchase(ServerPlayer player, String kit) {
      if (MapSetManager.isCompetitiveSet()) {
         player.m_213846_(Component.m_237113_("[WAR] Магазинные классы недоступны в соревновательном режиме."));
         LobbyManager.sync(player);
      } else {
         PlayerProgressManager.PurchaseResult result = PlayerProgressManager.purchaseClass(player, kit);
         switch (result) {
            case PURCHASED:
               player.m_213846_(
                  Component.m_237113_("[WAR] Куплен класс " + this.getDisplayName(kit) + " за " + PlayerProgressManager.getShopPrice(kit) + " монет.")
               );
               PlayerProgressManager.savePlayer(player);
               break;
            case ALREADY_OWNED:
               player.m_213846_(Component.m_237113_("[WAR] Класс " + this.getDisplayName(kit) + " уже куплен."));
               break;
            case NOT_ENOUGH_COINS:
               player.m_213846_(
                  Component.m_237113_(
                     "[WAR] Не хватает монет для " + this.getDisplayName(kit) + ". Нужно " + PlayerProgressManager.getShopPrice(kit) + " монет."
                  )
               );
               break;
            case NOT_PURCHASABLE:
               player.m_213846_(Component.m_237113_("[WAR] Этот класс нельзя купить."));
         }

         LobbyManager.sync(player);
      }
   }

   private void handleBaseUnlock(ServerPlayer player, int classActionId) {
      String kit = KITS.get(classActionId);
      if (kit != null && PlayerProgressManager.isUnlockableBaseClass(kit)) {
         PlayerProgressManager.ProgressionResult result = PlayerProgressManager.unlockBaseClass(player, kit);
         switch (result) {
            case SUCCESS:
               player.m_213846_(Component.m_237113_("[WAR] Открыт класс " + this.getDisplayName(kit) + " за 25 монет."));
               PlayerProgressManager.savePlayer(player);
               break;
            case ALREADY_UNLOCKED:
               player.m_213846_(Component.m_237113_("[WAR] Класс " + this.getDisplayName(kit) + " уже открыт."));
               break;
            case NOT_ENOUGH_COINS:
               player.m_213846_(Component.m_237113_("[WAR] Не хватает монет для открытия " + this.getDisplayName(kit) + ". Нужно 25 монет."));
               break;
            default:
               player.m_213846_(Component.m_237113_("[WAR] Этот класс нельзя открыть через планшет."));
         }

         LobbyManager.sync(player);
      } else {
         AntiCheatManager.record(player, ViolationType.INVALID_TABLET_PACKET, Severity.HIGH, "invalid base unlock actionId=" + this.actionId);
         LobbyManager.sync(player);
      }
   }

   private void handleTierUpgrade(ServerPlayer player, int classActionId, int targetTier) {
      String kit = KITS.get(classActionId);
      if (kit != null && PlayerProgressManager.isBaseProgressionClass(kit)) {
         PlayerProgressManager.ProgressionResult result = PlayerProgressManager.upgradeClassTier(player, kit, targetTier);
         String tierName = targetTier >= 2 ? "LEGEND" : "EPIC";
         switch (result) {
            case SUCCESS:
               player.m_213846_(
                  Component.m_237113_(
                     "[WAR] Класс "
                        + this.getDisplayName(kit)
                        + " улучшен до "
                        + tierName
                        + " за "
                        + PlayerProgressManager.getUpgradeCost(targetTier)
                        + " монет."
                  )
               );
               PlayerProgressManager.savePlayer(player);
               break;
            case ALREADY_UNLOCKED:
            default:
               player.m_213846_(Component.m_237113_("[WAR] Этот класс нельзя улучшить."));
               break;
            case NOT_ENOUGH_COINS:
               player.m_213846_(
                  Component.m_237113_(
                     "[WAR] Не хватает монет для улучшения "
                        + this.getDisplayName(kit)
                        + ". Нужно "
                        + PlayerProgressManager.getUpgradeCost(targetTier)
                        + " монет."
                  )
               );
               break;
            case LOCKED:
               player.m_213846_(Component.m_237113_("[WAR] Сначала открой класс " + this.getDisplayName(kit) + "."));
               break;
            case NOT_ENOUGH_XP:
               player.m_213846_(Component.m_237113_("[WAR] Недостаточно опыта для улучшения " + this.getDisplayName(kit) + "."));
               break;
            case MAX_TIER:
               player.m_213846_(Component.m_237113_("[WAR] Класс " + this.getDisplayName(kit) + " уже максимального уровня."));
               break;
            case WRONG_TIER:
               player.m_213846_(Component.m_237113_("[WAR] Это улучшение сейчас недоступно."));
         }

         LobbyManager.sync(player);
      } else {
         AntiCheatManager.record(player, ViolationType.INVALID_TABLET_PACKET, Severity.HIGH, "invalid class upgrade actionId=" + this.actionId);
         LobbyManager.sync(player);
      }
   }

   private void handleRtp(ServerPlayer player) {
      if (MapSetManager.isClanWarSet() && !ClanWarManager.hasClan(player)) {
         ClanWarManager.showNeedClan(player);
         LobbyManager.sync(player);
      } else {
         String invalidReason = this.getInvalidRtpReason(player);
         if (!invalidReason.isEmpty()) {
            AntiCheatManager.record(player, ViolationType.INVALID_RTP, this.getInvalidRtpSeverity(invalidReason), invalidReason);
         }

         if (!GameStateManager.isRunning(player.f_8924_)) {
            player.m_213846_(Component.m_237113_("[WAR] Планшет недоступен до начала матча."));
            LobbyManager.sync(player);
         } else if (MapSetManager.isClanWarSet() && !ClanManager.getClanIdForPlayer(player).isBlank() && !ClanManager.isClanOwner(player)) {
            player.m_213846_(Component.m_237113_("[WAR] В войне кланов RTP вручную запускает только глава клана."));
            LobbyManager.sync(player);
         } else {
            RtpTimerManager.forceRtp(player);
         }
      }
   }

   private String getInvalidRtpReason(ServerPlayer player) {
      if (!GameStateManager.isRunning(player.f_8924_)) {
         return "game not running";
      } else if (!LivesManager.canContinueMatch(player)) {
         return "player eliminated";
      } else if (!GameStateManager.isInLobby(player)) {
         return "not in lobby dimension";
      } else if (!player.m_19880_().contains("in_lobby")) {
         return "missing in_lobby tag";
      } else {
         return PlayerTabletState.isRtpUsed(player) ? "rtp already used" : "";
      }
   }

   private Severity getInvalidRtpSeverity(String reason) {
      return !"not in lobby dimension".equals(reason) && !"missing in_lobby tag".equals(reason) && !"rtp already used".equals(reason)
         ? Severity.MEDIUM
         : Severity.HIGH;
   }

   private void handleKit(ServerPlayer player, String kit) {
      if (MapSetManager.isClanWarSet() && !ClanWarManager.hasClan(player)) {
         ClanWarManager.showNeedClan(player);
         LobbyManager.sync(player);
      } else if (MapSetManager.isCompetitiveSet() && PlayerProgressManager.isShopClass(kit)) {
         player.m_213846_(Component.m_237113_("[WAR] Магазинные классы недоступны в соревновательном режиме."));
         LobbyManager.sync(player);
      } else if (PlayerProgressManager.isShopClass(kit) && !PlayerProgressManager.isClassPurchased(player, kit)) {
         this.handleShopPurchase(player, kit);
      } else if (PlayerProgressManager.isExclusiveClass(kit) && !PlayerProgressManager.isExclusiveClassGranted(player, kit)) {
         AntiCheatManager.record(player, ViolationType.INVALID_TABLET_PACKET, Severity.HIGH, "locked exclusive class actionId=" + this.actionId);
         player.m_213846_(Component.m_237113_("[WAR] Этот эксклюзивный класс тебе не выдан."));
         LobbyManager.sync(player);
      } else if (PlayerProgressManager.isBaseProgressionClass(kit) && !PlayerProgressManager.isBaseClassUnlocked(player, kit)) {
         player.m_213846_(Component.m_237113_("[WAR] Сначала открой класс " + this.getDisplayName(kit) + " за 25 монет."));
         LobbyManager.sync(player);
      } else if (!GameStateManager.isRunning(player.f_8924_)) {
         player.m_213846_(Component.m_237113_("[WAR] Планшет недоступен до начала матча."));
         LobbyManager.sync(player);
      } else {
         boolean inLobby = player.m_19880_().contains("in_lobby") || GameStateManager.isInLobby(player);
         boolean inBattle = player.m_19880_().contains("war.playing");
         if (!inLobby && !inBattle) {
            player.m_213846_(Component.m_237113_("[WAR] Сейчас нельзя выбрать класс."));
            LobbyManager.sync(player);
         } else if (!LivesManager.canContinueMatch(player)) {
            player.m_213846_(Component.m_237113_("[WAR] Ты выбыл из матча."));
            LobbyManager.sync(player);
         } else if (PlayerTabletState.isKitUsed(player)) {
            LobbyManager.sync(player);
         } else if (ClassCooldownManager.isOnCooldown(player, this.actionId)) {
            LobbyManager.sync(player);
         } else if (!KitManager.giveKit(player, kit)) {
            player.m_213846_(Component.m_237113_("[WAR] Не удалось выдать набор. Выбери другой класс или проверь конфиг наборов."));
            LobbyManager.sync(player);
         } else {
            PlayerTabletState.setSelectedClass(player, kit);
            PlayerTabletState.setKitUsed(player);
            if (PlayerTabletState.isRtpUsed(player)) {
               InventoryManager.clearTablets(player);
            }

            LobbyManager.sync(player);
         }
      }
   }

   private String getDisplayName(String kit) {
      if ("boomguy".equals(kit)) {
         return "Подрывник";
      } else if ("dream".equals(kit)) {
         return "Дрим";
      } else if ("rpgtrooper".equals(kit)) {
         return "РПГ-боец";
      } else if ("droneoperator".equals(kit)) {
         return "Оператор дрона";
      } else if ("machinegunner".equals(kit)) {
         return "Пулемётчик";
      } else if ("mortarman".equals(kit)) {
         return "Миномётчик";
      } else if ("stormtrooper".equals(kit)) {
         return "Штурмовик";
      } else if ("sniper".equals(kit)) {
         return "Снайпер";
      } else if ("scout".equals(kit)) {
         return "Разведчик";
      } else if ("tagilla".equals(kit)) {
         return "Тагилла";
      } else if ("blackops".equals(kit)) {
         return "Спецназ";
      } else if ("cowboy".equals(kit)) {
         return "Ковбой";
      } else if ("solider".equals(kit)) {
         return "Солдат";
      } else if ("rebel".equals(kit)) {
         return "Повстанец";
      } else if ("saboteur".equals(kit)) {
         return "Диверсант";
      } else if ("killer".equals(kit)) {
         return "Киллер";
      } else if ("miniboss".equals(kit)) {
         return "Мини-Босс";
      } else if ("shahed".equals(kit)) {
         return "Шахед оп.";
      } else {
         return "krot".equals(kit) ? "Крот" : kit;
      }
   }
}
