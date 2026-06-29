package com.makar.tacticaltablet.airdrop.net;

import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent.Context;

public final class AirdropSmokeStatePacket {
   private final boolean active;
   private final ResourceLocation dimension;
   private final BlockPos pos;

   public AirdropSmokeStatePacket(boolean active, ResourceLocation dimension, BlockPos pos) {
      this.active = active;
      this.dimension = dimension;
      this.pos = pos == null ? BlockPos.f_121853_ : pos.m_7949_();
   }

   public AirdropSmokeStatePacket(FriendlyByteBuf buf) {
      this.active = buf.readBoolean();
      this.dimension = buf.m_130281_();
      this.pos = buf.m_130135_();
   }

   public void encode(FriendlyByteBuf buf) {
      buf.writeBoolean(this.active);
      buf.m_130085_(this.dimension);
      buf.m_130064_(this.pos);
   }

   public void handle(Supplier<Context> contextSupplier) {
      Context context = contextSupplier.get();
      context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> this::invokeClientHandler));
      context.setPacketHandled(true);
   }

   private void invokeClientHandler() {
      try {
         Class<?> handler = Class.forName("com.makar.tacticaltablet.airdrop.client.AirdropSmokeClientState");
         handler.getMethod("handle", AirdropSmokeStatePacket.class).invoke(null, this);
      } catch (ReflectiveOperationException exception) {
         throw new RuntimeException("Failed to update AirDrop smoke on client", exception);
      }
   }

   public boolean active() {
      return this.active;
   }

   public ResourceLocation dimension() {
      return this.dimension;
   }

   public BlockPos pos() {
      return this.pos;
   }
}
