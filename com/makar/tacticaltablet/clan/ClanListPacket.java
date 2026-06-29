package com.makar.tacticaltablet.clan;

import com.makar.tacticaltablet.tablet.client.TabletClientState;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent.Context;

public class ClanListPacket {
   private final List<ClanListPacket.ClanEntry> clans;

   public ClanListPacket(List<ClanListPacket.ClanEntry> clans) {
      this.clans = clans == null ? new ArrayList<>() : new ArrayList<>(clans);
   }

   public ClanListPacket(FriendlyByteBuf buf) {
      int size = Math.min(buf.m_130242_(), 128);
      this.clans = new ArrayList<>();

      for (int i = 0; i < size; i++) {
         String id = buf.m_130136_(64);
         String name = buf.m_130136_(24);
         String tag = buf.m_130136_(4);
         int color = buf.readInt();
         String ownerName = buf.m_130136_(24);
         String ownerUuid = buf.m_130136_(64);
         int memberCount = buf.m_130242_();
         int clanCoins = buf.m_130242_();
         boolean owner = buf.readBoolean();
         boolean member = buf.readBoolean();
         boolean pending = buf.readBoolean();
         int pendingSize = Math.min(buf.m_130242_(), 64);
         List<ClanListPacket.PendingEntry> pendingEntries = new ArrayList<>();

         for (int j = 0; j < pendingSize; j++) {
            pendingEntries.add(new ClanListPacket.PendingEntry(buf.m_130136_(64), buf.m_130136_(24)));
         }

         int memberSize = Math.min(buf.m_130242_(), 5);
         List<ClanListPacket.MemberEntry> memberEntries = new ArrayList<>();

         for (int j = 0; j < memberSize; j++) {
            memberEntries.add(new ClanListPacket.MemberEntry(buf.m_130136_(64), buf.m_130136_(24)));
         }

         this.clans
            .add(
               new ClanListPacket.ClanEntry(
                  id, name, tag, color, ownerName, ownerUuid, memberCount, clanCoins, owner, member, pending, pendingEntries, memberEntries
               )
            );
      }
   }

   public void encode(FriendlyByteBuf buf) {
      int size = Math.min(this.clans.size(), 128);
      buf.m_130130_(size);

      for (int i = 0; i < size; i++) {
         ClanListPacket.ClanEntry clan = this.clans.get(i);
         buf.m_130072_(limit(clan.id(), 64), 64);
         buf.m_130072_(limit(clan.name(), 24), 24);
         buf.m_130072_(limit(clan.tag(), 4), 4);
         buf.writeInt(clan.color());
         buf.m_130072_(limit(clan.ownerName(), 24), 24);
         buf.m_130072_(limit(clan.ownerUuid(), 64), 64);
         buf.m_130130_(Math.max(0, clan.memberCount()));
         buf.m_130130_(Math.max(0, clan.clanCoins()));
         buf.writeBoolean(clan.owner());
         buf.writeBoolean(clan.member());
         buf.writeBoolean(clan.pending());
         List<ClanListPacket.PendingEntry> pendingEntries = clan.pendingEntries() == null ? List.of() : clan.pendingEntries();
         int pendingSize = Math.min(pendingEntries.size(), 64);
         buf.m_130130_(pendingSize);

         for (int j = 0; j < pendingSize; j++) {
            ClanListPacket.PendingEntry pending = pendingEntries.get(j);
            buf.m_130072_(limit(pending.uuid(), 64), 64);
            buf.m_130072_(limit(pending.name(), 24), 24);
         }

         List<ClanListPacket.MemberEntry> memberEntries = clan.memberEntries() == null ? List.of() : clan.memberEntries();
         int memberSize = Math.min(memberEntries.size(), 5);
         buf.m_130130_(memberSize);

         for (int j = 0; j < memberSize; j++) {
            ClanListPacket.MemberEntry member = memberEntries.get(j);
            buf.m_130072_(limit(member.uuid(), 64), 64);
            buf.m_130072_(limit(member.name(), 24), 24);
         }
      }
   }

   public void handle(Supplier<Context> ctx) {
      ctx.get().enqueueWork(() -> TabletClientState.updateClans(this.clans));
      ctx.get().setPacketHandled(true);
   }

   private static String limit(String value, int max) {
      if (value == null) {
         return "";
      } else {
         return value.length() <= max ? value : value.substring(0, max);
      }
   }

   public record ClanEntry(
      String id,
      String name,
      String tag,
      int color,
      String ownerName,
      String ownerUuid,
      int memberCount,
      int clanCoins,
      boolean owner,
      boolean member,
      boolean pending,
      List<ClanListPacket.PendingEntry> pendingEntries,
      List<ClanListPacket.MemberEntry> memberEntries
   ) {
   }

   public record MemberEntry(String uuid, String name) {
   }

   public record PendingEntry(String uuid, String name) {
   }
}
