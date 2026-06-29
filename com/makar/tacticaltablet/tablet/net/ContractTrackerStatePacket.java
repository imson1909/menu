package com.makar.tacticaltablet.tablet.net;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent.Context;

public class ContractTrackerStatePacket {
   private static final int MAX_TARGETS = 16;
   private static final int MAX_NAME_LENGTH = 32;
   private static final int MAX_CLASS_LENGTH = 32;
   private final boolean active;
   private final boolean openScreen;
   private final int zoneCenterX;
   private final int zoneCenterZ;
   private final int zoneRadius;
   private final int playerX;
   private final int playerZ;
   private final int signalSecondsLeft;
   private final List<ContractTrackerStatePacket.TargetEntry> targets;

   public static ContractTrackerStatePacket empty(boolean openScreen, int zoneCenterX, int zoneCenterZ, int zoneRadius) {
      return new ContractTrackerStatePacket(false, openScreen, zoneCenterX, zoneCenterZ, zoneRadius, 0, 0, 0, List.of());
   }

   public ContractTrackerStatePacket(
      boolean active,
      boolean openScreen,
      int zoneCenterX,
      int zoneCenterZ,
      int zoneRadius,
      int playerX,
      int playerZ,
      int signalSecondsLeft,
      List<ContractTrackerStatePacket.TargetEntry> targets
   ) {
      this.active = active;
      this.openScreen = openScreen;
      this.zoneCenterX = zoneCenterX;
      this.zoneCenterZ = zoneCenterZ;
      this.zoneRadius = Math.max(1, zoneRadius);
      this.playerX = playerX;
      this.playerZ = playerZ;
      this.signalSecondsLeft = Math.max(0, signalSecondsLeft);
      this.targets = copyTargets(targets);
   }

   public ContractTrackerStatePacket(FriendlyByteBuf buf) {
      this.active = buf.readBoolean();
      this.openScreen = buf.readBoolean();
      this.zoneCenterX = buf.readInt();
      this.zoneCenterZ = buf.readInt();
      this.zoneRadius = buf.readInt();
      this.playerX = buf.readInt();
      this.playerZ = buf.readInt();
      this.signalSecondsLeft = buf.readInt();
      int size = buf.readInt();
      if (size >= 0 && size <= 16) {
         List<ContractTrackerStatePacket.TargetEntry> entries = new ArrayList<>();

         for (int i = 0; i < size; i++) {
            entries.add(
               new ContractTrackerStatePacket.TargetEntry(
                  buf.m_130136_(32),
                  buf.m_130136_(32),
                  buf.readInt(),
                  buf.readInt(),
                  buf.readInt(),
                  buf.readInt(),
                  buf.readInt(),
                  buf.readInt(),
                  buf.readInt(),
                  buf.readInt(),
                  buf.readInt()
               )
            );
         }

         this.targets = List.copyOf(entries);
      } else {
         throw new IllegalArgumentException("Invalid tracker target count: " + size);
      }
   }

   public void encode(FriendlyByteBuf buf) {
      buf.writeBoolean(this.active);
      buf.writeBoolean(this.openScreen);
      buf.writeInt(this.zoneCenterX);
      buf.writeInt(this.zoneCenterZ);
      buf.writeInt(this.zoneRadius);
      buf.writeInt(this.playerX);
      buf.writeInt(this.playerZ);
      buf.writeInt(this.signalSecondsLeft);
      buf.writeInt(this.targets.size());

      for (ContractTrackerStatePacket.TargetEntry target : this.targets) {
         buf.m_130072_(target.name(), 32);
         buf.m_130072_(target.selectedClass(), 32);
         buf.writeInt(target.kills());
         buf.writeInt(target.wins());
         buf.writeInt(target.careerPercent());
         buf.writeInt(target.difficulty());
         buf.writeInt(target.price());
         buf.writeInt(target.reward());
         buf.writeInt(target.areaX());
         buf.writeInt(target.areaZ());
         buf.writeInt(target.areaRadius());
      }
   }

   public void handle(Supplier<Context> ctx) {
      ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> this.invokeClientHandler()));
      ctx.get().setPacketHandled(true);
   }

   private void invokeClientHandler() {
      try {
         Class<?> handler = Class.forName("com.makar.tacticaltablet.tablet.client.ContractClientPacketHandler");
         handler.getMethod("handleTracker", ContractTrackerStatePacket.class).invoke(null, this);
      } catch (ReflectiveOperationException exception) {
         throw new RuntimeException("Failed to handle contract tracker packet on client", exception);
      }
   }

   public boolean active() {
      return this.active;
   }

   public boolean openScreen() {
      return this.openScreen;
   }

   public int zoneCenterX() {
      return this.zoneCenterX;
   }

   public int zoneCenterZ() {
      return this.zoneCenterZ;
   }

   public int zoneRadius() {
      return this.zoneRadius;
   }

   public int playerX() {
      return this.playerX;
   }

   public int playerZ() {
      return this.playerZ;
   }

   public int signalSecondsLeft() {
      return this.signalSecondsLeft;
   }

   public List<ContractTrackerStatePacket.TargetEntry> targets() {
      return this.targets;
   }

   private static List<ContractTrackerStatePacket.TargetEntry> copyTargets(List<ContractTrackerStatePacket.TargetEntry> input) {
      if (input != null && !input.isEmpty()) {
         List<ContractTrackerStatePacket.TargetEntry> result = new ArrayList<>();

         for (ContractTrackerStatePacket.TargetEntry entry : input) {
            if (entry != null) {
               if (result.size() >= 16) {
                  break;
               }

               result.add(entry);
            }
         }

         return List.copyOf(result);
      } else {
         return List.of();
      }
   }

   public record TargetEntry(
      String name, String selectedClass, int kills, int wins, int careerPercent, int difficulty, int price, int reward, int areaX, int areaZ, int areaRadius
   ) {
      public TargetEntry {
         name = name == null ? "" : name;
         selectedClass = selectedClass == null ? "" : selectedClass;
         kills = Math.max(0, kills);
         wins = Math.max(0, wins);
         careerPercent = Math.max(0, Math.min(100, careerPercent));
         difficulty = Math.max(0, difficulty);
         price = Math.max(0, price);
         reward = Math.max(0, reward);
         areaRadius = Math.max(0, areaRadius);
      }
   }
}
