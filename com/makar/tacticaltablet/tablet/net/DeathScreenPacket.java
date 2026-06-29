package com.makar.tacticaltablet.tablet.net;

import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent.Context;

public final class DeathScreenPacket {
   private static final int MAX_TEXT_LENGTH = 128;
   private final String title;
   private final String subtitle;
   private final int durationTicks;
   private final boolean playSadTrombone;

   public DeathScreenPacket(String title, String subtitle, int durationTicks) {
      this(title, subtitle, durationTicks, false);
   }

   public DeathScreenPacket(String title, String subtitle, int durationTicks, boolean playSadTrombone) {
      this.title = sanitize(title);
      this.subtitle = sanitize(subtitle);
      this.durationTicks = Math.max(1, durationTicks);
      this.playSadTrombone = playSadTrombone;
   }

   public DeathScreenPacket(FriendlyByteBuf buf) {
      this.title = buf.m_130136_(128);
      this.subtitle = buf.m_130136_(128);
      this.durationTicks = Math.max(1, buf.m_130242_());
      this.playSadTrombone = buf.readBoolean();
   }

   public void encode(FriendlyByteBuf buf) {
      buf.m_130072_(this.title, 128);
      buf.m_130072_(this.subtitle, 128);
      buf.m_130130_(this.durationTicks);
      buf.writeBoolean(this.playSadTrombone);
   }

   public void handle(Supplier<Context> contextSupplier) {
      Context context = contextSupplier.get();
      context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> this::invokeClientHandler));
      context.setPacketHandled(true);
   }

   private void invokeClientHandler() {
      try {
         Class<?> overlay = Class.forName("com.makar.tacticaltablet.client.DeathScreenOverlay");
         overlay.getMethod("show", String.class, String.class, int.class, boolean.class)
            .invoke(null, this.title, this.subtitle, this.durationTicks, this.playSadTrombone);
      } catch (ReflectiveOperationException exception) {
         throw new RuntimeException("Failed to display death screen", exception);
      }
   }

   private static String sanitize(String value) {
      if (value == null) {
         return "";
      } else {
         return value.length() <= 128 ? value : value.substring(0, 128);
      }
   }
}
