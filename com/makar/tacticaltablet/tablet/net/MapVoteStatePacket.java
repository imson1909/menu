package com.makar.tacticaltablet.tablet.net;

import com.makar.tacticaltablet.tablet.client.MapVoteClientState;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent.Context;

public final class MapVoteStatePacket {
   private static final int MAX_MAPS = 16;
   private static final int MAX_MAP_NAME_LENGTH = 64;
   private final boolean active;
   private final boolean openScreen;
   private final boolean operator;
   private final boolean nextSetCompetitive;
   private final boolean nextSetClanWar;
   private final int secondsLeft;
   private final String selectedMap;
   private final List<String> maps;
   private final Map<String, Integer> voteCounts;

   public MapVoteStatePacket(
      boolean active,
      boolean openScreen,
      boolean operator,
      boolean nextSetCompetitive,
      boolean nextSetClanWar,
      int secondsLeft,
      String selectedMap,
      List<String> maps,
      Map<String, Integer> voteCounts
   ) {
      this.active = active;
      this.openScreen = openScreen;
      this.operator = operator;
      this.nextSetCompetitive = nextSetCompetitive;
      this.nextSetClanWar = nextSetClanWar;
      this.secondsLeft = Math.max(0, secondsLeft);
      this.selectedMap = selectedMap == null ? "" : selectedMap;
      this.maps = sanitizeMaps(maps);
      this.voteCounts = sanitizeCounts(this.maps, voteCounts);
   }

   public MapVoteStatePacket(FriendlyByteBuf buf) {
      this.active = buf.readBoolean();
      this.openScreen = buf.readBoolean();
      this.operator = buf.readBoolean();
      this.nextSetCompetitive = buf.readBoolean();
      this.nextSetClanWar = buf.readBoolean();
      this.secondsLeft = Math.max(0, buf.readInt());
      this.selectedMap = buf.m_130136_(64);
      int size = buf.readInt();
      if (size >= 0 && size <= 16) {
         List<String> decodedMaps = new ArrayList<>();
         Map<String, Integer> decodedCounts = new LinkedHashMap<>();

         for (int i = 0; i < size; i++) {
            String map = buf.m_130136_(64);
            decodedMaps.add(map);
            decodedCounts.put(map, Math.max(0, buf.readInt()));
         }

         this.maps = List.copyOf(decodedMaps);
         this.voteCounts = Map.copyOf(decodedCounts);
      } else {
         throw new IllegalArgumentException("Invalid map vote pool size: " + size);
      }
   }

   public void encode(FriendlyByteBuf buf) {
      buf.writeBoolean(this.active);
      buf.writeBoolean(this.openScreen);
      buf.writeBoolean(this.operator);
      buf.writeBoolean(this.nextSetCompetitive);
      buf.writeBoolean(this.nextSetClanWar);
      buf.writeInt(this.secondsLeft);
      buf.m_130072_(this.selectedMap, 64);
      buf.writeInt(this.maps.size());

      for (String map : this.maps) {
         buf.m_130072_(map, 64);
         buf.writeInt(this.voteCounts.getOrDefault(map, 0));
      }
   }

   public void handle(Supplier<Context> contextSupplier) {
      Context context = contextSupplier.get();
      context.enqueueWork(
         () -> MapVoteClientState.update(
            this.active,
            this.openScreen,
            this.operator,
            this.nextSetCompetitive,
            this.nextSetClanWar,
            this.secondsLeft,
            this.selectedMap,
            this.maps,
            this.voteCounts
         )
      );
      context.setPacketHandled(true);
   }

   private static List<String> sanitizeMaps(List<String> input) {
      if (input != null && !input.isEmpty()) {
         List<String> result = new ArrayList<>();

         for (String map : input) {
            if (result.size() >= 16) {
               break;
            }

            if (map != null && !map.isBlank()) {
               result.add(map.trim());
            }
         }

         return List.copyOf(result);
      } else {
         return List.of();
      }
   }

   private static Map<String, Integer> sanitizeCounts(List<String> maps, Map<String, Integer> input) {
      Map<String, Integer> result = new LinkedHashMap<>();

      for (String map : maps) {
         result.put(map, Math.max(0, input == null ? 0 : input.getOrDefault(map, 0)));
      }

      return Map.copyOf(result);
   }
}
