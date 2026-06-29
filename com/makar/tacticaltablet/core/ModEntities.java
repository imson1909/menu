package com.makar.tacticaltablet.core;

import com.makar.tacticaltablet.corpse.CorpseEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.EntityType.Builder;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModEntities {
   public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, "tacticaltablet");
   public static final RegistryObject<EntityType<CorpseEntity>> PLAYER_CORPSE = ENTITIES.register(
      "player_corpse", () -> Builder.m_20704_(CorpseEntity::new, MobCategory.MISC).m_20699_(0.6F, 0.35F).m_20702_(64).m_20717_(20).m_20712_("player_corpse")
   );

   private ModEntities() {
   }
}
