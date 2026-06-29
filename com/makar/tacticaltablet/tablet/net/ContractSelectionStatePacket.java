package com.makar.tacticaltablet.tablet.net;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent.Context;

public class ContractSelectionStatePacket {
   private static final int MAX_TARGETS = 16;
   private static final int MAX_NAME_LENGTH = 32;
   private static final int MAX_CLASS_LENGTH = 32;
   private final boolean selectionActive;
   private final int selectionSecondsLeft;
   private final long cooldownLeftMs;
   private final boolean hasActiveContract;
   private final boolean soloMode;
   private final List<ContractSelectionStatePacket.TargetEntry> targets;

   public ContractSelectionStatePacket(
      boolean selectionActive,
      int selectionSecondsLeft,
      long cooldownLeftMs,
      boolean hasActiveContract,
      boolean soloMode,
      List<ContractSelectionStatePacket.TargetEntry> targets
   ) {
      this.selectionActive = selectionActive;
      this.selectionSecondsLeft = Math.max(0, selectionSecondsLeft);
      this.cooldownLeftMs = Math.max(0L, cooldownLeftMs);
      this.hasActiveContract = hasActiveContract;
      this.soloMode = soloMode;
      this.targets = copyTargets(targets);
   }

   public ContractSelectionStatePacket(FriendlyByteBuf buf) {
      this.selectionActive = buf.readBoolean();
      this.selectionSecondsLeft = buf.readInt();
      this.cooldownLeftMs = buf.readLong();
      this.hasActiveContract = buf.readBoolean();
      this.soloMode = buf.readBoolean();
      int size = buf.readInt();
      if (size >= 0 && size <= 16) {
         List<ContractSelectionStatePacket.TargetEntry> entries = new ArrayList<>();

         for (int i = 0; i < size; i++) {
            entries.add(
               new ContractSelectionStatePacket.TargetEntry(
                  buf.m_130259_(),
                  buf.m_130136_(32),
                  buf.m_130136_(32),
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
         throw new IllegalArgumentException("Invalid contract target count: " + size);
      }
   }

   public void encode(FriendlyByteBuf buf) {
      buf.writeBoolean(this.selectionActive);
      buf.writeInt(this.selectionSecondsLeft);
      buf.writeLong(this.cooldownLeftMs);
      buf.writeBoolean(this.hasActiveContract);
      buf.writeBoolean(this.soloMode);
      buf.writeInt(this.targets.size());

      for (ContractSelectionStatePacket.TargetEntry target : this.targets) {
         buf.m_130077_(target.uuid());
         buf.m_130072_(target.name(), 32);
         buf.m_130072_(target.selectedClass(), 32);
         buf.writeInt(target.kills());
         buf.writeInt(target.wins());
         buf.writeInt(target.careerPercent());
         buf.writeInt(target.difficulty());
         buf.writeInt(target.price());
         buf.writeInt(target.reward());
      }
   }

   public void handle(Supplier<Context> ctx) {
      ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> this.invokeClientHandler()));
      ctx.get().setPacketHandled(true);
   }

   private void invokeClientHandler() {
      try {
         Class<?> handler = Class.forName("com.makar.tacticaltablet.tablet.client.ContractClientPacketHandler");
         handler.getMethod("handleSelection", ContractSelectionStatePacket.class).invoke(null, this);
      } catch (ReflectiveOperationException exception) {
         throw new RuntimeException("Failed to handle contract selection packet on client", exception);
      }
   }

   public boolean selectionActive() {
      return this.selectionActive;
   }

   public int selectionSecondsLeft() {
      return this.selectionSecondsLeft;
   }

   public long cooldownLeftMs() {
      return this.cooldownLeftMs;
   }

   public boolean hasActiveContract() {
      return this.hasActiveContract;
   }

   public boolean soloMode() {
      return this.soloMode;
   }

   public List<ContractSelectionStatePacket.TargetEntry> targets() {
      return this.targets;
   }

   private static List<ContractSelectionStatePacket.TargetEntry> copyTargets(List<ContractSelectionStatePacket.TargetEntry> input) {
      List<ContractSelectionStatePacket.TargetEntry> result = new ArrayList<>();
      if (input != null && !input.isEmpty()) {
         for (ContractSelectionStatePacket.TargetEntry entry : input) {
            if (entry != null) {
               if (result.size() >= 16) {
                  break;
               }

               result.add(entry);
            }
         }

         return List.copyOf(result);
      } else {
         return result;
      }
   }

   public record TargetEntry(UUID uuid, String name, String selectedClass, int kills, int wins, int careerPercent, int difficulty, int price, int reward) {
   }
}
