package com.makar.tacticaltablet.corpse;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.Entity.RemovalReason;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier.Builder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public class CorpseEntity extends LivingEntity {
   public static final int CONTAINER_SIZE = 27;
   private static final int LIFETIME_TICKS = 1200;
   private static final EntityDataAccessor<String> OWNER_ID = SynchedEntityData.m_135353_(CorpseEntity.class, EntityDataSerializers.f_135030_);
   private static final EntityDataAccessor<String> OWNER_NAME = SynchedEntityData.m_135353_(CorpseEntity.class, EntityDataSerializers.f_135030_);
   private static final EntityDataAccessor<String> SKIN_VALUE = SynchedEntityData.m_135353_(CorpseEntity.class, EntityDataSerializers.f_135030_);
   private static final EntityDataAccessor<String> SKIN_SIGNATURE = SynchedEntityData.m_135353_(CorpseEntity.class, EntityDataSerializers.f_135030_);
   private final SimpleContainer loot = new SimpleContainer(27);

   public CorpseEntity(EntityType<? extends CorpseEntity> type, Level level) {
      super(type, level);
      this.m_20242_(true);
      this.m_20124_(Pose.SLEEPING);
      this.m_21153_(1.0F);
      this.f_19794_ = true;
   }

   public static Builder createAttributes() {
      return LivingEntity.m_21183_().m_22268_(Attributes.f_22276_, 1.0).m_22268_(Attributes.f_22279_, 0.0).m_22268_(Attributes.f_22278_, 1.0);
   }

   public void initialize(UUID ownerId, String ownerName, String skinValue, String skinSignature, List<ItemStack> stacks) {
      this.f_19804_.m_135381_(OWNER_ID, ownerId == null ? "" : ownerId.toString());
      this.f_19804_.m_135381_(OWNER_NAME, ownerName == null ? "" : ownerName);
      this.f_19804_.m_135381_(SKIN_VALUE, skinValue == null ? "" : skinValue);
      this.f_19804_.m_135381_(SKIN_SIGNATURE, skinSignature == null ? "" : skinSignature);
      this.m_6593_(Component.m_237113_(this.getOwnerName()));
      this.m_20340_(false);
      this.loot.m_6211_();
      if (stacks != null) {
         for (int i = 0; i < stacks.size() && i < this.loot.m_6643_(); i++) {
            this.loot.m_6836_(i, stacks.get(i).m_41777_());
         }
      }
   }

   public GameProfile createGameProfile() {
      UUID uuid = this.getOwnerId();
      GameProfile profile = new GameProfile(uuid, this.getOwnerName());
      String skinValue = this.getSkinValue();
      if (!skinValue.isBlank()) {
         profile.getProperties().put("textures", new Property("textures", skinValue, this.getSkinSignature()));
      }

      return profile;
   }

   public String getOwnerName() {
      String ownerName = (String)this.f_19804_.m_135370_(OWNER_NAME);
      return ownerName.isBlank() ? "Игрок" : ownerName;
   }

   public void m_8119_() {
      super.m_8119_();
      this.m_20256_(Vec3.f_82478_);
      this.m_20124_(Pose.SLEEPING);
      if (!this.m_9236_().f_46443_ && (this.f_19797_ >= 1200 || this.isLootEmpty())) {
         this.removeWithLoot(RemovalReason.DISCARDED);
      }
   }

   public InteractionResult m_6096_(Player player, InteractionHand hand) {
      if (this.m_9236_().f_46443_) {
         return InteractionResult.SUCCESS;
      }

      if (player instanceof ServerPlayer serverPlayer) {
         if (serverPlayer.m_6084_() && !serverPlayer.m_5833_()) {
            UUID ownerId = this.getOwnerId();
            if (ownerId != null && ownerId.equals(serverPlayer.m_20148_()) && !CorpseTestManager.canLootOwnCorpses(serverPlayer)) {
               serverPlayer.m_5661_(Component.m_237113_("Вы не можете лутать свой труп."), true);
               return InteractionResult.CONSUME;
            } else if (this.isLootEmpty()) {
               this.removeWithLoot(RemovalReason.DISCARDED);
               return InteractionResult.CONSUME;
            } else {
               serverPlayer.m_5893_(
                  new SimpleMenuProvider(
                     (windowId, inventory, ignored) -> ChestMenu.m_39237_(windowId, inventory, this.loot), Component.m_237113_("Труп: " + this.getOwnerName())
                  )
               );
               return InteractionResult.CONSUME;
            }
         } else {
            return InteractionResult.CONSUME;
         }
      } else {
         return InteractionResult.PASS;
      }
   }

   public boolean m_6087_() {
      return true;
   }

   public boolean m_5829_() {
      return false;
   }

   public boolean m_6094_() {
      return false;
   }

   public boolean m_6469_(DamageSource source, float amount) {
      return false;
   }

   public boolean m_142391_() {
      return false;
   }

   public EntityDimensions m_6972_(Pose pose) {
      return EntityDimensions.m_20398_(1.9F, 0.5F);
   }

   public Direction m_21259_() {
      return Direction.m_122364_(this.m_146908_());
   }

   public Iterable<ItemStack> m_6168_() {
      return List.of();
   }

   public ItemStack m_6844_(EquipmentSlot slot) {
      return ItemStack.f_41583_;
   }

   public void m_8061_(EquipmentSlot slot, ItemStack stack) {
   }

   public HumanoidArm m_5737_() {
      return HumanoidArm.RIGHT;
   }

   protected void m_8097_() {
      super.m_8097_();
      this.f_19804_.m_135372_(OWNER_ID, "");
      this.f_19804_.m_135372_(OWNER_NAME, "");
      this.f_19804_.m_135372_(SKIN_VALUE, "");
      this.f_19804_.m_135372_(SKIN_SIGNATURE, "");
   }

   public void m_7378_(CompoundTag tag) {
   }

   public void m_7380_(CompoundTag tag) {
   }

   private UUID getOwnerId() {
      String value = (String)this.f_19804_.m_135370_(OWNER_ID);
      if (value.isBlank()) {
         return null;
      }

      try {
         return UUID.fromString(value);
      } catch (IllegalArgumentException ignored) {
         return null;
      }
   }

   private String getSkinValue() {
      return (String)this.f_19804_.m_135370_(SKIN_VALUE);
   }

   private String getSkinSignature() {
      return (String)this.f_19804_.m_135370_(SKIN_SIGNATURE);
   }

   private boolean isLootEmpty() {
      for (int slot = 0; slot < this.loot.m_6643_(); slot++) {
         if (!this.loot.m_8020_(slot).m_41619_()) {
            return false;
         }
      }

      return true;
   }

   private void removeWithLoot(RemovalReason reason) {
      this.loot.m_6211_();
      this.m_142687_(reason);
   }
}
