package com.makar.tacticaltablet.corpse.client;

import com.makar.tacticaltablet.corpse.CorpseEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.HumanoidModel.ArmPose;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.resources.ResourceLocation;

public class CorpseRenderer extends LivingEntityRenderer<CorpseEntity, PlayerModel<CorpseEntity>> {
   public CorpseRenderer(Context context) {
      super(context, new PlayerModel(context.m_174023_(ModelLayers.f_171162_), false), 0.25F);
      this.m_115326_(new CorpseRenderer.DarkRedCorpseLayer(this));
   }

   public void render(CorpseEntity corpse, float yaw, float partialTick, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
      ((PlayerModel)this.f_115290_).m_8009_(true);
      ((PlayerModel)this.f_115290_).f_102817_ = false;
      ((PlayerModel)this.f_115290_).f_102815_ = ArmPose.EMPTY;
      ((PlayerModel)this.f_115290_).f_102816_ = ArmPose.EMPTY;
      super.m_7392_(corpse, yaw, partialTick, poseStack, buffer, packedLight);
   }

   public ResourceLocation getTextureLocation(CorpseEntity corpse) {
      return corpse.createGameProfile().getProperties().containsKey("textures")
         ? Minecraft.m_91087_().m_91109_().m_240306_(corpse.createGameProfile())
         : DefaultPlayerSkin.m_118627_(corpse.m_20148_());
   }

   protected boolean shouldShowName(CorpseEntity corpse) {
      return false;
   }

   private static class DarkRedCorpseLayer extends RenderLayer<CorpseEntity, PlayerModel<CorpseEntity>> {
      private DarkRedCorpseLayer(RenderLayerParent<CorpseEntity, PlayerModel<CorpseEntity>> parent) {
         super(parent);
      }

      public void render(
         PoseStack poseStack,
         MultiBufferSource buffer,
         int packedLight,
         CorpseEntity corpse,
         float limbSwing,
         float limbSwingAmount,
         float partialTick,
         float ageInTicks,
         float netHeadYaw,
         float headPitch
      ) {
         VertexConsumer vertex = buffer.m_6299_(RenderType.m_110473_(this.m_117347_(corpse)));
         ((PlayerModel)this.m_117386_()).m_7695_(poseStack, vertex, packedLight, OverlayTexture.f_118083_, 0.38F, 0.0F, 0.0F, 0.55F);
      }
   }
}
