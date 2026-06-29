package com.makar.tacticaltablet.tablet.net;

import com.makar.tacticaltablet.game.MatchMode;
import com.makar.tacticaltablet.game.MatchPhase;
import com.makar.tacticaltablet.tablet.client.TabletClientState;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent.Context;

public class TabletStatePacket {
   private static final int MAX_COOLDOWNS = 32;
   private static final int MAX_CLASS_ENTRIES = 64;
   private static final int MAX_CLASS_KEY_LENGTH = 64;
   private static final int MAX_TEAM_SLOT_ENTRIES = 32;
   private static final int MAX_TEAM_SLOT_KEY_LENGTH = 8;
   private static final int MAX_PLAYER_NAME_LENGTH = 32;
   private final Map<Integer, Long> cooldowns;
   private final boolean kitUsed;
   private final boolean rtpUsed;
   private final long rtpEndTime;
   private final Map<String, Integer> classLevels;
   private final Map<String, Integer> classXP;
   private final Map<String, Integer> classTiers;
   private final Map<String, Integer> unlockedBaseClasses;
   private final Map<String, Integer> purchasedClasses;
   private final boolean gameRunning;
   private final int wins;
   private final int kills;
   private final int deaths;
   private final int matchesPlayed;
   private final int coins;
   private final int careerProgressPercent;
   private final int lives;
   private final int alivePlayers;
   private final int remainingLivesTotal;
   private final int tabletAppearanceTier;
   private final MatchPhase matchPhase;
   private final MatchMode matchMode;
   private final MatchMode selectedVote;
   private final int voteTimeLeft;
   private final int soloVotes;
   private final int duoVotes;
   private final int trioVotes;
   private final int squadVotes;
   private final int voteOptionsMask;
   private final int teamSelectTimeLeft;
   private final int teamSlotSize;
   private final int selectedTeam;
   private final Map<String, String> teamSlots;
   private final boolean competitiveSet;

   public TabletStatePacket(
      Map<Integer, Long> cooldowns,
      boolean kitUsed,
      boolean rtpUsed,
      long rtpEndTime,
      Map<String, Integer> classLevels,
      Map<String, Integer> classXP,
      Map<String, Integer> classTiers,
      Map<String, Integer> unlockedBaseClasses,
      Map<String, Integer> purchasedClasses,
      boolean gameRunning,
      int wins,
      int kills,
      int deaths,
      int matchesPlayed,
      int coins,
      int careerProgressPercent,
      int lives,
      int alivePlayers,
      int remainingLivesTotal,
      int tabletAppearanceTier
   ) {
      this(
         cooldowns,
         kitUsed,
         rtpUsed,
         rtpEndTime,
         classLevels,
         classXP,
         classTiers,
         unlockedBaseClasses,
         purchasedClasses,
         gameRunning,
         wins,
         kills,
         deaths,
         matchesPlayed,
         coins,
         careerProgressPercent,
         lives,
         alivePlayers,
         remainingLivesTotal,
         tabletAppearanceTier,
         MatchPhase.WAITING,
         MatchMode.SOLO,
         null,
         0,
         0,
         0,
         0,
         0,
         MatchMode.voteMaskFor(0, false),
         0,
         1,
         -1,
         new HashMap<>(),
         false
      );
   }

   public TabletStatePacket(
      Map<Integer, Long> cooldowns,
      boolean kitUsed,
      boolean rtpUsed,
      long rtpEndTime,
      Map<String, Integer> classLevels,
      Map<String, Integer> classXP,
      Map<String, Integer> classTiers,
      Map<String, Integer> unlockedBaseClasses,
      Map<String, Integer> purchasedClasses,
      boolean gameRunning,
      int wins,
      int kills,
      int deaths,
      int matchesPlayed,
      int coins,
      int careerProgressPercent,
      int lives,
      int alivePlayers,
      int remainingLivesTotal,
      int tabletAppearanceTier,
      MatchPhase matchPhase,
      MatchMode matchMode,
      MatchMode selectedVote,
      int voteTimeLeft,
      int soloVotes,
      int duoVotes,
      int trioVotes,
      int squadVotes,
      int voteOptionsMask,
      int teamSelectTimeLeft,
      int teamSlotSize,
      int selectedTeam,
      Map<String, String> teamSlots,
      boolean competitiveSet
   ) {
      this.cooldowns = copyIntLongMap(cooldowns, 32);
      this.kitUsed = kitUsed;
      this.rtpUsed = rtpUsed;
      this.rtpEndTime = rtpEndTime;
      this.classLevels = copyStringIntMap(classLevels, 64);
      this.classXP = copyStringIntMap(classXP, 64);
      this.classTiers = copyStringIntMap(classTiers, 64);
      this.unlockedBaseClasses = copyStringIntMap(unlockedBaseClasses, 64);
      this.purchasedClasses = copyStringIntMap(purchasedClasses, 64);
      this.gameRunning = gameRunning;
      this.wins = wins;
      this.kills = kills;
      this.deaths = deaths;
      this.matchesPlayed = matchesPlayed;
      this.coins = coins;
      this.careerProgressPercent = careerProgressPercent;
      this.lives = Math.max(0, lives);
      this.alivePlayers = Math.max(0, alivePlayers);
      this.remainingLivesTotal = Math.max(0, remainingLivesTotal);
      this.tabletAppearanceTier = Math.max(0, tabletAppearanceTier);
      this.matchPhase = matchPhase == null ? MatchPhase.WAITING : matchPhase;
      this.matchMode = matchMode == null ? MatchMode.SOLO : matchMode;
      this.selectedVote = selectedVote;
      this.voteTimeLeft = Math.max(0, voteTimeLeft);
      this.soloVotes = Math.max(0, soloVotes);
      this.duoVotes = Math.max(0, duoVotes);
      this.trioVotes = Math.max(0, trioVotes);
      this.squadVotes = Math.max(0, squadVotes);
      this.voteOptionsMask = voteOptionsMask;
      this.teamSelectTimeLeft = Math.max(0, teamSelectTimeLeft);
      this.teamSlotSize = Math.max(1, teamSlotSize);
      this.selectedTeam = Math.max(-1, selectedTeam);
      this.teamSlots = copyStringStringMap(teamSlots, 32);
      this.competitiveSet = competitiveSet;
   }

   public TabletStatePacket(
      Map<Integer, Long> cooldowns,
      boolean kitUsed,
      boolean rtpUsed,
      long rtpEndTime,
      Map<String, Integer> classLevels,
      Map<String, Integer> classXP,
      boolean gameRunning
   ) {
      this(
         cooldowns,
         kitUsed,
         rtpUsed,
         rtpEndTime,
         classLevels,
         classXP,
         new HashMap<>(),
         new HashMap<>(),
         new HashMap<>(),
         gameRunning,
         0,
         0,
         0,
         0,
         0,
         0,
         0,
         0,
         0,
         0
      );
   }

   public TabletStatePacket(FriendlyByteBuf buf) {
      this.cooldowns = new HashMap<>();
      int cdSize = readBoundedSize(buf, 32, "cooldowns");

      for (int i = 0; i < cdSize; i++) {
         this.cooldowns.put(buf.readInt(), buf.readLong());
      }

      this.kitUsed = buf.readBoolean();
      this.rtpUsed = buf.readBoolean();
      this.rtpEndTime = buf.readLong();
      this.classLevels = new HashMap<>();
      int levelSize = readBoundedSize(buf, 64, "classLevels");

      for (int i = 0; i < levelSize; i++) {
         this.classLevels.put(buf.m_130136_(64), buf.readInt());
      }

      this.classXP = new HashMap<>();
      int xpSize = readBoundedSize(buf, 64, "classXP");

      for (int i = 0; i < xpSize; i++) {
         this.classXP.put(buf.m_130136_(64), buf.readInt());
      }

      this.classTiers = new HashMap<>();
      int tierSize = readBoundedSize(buf, 64, "classTiers");

      for (int i = 0; i < tierSize; i++) {
         this.classTiers.put(buf.m_130136_(64), buf.readInt());
      }

      this.unlockedBaseClasses = new HashMap<>();
      int unlockedSize = readBoundedSize(buf, 64, "unlockedBaseClasses");

      for (int i = 0; i < unlockedSize; i++) {
         this.unlockedBaseClasses.put(buf.m_130136_(64), buf.readInt());
      }

      this.purchasedClasses = new HashMap<>();
      int purchasedSize = readBoundedSize(buf, 64, "purchasedClasses");

      for (int i = 0; i < purchasedSize; i++) {
         this.purchasedClasses.put(buf.m_130136_(64), buf.readInt());
      }

      this.gameRunning = buf.readBoolean();
      this.wins = buf.readInt();
      this.kills = buf.readInt();
      this.deaths = buf.readInt();
      this.matchesPlayed = buf.readInt();
      this.coins = buf.readInt();
      this.careerProgressPercent = buf.readInt();
      this.lives = buf.readInt();
      this.alivePlayers = buf.readInt();
      this.remainingLivesTotal = buf.readInt();
      this.tabletAppearanceTier = buf.readInt();
      this.matchPhase = MatchPhase.byId(buf.readByte());
      this.matchMode = MatchMode.byId(buf.readByte());
      int selectedVoteId = buf.readByte();
      this.selectedVote = selectedVoteId < 0 ? null : MatchMode.byId(selectedVoteId);
      this.voteTimeLeft = buf.readInt();
      this.soloVotes = buf.readInt();
      this.duoVotes = buf.readInt();
      this.trioVotes = buf.readInt();
      this.squadVotes = buf.readInt();
      this.voteOptionsMask = buf.readInt();
      this.teamSelectTimeLeft = buf.readInt();
      this.teamSlotSize = buf.readInt();
      this.selectedTeam = buf.readInt();
      this.teamSlots = new HashMap<>();
      int teamSlotEntries = readBoundedSize(buf, 32, "teamSlots");

      for (int i = 0; i < teamSlotEntries; i++) {
         this.teamSlots.put(buf.m_130136_(8), buf.m_130136_(32));
      }

      this.competitiveSet = buf.readBoolean();
   }

   public void encode(FriendlyByteBuf buf) {
      buf.writeInt(this.cooldowns.size());

      for (Entry<Integer, Long> entry : this.cooldowns.entrySet()) {
         buf.writeInt(entry.getKey());
         buf.writeLong(entry.getValue());
      }

      buf.writeBoolean(this.kitUsed);
      buf.writeBoolean(this.rtpUsed);
      buf.writeLong(this.rtpEndTime);
      buf.writeInt(this.classLevels.size());

      for (Entry<String, Integer> entry : this.classLevels.entrySet()) {
         buf.m_130070_(entry.getKey());
         buf.writeInt(entry.getValue());
      }

      buf.writeInt(this.classXP.size());

      for (Entry<String, Integer> entry : this.classXP.entrySet()) {
         buf.m_130070_(entry.getKey());
         buf.writeInt(entry.getValue());
      }

      buf.writeInt(this.classTiers.size());

      for (Entry<String, Integer> entry : this.classTiers.entrySet()) {
         buf.m_130070_(entry.getKey());
         buf.writeInt(entry.getValue());
      }

      buf.writeInt(this.unlockedBaseClasses.size());

      for (Entry<String, Integer> entry : this.unlockedBaseClasses.entrySet()) {
         buf.m_130070_(entry.getKey());
         buf.writeInt(entry.getValue());
      }

      buf.writeInt(this.purchasedClasses.size());

      for (Entry<String, Integer> entry : this.purchasedClasses.entrySet()) {
         buf.m_130070_(entry.getKey());
         buf.writeInt(entry.getValue());
      }

      buf.writeBoolean(this.gameRunning);
      buf.writeInt(this.wins);
      buf.writeInt(this.kills);
      buf.writeInt(this.deaths);
      buf.writeInt(this.matchesPlayed);
      buf.writeInt(this.coins);
      buf.writeInt(this.careerProgressPercent);
      buf.writeInt(this.lives);
      buf.writeInt(this.alivePlayers);
      buf.writeInt(this.remainingLivesTotal);
      buf.writeInt(this.tabletAppearanceTier);
      buf.writeByte(this.matchPhase.ordinal());
      buf.writeByte(this.matchMode.ordinal());
      buf.writeByte(this.selectedVote == null ? -1 : this.selectedVote.ordinal());
      buf.writeInt(this.voteTimeLeft);
      buf.writeInt(this.soloVotes);
      buf.writeInt(this.duoVotes);
      buf.writeInt(this.trioVotes);
      buf.writeInt(this.squadVotes);
      buf.writeInt(this.voteOptionsMask);
      buf.writeInt(this.teamSelectTimeLeft);
      buf.writeInt(this.teamSlotSize);
      buf.writeInt(this.selectedTeam);
      buf.writeInt(this.teamSlots.size());

      for (Entry<String, String> entry : this.teamSlots.entrySet()) {
         buf.m_130072_(entry.getKey(), 8);
         buf.m_130072_(entry.getValue(), 32);
      }

      buf.writeBoolean(this.competitiveSet);
   }

   public void handle(Supplier<Context> ctx) {
      ctx.get()
         .enqueueWork(
            () -> {
               TabletClientState.update(this.cooldowns, this.kitUsed, this.rtpUsed, this.rtpEndTime);
               TabletClientState.updateLevels(this.classLevels);
               TabletClientState.updateXP(this.classXP);
               TabletClientState.updateClassTiers(this.classTiers);
               TabletClientState.updateUnlockedBaseClasses(this.unlockedBaseClasses);
               TabletClientState.updatePurchasedClasses(this.purchasedClasses);
               TabletClientState.updateGameRunning(this.gameRunning);
               TabletClientState.updateProfileStats(this.wins, this.kills, this.deaths, this.matchesPlayed, this.coins, this.careerProgressPercent);
               TabletClientState.updateLives(this.lives);
               TabletClientState.updateMatchCounts(this.alivePlayers, this.remainingLivesTotal);
               TabletClientState.updateTabletAppearanceTier(this.tabletAppearanceTier);
               TabletClientState.updateMatchSetup(
                  this.matchPhase,
                  this.matchMode,
                  this.selectedVote,
                  this.voteTimeLeft,
                  this.soloVotes,
                  this.duoVotes,
                  this.trioVotes,
                  this.squadVotes,
                  this.voteOptionsMask,
                  this.teamSelectTimeLeft,
                  this.teamSlotSize,
                  this.selectedTeam,
                  this.teamSlots
               );
               TabletClientState.updateCompetitiveSet(this.competitiveSet);
               if (this.kitUsed && this.rtpUsed) {
                  TabletClientState.requestClose();
               }
            }
         );
      ctx.get().setPacketHandled(true);
   }

   private static int readBoundedSize(FriendlyByteBuf buf, int max, String field) {
      int size = buf.readInt();
      if (size >= 0 && size <= max) {
         return size;
      } else {
         throw new IllegalArgumentException("Invalid " + field + " size: " + size + " max=" + max);
      }
   }

   private static Map<Integer, Long> copyIntLongMap(Map<Integer, Long> input, int maxEntries) {
      Map<Integer, Long> result = new HashMap<>();
      if (input != null && !input.isEmpty()) {
         for (Entry<Integer, Long> entry : input.entrySet()) {
            if (result.size() >= maxEntries) {
               break;
            }

            if (entry.getKey() != null && entry.getValue() != null) {
               result.put(entry.getKey(), entry.getValue());
            }
         }

         return result;
      } else {
         return result;
      }
   }

   private static Map<String, Integer> copyStringIntMap(Map<String, Integer> input, int maxEntries) {
      Map<String, Integer> result = new HashMap<>();
      if (input != null && !input.isEmpty()) {
         for (Entry<String, Integer> entry : input.entrySet()) {
            if (result.size() >= maxEntries) {
               break;
            }

            if (entry.getKey() != null && entry.getValue() != null) {
               String key = entry.getKey();
               if (key.length() > 64) {
                  key = key.substring(0, 64);
               }

               result.put(key, entry.getValue());
            }
         }

         return result;
      } else {
         return result;
      }
   }

   private static Map<String, String> copyStringStringMap(Map<String, String> input, int maxEntries) {
      Map<String, String> result = new HashMap<>();
      if (input != null && !input.isEmpty()) {
         for (Entry<String, String> entry : input.entrySet()) {
            if (result.size() >= maxEntries) {
               break;
            }

            if (entry.getKey() != null && entry.getValue() != null) {
               String key = entry.getKey();
               if (key.length() > 8) {
                  key = key.substring(0, 8);
               }

               String value = entry.getValue();
               if (value.length() > 32) {
                  value = value.substring(0, 32);
               }

               result.put(key, value);
            }
         }

         return result;
      } else {
         return result;
      }
   }
}
