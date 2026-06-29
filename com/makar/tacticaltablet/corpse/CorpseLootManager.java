package com.makar.tacticaltablet.corpse;

import com.makar.tacticaltablet.core.ModEntities;
import com.makar.tacticaltablet.core.TacticalTabletMod;
import com.mojang.authlib.properties.Property;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;

public final class CorpseLootManager {
   private static final double LOOT_FRACTION = 0.5;

   private CorpseLootManager() {
   }

   public static void createCorpse(ServerPlayer victim) {
      if (victim != null && victim.m_9236_() instanceof ServerLevel level) {
         try {
            List<ItemStack> loot = selectLoot(victim);
            if (loot.isEmpty()) {
               return;
            }

            CorpseEntity corpse = (CorpseEntity)((EntityType)ModEntities.PLAYER_CORPSE.get()).m_20615_(level);
            if (corpse == null) {
               return;
            }

            CorpseLootManager.SkinData skin = skinData(victim);
            corpse.initialize(victim.m_20148_(), victim.m_36316_().getName(), skin.value(), skin.signature(), loot);
            corpse.m_7678_(victim.m_20185_(), victim.m_20186_(), victim.m_20189_(), victim.m_146908_(), 0.0F);
            if (!level.m_7967_(corpse)) {
               return;
            }

            victim.m_150109_().m_6211_();
            victim.m_150109_().m_6596_();
         } catch (RuntimeException exception) {
            TacticalTabletMod.LOGGER.error("Failed to create corpse for {}", victim.m_36316_().getName(), exception);
         }
      }
   }

   private static List<ItemStack> selectLoot(ServerPlayer victim) {
      List<ItemStack> stacks = new ArrayList<>();

      for (int slot = 0; slot < victim.m_150109_().m_6643_(); slot++) {
         ItemStack stack = victim.m_150109_().m_8020_(slot);
         if (!stack.m_41619_()) {
            stacks.add(stack.m_41777_());
         }
      }

      if (stacks.isEmpty()) {
         return List.of();
      }

      Collections.shuffle(stacks);
      int lootCount = Math.max(1, (int)Math.ceil(stacks.size() * 0.5));
      lootCount = Math.min(lootCount, 27);
      return new ArrayList<>(stacks.subList(0, lootCount));
   }

   private static CorpseLootManager.SkinData skinData(ServerPlayer player) {
      Iterator var1 = player.m_36316_().getProperties().get("textures").iterator();
      if (var1.hasNext()) {
         Property property = (Property)var1.next();
         return new CorpseLootManager.SkinData(property.getValue(), property.getSignature());
      } else {
         return new CorpseLootManager.SkinData("", "");
      }
   }

   private record SkinData(String value, String signature) {
   }
}
