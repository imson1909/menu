package com.makar.tacticaltablet.inventory;

import com.makar.tacticaltablet.airdrop.AirdropManager;
import com.makar.tacticaltablet.anticheat.AntiCheatManager;
import com.makar.tacticaltablet.anticheat.Severity;
import com.makar.tacticaltablet.anticheat.ViolationType;
import com.makar.tacticaltablet.game.GameStateManager;
import com.makar.tacticaltablet.game.lives.LivesManager;
import com.makar.tacticaltablet.game.team.TeamMatchManager;
import com.makar.tacticaltablet.tablet.PlayerTabletState;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.Map.Entry;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.event.entity.item.ItemTossEvent;
import net.minecraftforge.event.entity.player.EntityItemPickupEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent.RightClickBlock;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.registries.ForgeRegistries;

@EventBusSubscriber(modid = "tacticaltablet")
public class InventoryLockEvents {
   private static final long DROPPED_ITEM_PICKUP_WINDOW_MS = 120000L;
   private static final Map<UUID, InventoryLockEvents.DroppedItemOwner> playerDroppedItems = new HashMap<>();

   @SubscribeEvent
   public static void onItemToss(ItemTossEvent event) {
      if (event.getPlayer() instanceof ServerPlayer player) {
         if (isLobbyOrBattle(player) && !canUseDroppedItems(player)) {
            event.setCanceled(true);
            AntiCheatManager.record(player, ViolationType.ILLEGAL_INVENTORY, Severity.LOW, "blocked item toss item=" + itemName(event.getEntity().m_32055_()));
         } else {
            if (canUseDroppedItems(player)) {
               rememberDroppedItem(event.getEntity(), player);
            }
         }
      }
   }

   @SubscribeEvent
   public static void onRightClickBlock(RightClickBlock event) {
      if (event.getEntity() instanceof ServerPlayer player) {
         boolean inLobby = GameStateManager.isInLobby(player) || player.m_19880_().contains("in_lobby");
         boolean inBattle = player.m_19880_().contains("war.playing");
         if (inLobby || inBattle) {
            BlockEntity blockEntity = player.m_9236_().m_7702_(event.getPos());
            if (!tryRecoverSuperbWarfareJumpPlate(player, event)) {
               if (blockEntity instanceof Container) {
                  if (AirdropManager.isAirdropChest(event.getPos())) {
                     return;
                  }

                  if (canUseSuperbWarfareTool(player, event)) {
                     return;
                  }

                  event.setCanceled(true);
                  event.setCancellationResult(InteractionResult.FAIL);
                  AntiCheatManager.record(player, ViolationType.ILLEGAL_CONTAINER, Severity.LOW, "blocked container at " + event.getPos().m_123344_());
                  player.m_213846_(Component.m_237113_("[WAR] Контейнеры отключены во время матча."));
               }
            }
         }
      }
   }

   @SubscribeEvent
   public static void onItemPickup(EntityItemPickupEvent event) {
      if (event.getEntity() instanceof ServerPlayer player) {
         if (isLobbyOrBattle(player)) {
            ItemEntity item = event.getItem();
            if (!canUseDroppedItems(player) || !canPickUpRecentlyDroppedItem(item, player)) {
               event.setCanceled(true);
            }
         }
      }
   }

   private static void rememberDroppedItem(ItemEntity item, ServerPlayer player) {
      if (item != null && player != null) {
         purgeExpiredDroppedItems();
         playerDroppedItems.put(item.m_20148_(), new InventoryLockEvents.DroppedItemOwner(player.m_20148_(), System.currentTimeMillis() + 120000L));
      }
   }

   private static boolean canPickUpRecentlyDroppedItem(ItemEntity item, ServerPlayer player) {
      if (item != null && player != null) {
         purgeExpiredDroppedItems();
         InventoryLockEvents.DroppedItemOwner owner = playerDroppedItems.get(item.m_20148_());
         if (owner == null) {
            return false;
         } else {
            return owner.expiresAtMillis() < System.currentTimeMillis()
               ? false
               : owner.owner().equals(player.m_20148_()) || TeamMatchManager.areTeammates(owner.owner(), player.m_20148_());
         }
      } else {
         return false;
      }
   }

   private static void purgeExpiredDroppedItems() {
      long now = System.currentTimeMillis();
      Iterator<Entry<UUID, InventoryLockEvents.DroppedItemOwner>> iterator = playerDroppedItems.entrySet().iterator();

      while (iterator.hasNext()) {
         if (iterator.next().getValue().expiresAtMillis() < now) {
            iterator.remove();
         }
      }
   }

   public static void resetTracking() {
      playerDroppedItems.clear();
   }

   private static boolean isLobbyOrBattle(ServerPlayer player) {
      boolean inLobby = GameStateManager.isInLobby(player) || player.m_19880_().contains("in_lobby");
      boolean inBattle = player.m_19880_().contains("war.playing");
      return inLobby || inBattle;
   }

   private static boolean canUseDroppedItems(ServerPlayer player) {
      return player.m_19880_().contains("war.playing") && PlayerTabletState.isKitUsed(player) && !LivesManager.isEliminated(player);
   }

   private static boolean canUseSuperbWarfareTool(ServerPlayer player, RightClickBlock event) {
      if (!canUseDroppedItems(player)) {
         return false;
      }

      ItemStack stack = player.m_21120_(event.getHand());
      ResourceLocation itemId = ForgeRegistries.ITEMS.getKey(stack.m_41720_());
      ResourceLocation blockId = ForgeRegistries.BLOCKS.getKey(player.m_9236_().m_8055_(event.getPos()).m_60734_());
      return isSuperbWarfareCrowbar(itemId) && isSuperbWarfareBlock(blockId);
   }

   private static boolean isSuperbWarfareCrowbar(ResourceLocation itemId) {
      return itemId != null && "superbwarfare".equals(itemId.m_135827_()) && itemId.m_135815_().contains("crowbar");
   }

   private static boolean isSuperbWarfareBlock(ResourceLocation blockId) {
      return blockId != null && "superbwarfare".equals(blockId.m_135827_());
   }

   private static boolean tryRecoverSuperbWarfareJumpPlate(ServerPlayer player, RightClickBlock event) {
      if (!canUseDroppedItems(player)) {
         return false;
      }

      ItemStack held = player.m_21120_(event.getHand());
      ResourceLocation itemId = ForgeRegistries.ITEMS.getKey(held.m_41720_());
      if (!isSuperbWarfareCrowbar(itemId)) {
         return false;
      }

      Block block = player.m_9236_().m_8055_(event.getPos()).m_60734_();
      ResourceLocation blockId = ForgeRegistries.BLOCKS.getKey(block);
      if (!isSuperbWarfareJumpPlate(blockId)) {
         return false;
      }

      Level level = player.m_9236_();
      if (level.f_46443_) {
         return true;
      }

      ItemStack recovered = new ItemStack(block.m_5456_());
      if (recovered.m_41619_()) {
         return false;
      }

      level.m_7471_(event.getPos(), false);
      if (!player.m_150109_().m_36054_(recovered)) {
         ItemEntity dropped = new ItemEntity(level, player.m_20185_(), player.m_20186_(), player.m_20189_(), recovered);
         level.m_7967_(dropped);
         rememberDroppedItem(dropped, player);
      }

      event.setCanceled(true);
      event.setCancellationResult(InteractionResult.SUCCESS);
      player.m_213846_(Component.m_237113_("[WAR] Прыжковая платформа возвращена."));
      return true;
   }

   private static boolean isSuperbWarfareJumpPlate(ResourceLocation blockId) {
      if (!isSuperbWarfareBlock(blockId)) {
         return false;
      }

      String path = blockId.m_135815_();
      return path.contains("jump") && (path.contains("plate") || path.contains("pad") || path.contains("board"));
   }

   private static String itemName(ItemStack stack) {
      if (stack != null && !stack.m_41619_()) {
         ResourceLocation itemId = ForgeRegistries.ITEMS.getKey(stack.m_41720_());
         return itemId == null ? stack.m_41720_().toString() : itemId.toString();
      } else {
         return "empty";
      }
   }

   private record DroppedItemOwner(UUID owner, long expiresAtMillis) {
   }
}
