package com.makar.tacticaltablet.clan;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import com.makar.tacticaltablet.core.TacticalTabletMod;
import com.makar.tacticaltablet.game.GameStateManager;
import com.makar.tacticaltablet.game.MapSetManager;
import com.makar.tacticaltablet.game.team.TeamMatchManager;
import com.makar.tacticaltablet.progression.ClassXPManager;
import com.makar.tacticaltablet.progression.PlayerProgressManager;
import com.makar.tacticaltablet.tablet.net.PacketHandler;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.LevelResource;

public class ClanManager {
   public static final int CREATE_COST = 1000;
   public static final int[] ALLOWED_COLORS = ClanConstants.ALLOWED_COLORS;
   private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
   private static final String NEW_DATA_DIRECTORY = "tacticaltabletdata";
   private static final String LEGACY_DATA_DIRECTORY = "tacticaltablet_data";
   private static final String CLANS_FILE = "clans.json";
   private static final DateTimeFormatter BROKEN_FILE_TIMESTAMP = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");
   private static Path clansFile;
   private static ClanManager.ClanStorage storage = new ClanManager.ClanStorage();
   private static boolean loaded;

   public static synchronized void sync(ServerPlayer player) {
      if (player != null) {
         init(player.f_8924_);
         PacketHandler.sendToPlayer(player, new ClanListPacket(buildEntries(player)));
      }
   }

   public static synchronized void syncAll(MinecraftServer server) {
      if (server != null) {
         init(server);

         for (ServerPlayer player : server.m_6846_().m_11314_()) {
            sync(player);
         }
      }
   }

   public static synchronized ClanManager.Result createClan(ServerPlayer player, String rawName, int color, String rawTag) {
      if (player == null) {
         return ClanManager.Result.INVALID;
      }

      init(player.f_8924_);
      String name = normalizeName(rawName);
      String tag = normalizeTag(rawTag);
      if (name.length() >= 3 && name.length() <= 24 && !tag.isBlank() && tag.length() <= 4 && isAllowedColor(color)) {
         String playerId = player.m_20148_().toString();
         if (findClanByMember(playerId) != null) {
            return ClanManager.Result.ALREADY_IN_CLAN;
         }

         String normalizedName = name.toLowerCase(Locale.ROOT);
         String normalizedTag = tag.toLowerCase(Locale.ROOT);

         for (ClanManager.ClanData clan : storage.clans) {
            if (clan.name.toLowerCase(Locale.ROOT).equals(normalizedName) || clan.tag.toLowerCase(Locale.ROOT).equals(normalizedTag)) {
               return ClanManager.Result.NAME_TAKEN;
            }
         }

         if (PlayerProgressManager.getCoins(player) < 1000) {
            return ClanManager.Result.NOT_ENOUGH_COINS;
         }

         PlayerProgressManager.addCoins(player, -1000);
         PlayerProgressManager.savePlayer(player);
         ClassXPManager.sync(player);
         ClanManager.ClanData clan = new ClanManager.ClanData();
         clan.id = UUID.randomUUID().toString();
         clan.name = name;
         clan.tag = tag.toUpperCase(Locale.ROOT);
         clan.color = color;
         clan.ownerUuid = playerId;
         clan.ownerName = player.m_36316_().getName();
         clan.members.add(new ClanManager.ClanPlayerEntry(playerId, player.m_36316_().getName()));
         storage.clans.add(clan);
         save();
         syncAll(player.f_8924_);
         return ClanManager.Result.SUCCESS;
      } else {
         return ClanManager.Result.INVALID;
      }
   }

   public static synchronized ClanManager.Result requestJoin(ServerPlayer player, String clanId) {
      if (player != null && clanId != null && !clanId.isBlank()) {
         init(player.f_8924_);
         ClanManager.ClanData clan = findClan(clanId);
         if (clan == null) {
            return ClanManager.Result.NOT_FOUND;
         }

         String playerId = player.m_20148_().toString();
         if (findClanByMember(playerId) != null) {
            return ClanManager.Result.ALREADY_IN_CLAN;
         }

         if (containsUuid(clan.pending, playerId)) {
            return ClanManager.Result.ALREADY_PENDING;
         }

         clan.pending.add(new ClanManager.ClanPlayerEntry(playerId, player.m_36316_().getName()));
         save();
         ServerPlayer owner = getOnlinePlayer(player.f_8924_, clan.ownerUuid);
         if (owner != null) {
            owner.m_213846_(Component.m_237113_("[WAR] " + player.m_36316_().getName() + " отправил заявку в клан " + clan.name + "."));
            sync(owner);
         }

         sync(player);
         return ClanManager.Result.SUCCESS;
      } else {
         return ClanManager.Result.INVALID;
      }
   }

   public static synchronized ClanManager.Result acceptJoin(ServerPlayer owner, String clanId, String applicantUuid) {
      if (owner != null && clanId != null && applicantUuid != null) {
         init(owner.f_8924_);
         ClanManager.ClanData clan = findClan(clanId);
         if (clan == null) {
            return ClanManager.Result.NOT_FOUND;
         }

         if (!owner.m_20148_().toString().equals(clan.ownerUuid)) {
            return ClanManager.Result.NOT_OWNER;
         }

         ClanManager.ClanPlayerEntry applicant = findEntry(clan.pending, applicantUuid);
         if (applicant == null) {
            return ClanManager.Result.NOT_FOUND;
         }

         if (clan.members.size() >= 5) {
            return ClanManager.Result.CLAN_FULL;
         }

         if (findClanByMember(applicantUuid) != null) {
            removeEntry(clan.pending, applicantUuid);
            save();
            sync(owner);
            return ClanManager.Result.ALREADY_IN_CLAN;
         }

         removeEntry(clan.pending, applicantUuid);
         clan.members.add(new ClanManager.ClanPlayerEntry(applicant.uuid, applicant.name));
         save();
         ServerPlayer applicantPlayer = getOnlinePlayer(owner.f_8924_, applicantUuid);
         if (applicantPlayer != null) {
            applicantPlayer.m_213846_(Component.m_237113_("[WAR] Ваша заявка в клан " + clan.name + " принята."));
            if (MapSetManager.isClanWarSet() && GameStateManager.isRunning(owner.f_8924_)) {
               TeamMatchManager.assignClanWarPlayer(owner.f_8924_, applicantPlayer);
               TeamMatchManager.applyScoreboardTeams(owner.f_8924_);
            }

            sync(applicantPlayer);
         }

         sync(owner);
         return ClanManager.Result.SUCCESS;
      } else {
         return ClanManager.Result.INVALID;
      }
   }

   public static synchronized ClanManager.Result rejectJoin(ServerPlayer owner, String clanId, String applicantUuid) {
      if (owner != null && clanId != null && applicantUuid != null) {
         init(owner.f_8924_);
         ClanManager.ClanData clan = findClan(clanId);
         if (clan == null) {
            return ClanManager.Result.NOT_FOUND;
         }

         if (!owner.m_20148_().toString().equals(clan.ownerUuid)) {
            return ClanManager.Result.NOT_OWNER;
         }

         ClanManager.ClanPlayerEntry applicant = findEntry(clan.pending, applicantUuid);
         if (applicant == null) {
            return ClanManager.Result.NOT_FOUND;
         }

         removeEntry(clan.pending, applicantUuid);
         save();
         ServerPlayer applicantPlayer = getOnlinePlayer(owner.f_8924_, applicantUuid);
         if (applicantPlayer != null) {
            applicantPlayer.m_213846_(Component.m_237113_("[WAR] Заявка в клан " + clan.name + " отклонена."));
            sync(applicantPlayer);
         }

         sync(owner);
         return ClanManager.Result.SUCCESS;
      } else {
         return ClanManager.Result.INVALID;
      }
   }

   public static synchronized ClanManager.Result leaveCurrentClan(ServerPlayer player) {
      if (player == null) {
         return ClanManager.Result.INVALID;
      }

      init(player.f_8924_);
      if (MapSetManager.isClanWarSet()) {
         return ClanManager.Result.CLAN_WAR_LOCKED;
      }

      String playerId = player.m_20148_().toString();
      ClanManager.ClanData clan = findClanByMember(playerId);
      if (clan == null) {
         return ClanManager.Result.NOT_FOUND;
      }

      if (playerId.equals(clan.ownerUuid)) {
         return ClanManager.Result.OWNER_CANNOT_LEAVE;
      }

      removeEntry(clan.members, playerId);
      save();
      syncAll(player.f_8924_);
      return ClanManager.Result.SUCCESS;
   }

   public static synchronized ClanManager.Result disbandClan(ServerPlayer owner, String clanId) {
      if (owner != null && clanId != null && !clanId.isBlank()) {
         init(owner.f_8924_);
         if (MapSetManager.isClanWarSet()) {
            return ClanManager.Result.CLAN_WAR_LOCKED;
         }

         ClanManager.ClanData clan = findClan(clanId);
         if (clan == null) {
            return ClanManager.Result.NOT_FOUND;
         }

         if (!owner.m_20148_().toString().equals(clan.ownerUuid)) {
            return ClanManager.Result.NOT_OWNER;
         }

         storage.clans.remove(clan);
         save();

         for (ClanManager.ClanPlayerEntry member : clan.members) {
            if (!member.uuid.equals(owner.m_20148_().toString())) {
               ServerPlayer player = getOnlinePlayer(owner.f_8924_, member.uuid);
               if (player != null) {
                  player.m_213846_(Component.m_237113_("[WAR] Клан " + clan.name + " распущен."));
               }
            }
         }

         syncAll(owner.f_8924_);
         return ClanManager.Result.SUCCESS;
      } else {
         return ClanManager.Result.INVALID;
      }
   }

   public static synchronized ClanManager.Result kickMember(ServerPlayer owner, String clanId, String memberUuid) {
      if (owner != null && clanId != null && memberUuid != null) {
         init(owner.f_8924_);
         if (MapSetManager.isClanWarSet()) {
            return ClanManager.Result.CLAN_WAR_LOCKED;
         }

         ClanManager.ClanData clan = findClan(clanId);
         if (clan == null) {
            return ClanManager.Result.NOT_FOUND;
         }

         if (!owner.m_20148_().toString().equals(clan.ownerUuid)) {
            return ClanManager.Result.NOT_OWNER;
         }

         if (memberUuid.equals(clan.ownerUuid)) {
            return ClanManager.Result.CANNOT_KICK_OWNER;
         }

         ClanManager.ClanPlayerEntry member = findEntry(clan.members, memberUuid);
         if (member == null) {
            return ClanManager.Result.NOT_FOUND;
         }

         removeEntry(clan.members, memberUuid);
         save();
         ServerPlayer kicked = getOnlinePlayer(owner.f_8924_, memberUuid);
         if (kicked != null) {
            kicked.m_213846_(Component.m_237113_("[WAR] Вас исключили из клана " + clan.name + "."));
            sync(kicked);
         }

         sync(owner);
         return ClanManager.Result.SUCCESS;
      } else {
         return ClanManager.Result.INVALID;
      }
   }

   public static synchronized String getClanIdForPlayer(ServerPlayer player) {
      if (player == null) {
         return "";
      }

      init(player.f_8924_);
      ClanManager.ClanData clan = findClanByMember(player.m_20148_().toString());
      return clan == null ? "" : clan.id;
   }

   public static synchronized String getClanNameForPlayer(ServerPlayer player) {
      if (player == null) {
         return "";
      }

      init(player.f_8924_);
      ClanManager.ClanData clan = findClanByMember(player.m_20148_().toString());
      return clan == null ? "" : clan.name;
   }

   public static synchronized String getClanIdForPlayerUuid(MinecraftServer server, String playerUuid) {
      if (server != null && playerUuid != null && !playerUuid.isBlank()) {
         init(server);
         ClanManager.ClanData clan = findClanByMember(playerUuid);
         return clan == null ? "" : clan.id;
      } else {
         return "";
      }
   }

   public static synchronized String getClanNameById(MinecraftServer server, String clanId) {
      if (server != null && clanId != null && !clanId.isBlank()) {
         init(server);
         ClanManager.ClanData clan = findClan(clanId);
         return clan == null ? "" : clan.name;
      } else {
         return "";
      }
   }

   public static synchronized int getClanCoinsById(MinecraftServer server, String clanId) {
      if (server != null && clanId != null && !clanId.isBlank()) {
         init(server);
         ClanManager.ClanData clan = findClan(clanId);
         return clan == null ? 0 : Math.max(0, clan.clanCoins);
      } else {
         return 0;
      }
   }

   public static synchronized boolean addClanCoins(MinecraftServer server, String clanId, int amount) {
      if (server != null && clanId != null && !clanId.isBlank() && amount != 0) {
         init(server);
         ClanManager.ClanData clan = findClan(clanId);
         if (clan == null) {
            return false;
         }

         clan.clanCoins = Math.max(0, clan.clanCoins + amount);
         save();
         syncAll(server);
         return true;
      } else {
         return false;
      }
   }

   public static synchronized boolean isClanOwner(ServerPlayer player) {
      if (player == null) {
         return false;
      }

      init(player.f_8924_);
      ClanManager.ClanData clan = findClanByMember(player.m_20148_().toString());
      return clan != null && player.m_20148_().toString().equals(clan.ownerUuid);
   }

   private static List<ClanListPacket.ClanEntry> buildEntries(ServerPlayer viewer) {
      String viewerId = viewer.m_20148_().toString();
      List<ClanListPacket.ClanEntry> entries = new ArrayList<>();
      storage.clans
         .stream()
         .sorted(Comparator.comparing(clan -> clan.name.toLowerCase(Locale.ROOT)))
         .limit(128L)
         .forEach(
            clan -> entries.add(
               new ClanListPacket.ClanEntry(
                  clan.id,
                  clan.name,
                  clan.tag,
                  clan.color,
                  clan.ownerName,
                  clan.ownerUuid,
                  clan.members.size(),
                  Math.max(0, clan.clanCoins),
                  clan.ownerUuid.equals(viewerId),
                  containsUuid(clan.members, viewerId),
                  containsUuid(clan.pending, viewerId),
                  buildPendingEntries(clan, viewerId),
                  buildMemberEntries(clan)
               )
            )
         );
      return entries;
   }

   private static List<ClanListPacket.PendingEntry> buildPendingEntries(ClanManager.ClanData clan, String viewerId) {
      List<ClanListPacket.PendingEntry> entries = new ArrayList<>();
      if (!clan.ownerUuid.equals(viewerId)) {
         return entries;
      }

      for (ClanManager.ClanPlayerEntry pending : clan.pending) {
         entries.add(new ClanListPacket.PendingEntry(pending.uuid, pending.name));
         if (entries.size() >= 64) {
            break;
         }
      }

      return entries;
   }

   private static List<ClanListPacket.MemberEntry> buildMemberEntries(ClanManager.ClanData clan) {
      List<ClanListPacket.MemberEntry> entries = new ArrayList<>();

      for (ClanManager.ClanPlayerEntry member : clan.members) {
         entries.add(new ClanListPacket.MemberEntry(member.uuid, member.name));
         if (entries.size() >= 5) {
            break;
         }
      }

      return entries;
   }

   private static void init(MinecraftServer server) {
      if (!loaded && server != null) {
         Path serverRoot = getServerRoot(server);
         Path dataRoot = serverRoot.resolve("tacticaltabletdata");
         clansFile = dataRoot.resolve("clans.json");
         migrateLegacyFile(server, clansFile);
         ClanManager.ClanStorage loadedStorage = new ClanManager.ClanStorage();
         boolean readOk = true;

         try {
            Files.createDirectories(dataRoot);
            if (Files.exists(clansFile)) {
               try (Reader reader = Files.newBufferedReader(clansFile, StandardCharsets.UTF_8)) {
                  ClanManager.ClanStorage read = (ClanManager.ClanStorage)GSON.fromJson(reader, ClanManager.ClanStorage.class);
                  loadedStorage = read == null ? new ClanManager.ClanStorage() : read;
               }
            }
         } catch (JsonSyntaxException | IOException exception) {
            readOk = false;
            TacticalTabletMod.LOGGER.error("Failed to load Tactical Tablet clans from {}", clansFile, exception);
            backupBrokenFile(clansFile);
         }

         storage = loadedStorage;
         boolean changed = normalizeStorage();
         loaded = true;
         if (readOk && changed) {
            save();
         }
      }
   }

   private static Path getServerRoot(MinecraftServer server) {
      Path worldRoot = server.m_129843_(LevelResource.f_78182_).toAbsolutePath().normalize();
      Path parent = worldRoot.getParent();
      return parent == null ? worldRoot : parent;
   }

   private static void migrateLegacyFile(MinecraftServer server, Path targetFile) {
      Path legacyFile = server.m_129843_(LevelResource.f_78182_).resolve("tacticaltablet_data").resolve("clans.json").toAbsolutePath().normalize();
      if (!Files.exists(targetFile) && Files.exists(legacyFile)) {
         try {
            Files.createDirectories(targetFile.getParent());
            Files.copy(legacyFile, targetFile, StandardCopyOption.REPLACE_EXISTING);
            TacticalTabletMod.LOGGER.info("Migrated Tactical Tablet clans from {} to {}", legacyFile, targetFile);
         } catch (IOException exception) {
            TacticalTabletMod.LOGGER.error("Failed to migrate Tactical Tablet clans from {} to {}", new Object[]{legacyFile, targetFile, exception});
         }
      }
   }

   private static void save() {
      if (clansFile != null) {
         Path tmp = clansFile.resolveSibling("clans.json.tmp");

         try {
            Files.createDirectories(clansFile.getParent());

            try (Writer writer = Files.newBufferedWriter(tmp, StandardCharsets.UTF_8)) {
               GSON.toJson(storage, writer);
            }

            moveReplace(tmp, clansFile);
         } catch (IOException exception) {
            TacticalTabletMod.LOGGER.error("Failed to save Tactical Tablet clans to {}", clansFile, exception);

            try {
               Files.deleteIfExists(tmp);
            } catch (IOException var4) {
            }
         }
      }
   }

   private static void moveReplace(Path source, Path target) throws IOException {
      try {
         Files.move(source, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
      } catch (AtomicMoveNotSupportedException exception) {
         Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
      }
   }

   private static void backupBrokenFile(Path file) {
      if (file != null && Files.exists(file)) {
         String timestamp = BROKEN_FILE_TIMESTAMP.format(LocalDateTime.now());
         Path broken = file.resolveSibling("clans.json.broken." + timestamp);

         try {
            Files.move(file, broken, StandardCopyOption.REPLACE_EXISTING);
            TacticalTabletMod.LOGGER.warn("Moved broken Tactical Tablet clans file to {}", broken);
         } catch (IOException exception) {
            TacticalTabletMod.LOGGER.error("Failed to move broken Tactical Tablet clans file {}", file, exception);
         }
      }
   }

   private static boolean normalizeStorage() {
      boolean changed = false;
      if (storage.clans == null) {
         storage.clans = new ArrayList<>();
         changed = true;
      }

      Set<String> ids = new HashSet<>();
      Set<String> names = new HashSet<>();
      Set<String> tags = new HashSet<>();
      Iterator<ClanManager.ClanData> iterator = storage.clans.iterator();

      while (iterator.hasNext()) {
         ClanManager.ClanData clan = iterator.next();
         if (clan == null) {
            iterator.remove();
            changed = true;
         } else {
            changed |= migrateLegacyEntries(clan);
            changed |= normalizeClan(clan);
            String nameKey = clan.name.toLowerCase(Locale.ROOT);
            String tagKey = clan.tag.toLowerCase(Locale.ROOT);
            if (!ids.add(clan.id) || !names.add(nameKey) || !tags.add(tagKey)) {
               TacticalTabletMod.LOGGER.warn("Removing duplicate Tactical Tablet clan {} [{}]", clan.name, clan.tag);
               iterator.remove();
               changed = true;
            } else if (clan.name.isBlank() || clan.tag.isBlank() || parseUuidOrNull(clan.ownerUuid) == null) {
               TacticalTabletMod.LOGGER.warn("Removing invalid Tactical Tablet clan with id {}", clan.id);
               iterator.remove();
               changed = true;
            }
         }
      }

      return changed;
   }

   private static boolean migrateLegacyEntries(ClanManager.ClanData clan) {
      boolean changed = false;
      if (clan.members == null) {
         clan.members = new ArrayList<>();
      }

      if (clan.pending == null) {
         clan.pending = new ArrayList<>();
      }

      if (clan.memberUuids != null) {
         for (int i = 0; i < clan.memberUuids.size(); i++) {
            String uuid = clan.memberUuids.get(i);
            String name = clan.memberNames != null && i < clan.memberNames.size() ? clan.memberNames.get(i) : uuid;
            if (!containsUuid(clan.members, uuid)) {
               clan.members.add(new ClanManager.ClanPlayerEntry(uuid, name));
            }
         }

         clan.memberUuids = null;
         clan.memberNames = null;
         changed = true;
      }

      if (clan.pendingUuids != null) {
         for (int i = 0; i < clan.pendingUuids.size(); i++) {
            String uuid = clan.pendingUuids.get(i);
            String name = clan.pendingNames != null && i < clan.pendingNames.size() ? clan.pendingNames.get(i) : uuid;
            if (!containsUuid(clan.pending, uuid)) {
               clan.pending.add(new ClanManager.ClanPlayerEntry(uuid, name));
            }
         }

         clan.pendingUuids = null;
         clan.pendingNames = null;
         changed = true;
      }

      return changed;
   }

   private static boolean normalizeClan(ClanManager.ClanData clan) {
      boolean changed = false;
      if (parseUuidOrNull(clan.id) == null) {
         clan.id = UUID.randomUUID().toString();
         changed = true;
      }

      String normalizedName = normalizeName(clan.name);
      if (!Objects.equals(clan.name, normalizedName)) {
         clan.name = normalizedName;
         changed = true;
      }

      String normalizedTag = normalizeTag(clan.tag).toUpperCase(Locale.ROOT);
      if (!Objects.equals(clan.tag, normalizedTag)) {
         clan.tag = normalizedTag;
         changed = true;
      }

      if (!isAllowedColor(clan.color)) {
         clan.color = ClanConstants.ALLOWED_COLORS[0];
         changed = true;
      }

      if (clan.clanCoins < 0) {
         clan.clanCoins = 0;
         changed = true;
      }

      if (clan.ownerName == null || clan.ownerName.isBlank()) {
         clan.ownerName = "Unknown";
         changed = true;
      }

      changed |= normalizeEntries(clan.members);
      changed |= normalizeEntries(clan.pending);
      if (parseUuidOrNull(clan.ownerUuid) != null && !containsUuid(clan.members, clan.ownerUuid)) {
         clan.members.add(0, new ClanManager.ClanPlayerEntry(clan.ownerUuid, clan.ownerName));
         changed = true;
      }

      if (parseUuidOrNull(clan.ownerUuid) == null && !clan.members.isEmpty()) {
         ClanManager.ClanPlayerEntry first = clan.members.get(0);
         clan.ownerUuid = first.uuid;
         clan.ownerName = first.name;
         changed = true;
      }

      if (removeMembersFromPending(clan)) {
         changed = true;
      }

      return changed;
   }

   private static boolean normalizeEntries(List<ClanManager.ClanPlayerEntry> entries) {
      boolean changed = false;
      Set<String> seen = new HashSet<>();
      Iterator<ClanManager.ClanPlayerEntry> iterator = entries.iterator();

      while (iterator.hasNext()) {
         ClanManager.ClanPlayerEntry entry = iterator.next();
         if (entry != null && parseUuidOrNull(entry.uuid) != null && seen.add(entry.uuid)) {
            String name = normalizeName(entry.name);
            if (name.isBlank()) {
               name = entry.uuid;
            }

            if (!Objects.equals(entry.name, name)) {
               entry.name = name;
               changed = true;
            }
         } else {
            iterator.remove();
            changed = true;
         }
      }

      return changed;
   }

   private static boolean removeMembersFromPending(ClanManager.ClanData clan) {
      boolean changed = false;
      Iterator<ClanManager.ClanPlayerEntry> iterator = clan.pending.iterator();

      while (iterator.hasNext()) {
         ClanManager.ClanPlayerEntry pending = iterator.next();
         if (containsUuid(clan.members, pending.uuid)) {
            iterator.remove();
            changed = true;
         }
      }

      return changed;
   }

   private static ClanManager.ClanData findClan(String clanId) {
      for (ClanManager.ClanData clan : storage.clans) {
         if (clan.id.equals(clanId)) {
            return clan;
         }
      }

      return null;
   }

   private static ClanManager.ClanData findClanByMember(String playerId) {
      for (ClanManager.ClanData clan : storage.clans) {
         if (containsUuid(clan.members, playerId)) {
            return clan;
         }
      }

      return null;
   }

   private static ClanManager.ClanPlayerEntry findEntry(List<ClanManager.ClanPlayerEntry> entries, String uuid) {
      for (ClanManager.ClanPlayerEntry entry : entries) {
         if (entry.uuid.equals(uuid)) {
            return entry;
         }
      }

      return null;
   }

   private static boolean containsUuid(List<ClanManager.ClanPlayerEntry> entries, String uuid) {
      return findEntry(entries, uuid) != null;
   }

   private static void removeEntry(List<ClanManager.ClanPlayerEntry> entries, String uuid) {
      entries.removeIf(entry -> entry.uuid.equals(uuid));
   }

   private static ServerPlayer getOnlinePlayer(MinecraftServer server, String uuid) {
      UUID parsed = parseUuidOrNull(uuid);
      return parsed == null ? null : server.m_6846_().m_11259_(parsed);
   }

   private static UUID parseUuidOrNull(String value) {
      if (value != null && !value.isBlank()) {
         try {
            return UUID.fromString(value);
         } catch (IllegalArgumentException exception) {
            return null;
         }
      } else {
         return null;
      }
   }

   private static String normalizeName(String value) {
      return value == null ? "" : value.trim().replaceAll("\\s+", " ");
   }

   private static String normalizeTag(String value) {
      return value == null ? "" : value.trim().replaceAll("[^A-Za-z0-9А-Яа-я]", "");
   }

   private static boolean isAllowedColor(int color) {
      for (int allowed : ClanConstants.ALLOWED_COLORS) {
         if (allowed == color) {
            return true;
         }
      }

      return false;
   }

   private static class ClanData {
      private String id = "";
      private String name = "";
      private String tag = "";
      private int color = ClanConstants.ALLOWED_COLORS[0];
      private int clanCoins;
      private String ownerUuid = "";
      private String ownerName = "";
      private List<ClanManager.ClanPlayerEntry> members = new ArrayList<>();
      private List<ClanManager.ClanPlayerEntry> pending = new ArrayList<>();
      private List<String> memberUuids;
      private List<String> memberNames;
      private List<String> pendingUuids;
      private List<String> pendingNames;
   }

   private static class ClanPlayerEntry {
      private String uuid = "";
      private String name = "";

      private ClanPlayerEntry() {
      }

      private ClanPlayerEntry(String uuid, String name) {
         this.uuid = uuid;
         this.name = name;
      }
   }

   private static class ClanStorage {
      private List<ClanManager.ClanData> clans = new ArrayList<>();
   }

   public enum Result {
      SUCCESS,
      INVALID,
      NOT_FOUND,
      NOT_OWNER,
      ALREADY_IN_CLAN,
      ALREADY_PENDING,
      NAME_TAKEN,
      NOT_ENOUGH_COINS,
      OWNER_CANNOT_LEAVE,
      CANNOT_KICK_OWNER,
      CLAN_FULL,
      CLAN_WAR_LOCKED,
      STORAGE_ERROR;
   }
}
